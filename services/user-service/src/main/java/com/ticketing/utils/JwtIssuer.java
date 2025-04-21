package com.ticketing.utils;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import com.ticketing.entity.User;
import io.smallrye.jwt.build.Jwt;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class JwtIssuer {

    private static final String ISSUER = "ticketing-app";
    private static final Duration EXPIRATION = Duration.ofDays(5);

    /**
     * 產生 JWT token，包含基本身份與自定義 claims
     *
     * @param user 使用者資訊
     * @return JWT 字串
     */
    public String generate(User user) {
        String token = Jwt.upn(user.getEmail())
                .issuer(ISSUER)
                .subject(user.getEmail())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(EXPIRATION))
                .claim("userId", user.getId())
                .groups(user.getRoles())
                .sign();

        log.debug("Generated JWT token for user: {}", user.getEmail());
        return token;
    }

}
