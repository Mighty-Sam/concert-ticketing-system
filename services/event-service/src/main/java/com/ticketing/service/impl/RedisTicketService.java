package com.ticketing.service.impl;

import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.core.json.Json;
import io.smallrye.mutiny.Uni;
import com.ticketing.mq.TicketQueueProducer;
import com.ticketing.entity.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class RedisTicketService {

    @Inject
    TicketQueueProducer ticketQueueProducer;

    private final ReactiveKeyCommands<String> keys;
    private final ReactiveRedisDataSource reactiveRedisDS;
    private final ReactiveListCommands<String, String> list;
    private final ReactiveValueCommands<String, String> values;

    private static final long DEFAULT_CACHE_TTL_MILLIS = 600_000; // 10 minutes
    private static final long USER_LOCK_TTL_SECONDS = 600; // 避免用戶重複搶票的快取秒數
    private static final String STOCK_KEY_FORMAT = "ticket:stock:event:%s:ticketType:%s";
    private static final String USER_LOCK_KEY_FORMAT = "ticket:lock:user:%s:event:%s";
    private static final String EVENT_CACHE_KEY_FORMAT = "ticket:event:%s";
    private static final String QUEUE_KEY_FORMAT = "ticket:queue:event:%s:ticketType:%s";
    private static final String RATE_LIMIT_USER_KEY = "ticket:rate:user:%s";
    private static final String RATE_LIMIT_IP_KEY = "ticket:rate:ip:%s";

    private static final String LUA_SCRIPT = """
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

    private static final String LUA_TRY_ENTER_QUEUE = """
    local queue_key = KEYS[1]
    local max_size = tonumber(ARGV[1])
    local user_id = ARGV[2]

    local current_len = redis.call('llen', queue_key)
    if current_len >= max_size then
        return 0
    end

    redis.call('rpush', queue_key, user_id)
    return 1
    """;

    public RedisTicketService(ReactiveRedisDataSource reactiveRedisDS) {
        this.keys = reactiveRedisDS.key();
        this.reactiveRedisDS = reactiveRedisDS;
        this.list = reactiveRedisDS.list(String.class);
        this.values = reactiveRedisDS.value(String.class);
    }

    public Uni<Boolean> isAllowedByRateLimit(String userId, String ip) {
        String userKey = String.format(RATE_LIMIT_USER_KEY, userId);
        String ipKey = String.format(RATE_LIMIT_IP_KEY, ip);

        return Uni.combine().all().unis(
                incrementWithLimit(userKey, 3),
                incrementWithLimit(ipKey, 10)
        ).with((u, i) -> u && i);
    }

    private Uni<Boolean> incrementWithLimit(String key, int limit) {
        return values.incr(key).flatMap(count -> {
            if (count == 1) return keys.expire(key, 1)
                    .replaceWith(true);
            return Uni.createFrom().item(count <= limit);
        });
    }

    public Uni<Boolean> tryEnterQueue(String eventId, String ticketTypeId, String userId, int maxQueueSize) {
        String queueKey = String.format(QUEUE_KEY_FORMAT, eventId, ticketTypeId);
        return reactiveRedisDS.execute(
                Command.EVAL,
                LUA_TRY_ENTER_QUEUE,
                "1",
                queueKey,
                String.valueOf(maxQueueSize),
                userId
        ).flatMap(success -> {
            if (success.toInteger() == 1) {
                // 發送消息到 RabbitMQ 進入真正搶票處理
                return ticketQueueProducer.send(eventId, ticketTypeId, userId)
                        .replaceWith(true);
            }
            return Uni.createFrom().item(false);
        });
    }

    // 初始化票種庫存 (例如活動建立時呼叫)
    public Uni<Void> initTicketStock(String eventId, String ticketTypeId, long stock) {
        String key = String.format(STOCK_KEY_FORMAT, eventId, ticketTypeId);
        return values.set(key, String.valueOf(stock)).replaceWithVoid();
    }

    // Lua 版本：原子操作，避免超賣
    public Uni<Integer> tryAcquireTicketAtomic(String eventId, String ticketTypeId, String userId) {
        String stockKey = String.format(STOCK_KEY_FORMAT, eventId, ticketTypeId);
        String userKey = String.format(USER_LOCK_KEY_FORMAT, userId, eventId);

        String numKeys = "2"; // 表示 KEYS 有 2 個

        return reactiveRedisDS.execute(
                Command.EVAL,
                LUA_SCRIPT,
                numKeys,
                stockKey,
                userKey,
                userId,
                String.valueOf(USER_LOCK_TTL_SECONDS)
        ).map(response -> {
            if (response == null) return -1;
            try {
                return response.toInteger();
            } catch (Exception e) {
                return -99; // 表示系統錯誤
            }
        });

    }

    // 快取活動資訊(預設 TTL)
    public Uni<Void> cacheEvent(String eventId, Event event) {
        String key = String.format(EVENT_CACHE_KEY_FORMAT, eventId);
        return values.setex(key, DEFAULT_CACHE_TTL_MILLIS/1000, Json.encode(event)).replaceWithVoid();
    }

    // 快取活動資訊(可設定 TTL)
    public Uni<Void> cacheEvent(String eventId, Event event, long ttlSeconds) {
    String key = String.format(EVENT_CACHE_KEY_FORMAT, eventId);
        return values.setex(key, ttlSeconds, Json.encode(event)).replaceWithVoid();
    }

    // 取得活動快取資訊
    public Uni<Event> getCachedEvent(String eventId) {
        String key = String.format(EVENT_CACHE_KEY_FORMAT, eventId);
        return values.get(key).map(json ->
                json != null ? Json.decodeValue(json, Event.class) : null);
    }

    // 清除搶票快取（可在活動結束後使用）
    public Uni<Void> clearAllTicketDataForEvent(String eventId, String ticketTypeId) {
        String stockKey = String.format(STOCK_KEY_FORMAT, eventId, ticketTypeId);
        String lockPattern = String.format("ticket:lock:user:*:event:%s", eventId);

        return keys.keys(lockPattern)
                .flatMap(lockKeys -> keys.del(lockKeys.toArray(new String[0])))
                .flatMap(x -> keys.del(stockKey))
                .replaceWithVoid();
    }

    // 觀察剩餘票數
    public Uni<Long> getRemainingTickets(String eventId, String ticketTypeId) {
        String stockKey = String.format(STOCK_KEY_FORMAT, eventId, ticketTypeId);
        return values.get(stockKey)
                .map(stock -> stock != null ? Long.parseLong(stock) : 0L);
    }


}
