package com.ticketing.service.impl;

import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;
import java.util.Objects;

@Singleton
public class RedisService {

    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    private final ReactiveKeyCommands<String> keys;
    private final ReactiveValueCommands<String, String> cmd;
    private final RedisDataSource redisDS;
    private final ReactiveRedisDataSource reactiveRedisDS;

    public RedisService(RedisDataSource redisDS, ReactiveRedisDataSource reactiveRedisDS) {
        this.redisDS = redisDS;
        this.reactiveRedisDS = reactiveRedisDS;
        this.keys = reactiveRedisDS.key();
        this.cmd = reactiveRedisDS.value(String.class);
    }

    /**
     * 將指定 jti 加入黑名單，並設置過期時間（秒）
     */
    public Uni<Void> blacklistToken(String jti, long ttlSeconds) {
        String key = JWT_BLACKLIST_PREFIX + jti;
        return cmd.setex(key, ttlSeconds, "blacklisted");
    }

    /**
     * 檢查指定 jti 是否在黑名單中
     */
    public Uni<Boolean> isTokenBlacklisted(String jti) {
        String key = JWT_BLACKLIST_PREFIX + jti;
        return cmd.get(key).map(Objects::nonNull);
    }

}
