package com.ticketing.resource;

import com.ticketing.common.response.ApiResponse;
import com.ticketing.service.impl.RedisService;
import com.ticketing.service.UserService;
import com.ticketing.dto.UserCreateDto;
import com.ticketing.dto.UserDto;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;

@Path("/user")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterForReflection
@Slf4j
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    RedisService redisService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/register")
    public Uni<ApiResponse<UserDto>> register(UserCreateDto userCreateDto) {
        return userService.register(userCreateDto)
                .map(ApiResponse::success);
    }

    @POST
    @Path("/login")
    public Uni<ApiResponse<String>> login(UserCreateDto userCreateDto) {
        return userService.login(userCreateDto)
                .map(ApiResponse::success);
    }

    @GET
    @Path("/logout")
    @RolesAllowed("USER")
    public Uni<Void> logout(@Context SecurityContext ctx) {
        String jti = jwt.getClaim("jti");
        Long expEpochSecond = jwt.getClaim("exp"); // 以秒為單位
        long now = Instant.now().getEpochSecond();
        long ttl = expEpochSecond - now;

        log.info("now: {}", now );
        log.info("tl: {}", ttl);

        if (ttl <= 0) {
            return Uni.createFrom().voidItem();
        }

        return redisService.blacklistToken(jti, ttl)
                .invoke(() -> log.info("Token jti {} blacklisted", jti))
                .replaceWithVoid();
    }

    @GET
    @Path("/{name}")
    @RolesAllowed("USER")
    public Uni<ApiResponse<UserDto>> findByName(String name) {
        return userService.findByName(name.trim())
                .map(ApiResponse::success);
    }

}
