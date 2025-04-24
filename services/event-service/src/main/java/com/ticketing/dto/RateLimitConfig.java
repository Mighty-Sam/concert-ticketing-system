package com.ticketing.dto;

import lombok.Getter;

@Getter
public record RateLimitConfig(int userRateLimit, int ipRateLimit) {
// 限流配置類
}