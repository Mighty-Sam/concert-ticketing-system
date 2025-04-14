package com.ticketing.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.Priorities;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import com.ticketing.service.impl.RedisService;

@ApplicationScoped
@Provider
@Priority(Priorities.AUTHENTICATION + 1) // 在驗證之後
public class JwtBlacklistFilter implements ContainerRequestFilter {

    @Inject
    RedisService redisService;

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String jti = jwt.getClaim("jti");
        if (jti == null) return;

        redisService.isTokenBlacklisted(jti)
                .subscribe().with(isBlacklisted -> {
                    if (isBlacklisted) {
                        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                                .entity("Token is blacklisted.").build());
                    }
                });
    }
}
