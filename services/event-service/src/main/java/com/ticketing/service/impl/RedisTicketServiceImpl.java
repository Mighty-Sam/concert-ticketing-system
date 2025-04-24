package com.ticketing.service.impl;

import com.ticketing.enums.AcquireResult;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.redis.datasource.sortedset.ReactiveSortedSetCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.core.json.Json;
import io.smallrye.mutiny.Uni;
import com.ticketing.service.RedisTicketService;
import com.ticketing.mq.TicketQueueProducer;
import com.ticketing.dto.RateLimitConfig;
import com.ticketing.entity.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

/***
 * 1. 限流 (Rate Limit)
 *   - 每秒最多 3 次請求（每個使用者）
 *   - 每秒最多 10 次請求（每個 IP）
 * 2. 排隊邏輯 (tryEnterQueue)
 *   - 使用 Lua 腳本將使用者排入搶票佇列
 */

@ApplicationScoped
public class RedisTicketServiceImpl implements RedisTicketService {

    @Inject
    TicketQueueProducer ticketQueueProducer;

    private final ReactiveKeyCommands<String> keys;
    private final ReactiveRedisDataSource reactiveRedisDS;
    private final ReactiveListCommands<String, String> list;
    private final ReactiveSortedSetCommands<String, String> sortedSet;
    private final ReactiveValueCommands<String, String> values;

    // 常數設定
    private static final long RATE_LIMIT_WINDOW_SECONDS = 10; // 滑動窗口 10 秒
    private static final long DEFAULT_CACHE_TTL_SECONDS = 600; // 活動快取預設存活時間（秒）
    private static final long USER_LOCK_TTL_SECONDS = 60; // 使用者搶票鎖定時間（秒）
    private static final int DEFAULT_USER_RATE_LIMIT = 3; // 每秒最多 3 次請求（每個使用者）
    private static final int DEFAULT_IP_RATE_LIMIT = 10; // 每秒最多 10 次請求（每個 IP）
    private static final long QUEUE_TTL_SECONDS = 30; // Rabbitmq 隊列超時 30 秒

    // Redis Key 格式
    private static final String STOCK_KEY_FORMAT = "ticket:stock:event:%s:ticketType:%s";
    private static final String STOCK_INIT_LOCK = "ticket:stock:init:lock:%s:%s";
    private static final String USER_LOCK_KEY_FORMAT = "ticket:lock:user:%s:event:%s";
    private static final String EVENT_CACHE_KEY_FORMAT = "ticket:event:%s";
    private static final String QUEUE_KEY_FORMAT = "ticket:queue:event:%s:ticketType:%s:priority:%s";
    private static final String USER_QUEUE_TTL_KEY = "ticket:queue:user:%s:event:%s:ticketType:%s";
    // 動態限流配置（可從 MongoDB 或配置文件加載）
    private static final String RATE_LIMIT_CONFIG_KEY = "ticket:rate:config:event:%s";


    // Lua 腳本：嘗試排入搶票佇列（支援優先級）
    private static final String LUA_TRY_ENTER_QUEUE = """
        local queue_key = KEYS[1]
        local user_ttl_key = KEYS[2]
        local max_size = tonumber(ARGV[1])
        local user_id = ARGV[2]
        local ttl_seconds = tonumber(ARGV[3])

        local current_len = redis.call('llen', queue_key)
        if current_len >= max_size then
            return 0
        end

        redis.call('rpush', queue_key, user_id)
        redis.call('setex', user_ttl_key, ttl_seconds, user_id)
        return 1
    """;

    // Lua 腳本：搶票原子操作（減庫存 + 加鎖）
    private static final String LUA_ATOMIC_TICKET_ACQUIRE_SCRIPT  = """
        local stock_key = KEYS[1]
        local user_lock_key = KEYS[2]
        local user_id = ARGV[1]
        local user_lock_ttl = tonumber(ARGV[2])

        if redis.call('exists', user_lock_key) == 1 then
            return 0  -- 用戶已搶過票
        end

        local stock = tonumber(redis.call('get', stock_key) or "-1")
        if stock <= 0 then
            return -1 -- 庫存不足
        end

        redis.call('decr', stock_key)
        redis.call('setex', user_lock_key, user_lock_ttl, user_id)
        return 1 -- 成功搶票
    """;


    public RedisTicketServiceImpl(ReactiveRedisDataSource reactiveRedisDS) {
        this.keys = reactiveRedisDS.key();
        this.reactiveRedisDS = reactiveRedisDS;
        this.list = reactiveRedisDS.list(String.class);
        this.sortedSet = reactiveRedisDS.sortedSet(String.class);
        this.values = reactiveRedisDS.value(String.class);
    }

    /**
     * 預熱活動數據到 Redis
     * 在活動開始前（例如通過定時任務或管理介面）呼叫此方法
     */
    public Uni<Void> preheatEventCache(ObjectId eventId) {
        // 假設從 MongoDB/MySQL 獲取活動數據
        return fetchEventFromDatabase(eventId)
                .flatMap(event -> {
                    if (event == null) {
                        return Uni.createFrom().voidItem();
                    }
                    String cacheKey = String.format(EVENT_CACHE_KEY_FORMAT, eventId);
                    return values.setex(cacheKey, DEFAULT_CACHE_TTL_SECONDS, Json.encode(event))
                            .replaceWithVoid();
                });
    }

    /**
     * 模擬從 MongoDB/MySQL 獲取活動數據
     */
    private Uni<Event> fetchEventFromDatabase(ObjectId eventId) {
        return Event.findById(eventId);
    }

    /**
     * 初始化票種庫存（使用 SETNX 防止重複初始化）
     */
    @Override
    public Uni<Boolean> initTicketStock(ObjectId eventId, ObjectId ticketTypeId, long stock) {
        String stockKey = String.format(STOCK_KEY_FORMAT, eventId, ticketTypeId);
        String lockKey = String.format(STOCK_INIT_LOCK, eventId, ticketTypeId);

        // 假設從 MySQL/MongoDB 獲取活動數據（模擬）
        return checkEventInDatabase(eventId, ticketTypeId, stock)
                .flatMap(valid -> {
                    if (!valid) {
                        return Uni.createFrom().item(false);
                    }
                    // 使用 SETNX 確保只初始化一次
                    return values.setnx(lockKey, "1")
                            .flatMap(set -> {
                                if (!set) {
                                    return Uni.createFrom().item(false); // 已初始化
                                }
                                return values.set(stockKey, String.valueOf(stock))
                                        .flatMap(x -> keys.expire(lockKey, 3600)) // 鎖定 1 小時
                                        .replaceWith(true);
                            });
                });
    }

    /**
     * 模擬檢查 MySQL/MongoDB 中的活動數據
     */
    private Uni<Boolean> checkEventInDatabase(ObjectId eventId, ObjectId ticketTypeId, long stock) {
        // 這裡應調用 MySQL/MongoDB 查詢，檢查活動和票種是否存在，且庫存匹配
        return Uni.createFrom().item(true); // 模擬檢查通過
    }

    /**
     * 檢查用戶和 IP 是否符合限流條件（滑動窗口）
     */
    @Override
    public Uni<Boolean> isAllowedByRateLimit(ObjectId eventId, String userId, String ip) {
        String userKey = String.format("ticket:rate:user:%s:event:%s", userId, eventId);
        String ipKey = String.format("ticket:rate:ip:%s:event:%s", ip, eventId);

        // 獲取動態限流配置
        return getRateLimitConfig(eventId).flatMap(config ->
                Uni.combine().all().unis(
                        checkSlidingWindowLimit(userKey, config.userRateLimit()),
                        checkSlidingWindowLimit(ipKey, config.ipRateLimit())
                ).with((userAllowed, ipAllowed) -> userAllowed && ipAllowed)
        );
    }

    /**
     * 滑動窗口限流實現
     */
    private Uni<Boolean> checkSlidingWindowLimit(String key, int limit) {
        long now = System.currentTimeMillis();
        long windowStart = now - RATE_LIMIT_WINDOW_SECONDS * 1000;

        // 清理過期記錄並檢查當前請求數
        return sortedSet.zremrangebyrank(key, 0, windowStart)
                .flatMap(removed -> sortedSet.zcard(key))
                .flatMap(count -> {
                    if (count >= limit) {
                        return Uni.createFrom().item(false);
                    }
                    // 添加新請求記錄
                    return sortedSet.zadd(key, now, String.valueOf(now))
                            .flatMap(added -> keys.expire(key, RATE_LIMIT_WINDOW_SECONDS))
                            .replaceWith(true);
                });
    }

    /**
     * 獲取活動的限流配置
     */
    private Uni<RateLimitConfig> getRateLimitConfig(ObjectId eventId) {
        String configKey = String.format(RATE_LIMIT_CONFIG_KEY, eventId);
        return values.get(configKey).map(json ->
                json != null ? Json.decodeValue(json, RateLimitConfig.class)
                        : new RateLimitConfig(DEFAULT_USER_RATE_LIMIT, DEFAULT_IP_RATE_LIMIT)
        );
    }

    /**
     * 嘗試將用戶排入搶票佇列（支援優先級和超時）
     */
    @Override
    public Uni<Boolean> tryEnterQueue(ObjectId eventId, ObjectId ticketTypeId, String userId, int maxQueueSize, boolean isVip) {
        String priority = isVip ? "vip" : "normal"; // VIP 用戶進入高優先級隊列
        String queueKey = String.format(QUEUE_KEY_FORMAT, eventId, ticketTypeId, priority);
        String userTtlKey = String.format(USER_QUEUE_TTL_KEY, userId, eventId, ticketTypeId);

        return reactiveRedisDS.execute(
                Command.EVAL,
                LUA_TRY_ENTER_QUEUE,
                "2",
                queueKey,
                userTtlKey,
                String.valueOf(maxQueueSize),
                userId,
                String.valueOf(QUEUE_TTL_SECONDS)
        ).flatMap(success -> {
            if (success.toInteger() == 1) {
                return ticketQueueProducer.send(eventId, ticketTypeId, userId, priority)
                        .replaceWith(true);
            }
            return Uni.createFrom().item(false);
        });
    }

    /**
     * 獲取隊列當前長度
     */
    public Uni<Long> getQueueLength(String queueKey) {
        return list.llen(queueKey).map(length -> length != null ? length : 0L);
    }

    /**
     * 使用 Lua 腳本嘗試原子性地搶票（減庫存 + 上鎖），避免超賣與重複搶票
     */
    @Override
    public Uni<AcquireResult> tryAcquireTicketAtomic(ObjectId eventId, ObjectId ticketTypeId, String userId) {
        String stockKey = String.format(STOCK_KEY_FORMAT, eventId, ticketTypeId);
        String userKey = String.format(USER_LOCK_KEY_FORMAT, userId, eventId);

        return reactiveRedisDS.execute(
                Command.EVAL,
                LUA_ATOMIC_TICKET_ACQUIRE_SCRIPT ,
                "2", // KEYS的數量(stockKey, userKey)
                stockKey,
                userKey,
                userId,
                String.valueOf(USER_LOCK_TTL_SECONDS)
        ).map(response -> {
            if (response == null) {
                return AcquireResult.SYSTEM_ERROR;
            }
            try {
                return AcquireResult.values()[response.toInteger() + 1]; // 假設 1->SUCCESS, 0->ALREADY_CLAIMED, -1->SOLD_OUT
            } catch (Exception e) {
                return AcquireResult.SYSTEM_ERROR;
            }
        });
    }

    /**
     * 快取活動資訊（使用預設 TTL）
     */
    @Override
    public Uni<Void> cacheEvent(ObjectId eventId, Event event) {
        String key = String.format(EVENT_CACHE_KEY_FORMAT, eventId);
        return values.setex(key, DEFAULT_CACHE_TTL_SECONDS, Json.encode(event)).replaceWithVoid();
    }

    /**
     * 快取活動資訊（自訂 TTL）
     */
    @Override
    public Uni<Void> cacheEvent(ObjectId eventId, Event event, long ttlSeconds) {
        String key = String.format(EVENT_CACHE_KEY_FORMAT, eventId);
        return values.setex(key, ttlSeconds, Json.encode(event)).replaceWithVoid();
    }

    /**
     * 取得 Redis 中快取的活動資訊
     */
    @Override
    public Uni<Event> getCachedEvent(ObjectId eventId) {
        String key = String.format(EVENT_CACHE_KEY_FORMAT, eventId);
        return values.get(key).map(json ->
                json != null ? Json.decodeValue(json, Event.class) : null);
    }

    /**
     * 清除活動的所有搶票資料（票數 + 使用者鎖），可用於活動結束後清理
     */
    @Override
    public Uni<Void> clearAllTicketDataForEvent(ObjectId eventId, ObjectId ticketTypeId) {
        String stockKey = String.format(STOCK_KEY_FORMAT, eventId, ticketTypeId);
        String lockPattern = String.format("ticket:lock:user:*:event:%s", eventId);

        return keys.keys(lockPattern)
                .flatMap(lockKeys -> keys.del(lockKeys.toArray(new String[0])))
                .flatMap(x -> keys.del(stockKey))
                .replaceWithVoid();
    }

    /**
     * 查詢指定票種目前剩餘的票數
     */
    @Override
    public Uni<Long> getRemainingTickets(ObjectId eventId, ObjectId ticketTypeId) {
        String stockKey = String.format(STOCK_KEY_FORMAT, eventId, ticketTypeId);
        return values.get(stockKey)
                .map(stock -> stock != null ? Long.parseLong(stock) : 0L);
    }

}

