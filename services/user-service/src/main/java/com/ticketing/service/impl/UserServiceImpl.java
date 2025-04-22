package com.ticketing.service.impl;

import com.ticketing.common.exception.CustomException;
import com.ticketing.common.response.CustomCode;
import com.ticketing.service.UserService;
import com.ticketing.dto.UserCreateDto;
import com.ticketing.mapper.UserMapper;
import com.ticketing.dto.UserLoginDto;
import com.ticketing.utils.JwtIssuer;
import com.ticketing.entity.User;
import com.ticketing.dto.UserDto;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mindrot.jbcrypt.BCrypt;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class UserServiceImpl implements UserService {

    @Inject
    JwtIssuer jwtIssuer;

    @Inject
    UserMapper userMapper;

    @WithTransaction
    @Override
    public Uni<UserDto> register(UserCreateDto userCreateDto) {
        return isRegistered(userCreateDto.getEmail())
                .flatMap(isExisted -> {
                    if (isExisted) {
                        return Uni.createFrom().failure(new CustomException(
                                CustomCode.USER_EMAIL_HAS_REGISTRY.getCode(),
                                CustomCode.USER_EMAIL_HAS_REGISTRY.getMsg()
                        ));
                    }

                    return User.create(userCreateDto)
                            .flatMap(newUser -> newUser.persist()
                                    .replaceWith(userMapper.toDto(newUser))
                            );
                });
    }

    // 查看是否此帳號(email)已被註冊過
    private Uni<Boolean> isRegistered(String email) {
        return User.findByEmail(email)
                .map(user -> Optional.ofNullable(user).isPresent());
    }

    @WithSession
    @Override
    public Uni<String> login(UserLoginDto userLoginDto) {
        String email = userLoginDto.getEmail();
        String password = userLoginDto.getPassword();

        return User.findByEmail(email)
                .flatMap(existingUser -> {
                    if (isInvalidCredential(existingUser, password)) {
                        return Uni.createFrom().failure(new CustomException(
                                CustomCode.USER_EMAIL_OR_PASSWORD_ERROR.getCode(),
                                CustomCode.USER_EMAIL_OR_PASSWORD_ERROR.getMsg()
                        ));
                    }

                    return Uni.createFrom().item(jwtIssuer.generate(existingUser)); // 登入成功，就產 Jwt token
                });
    }

    /***
     * 驗證使用者是否不存在或密碼錯誤
     *
     * 此方法會先檢查 `existingUser` 是否為 null，若是代表帳號不存在
     * 接著使用 BCrypt 驗證輸入的密碼是否與資料庫中的加密密碼相符
     * 如果使用者不存在或密碼不正確，則回傳 true，表示驗證失敗
     *
     * @param existingUser 查詢資料庫後取得的使用者資料（可能為 null）
     * @param password 使用者輸入的明文密碼
     * @return true 表示驗證失敗（帳號不存在或密碼錯誤）；false 表示驗證成功
     */
    private boolean isInvalidCredential(User existingUser, String password) {
        return (existingUser == null || !BCrypt.checkpw(password, existingUser.getPassword()));
    }

    @WithSession
    @Override
    public Uni<UserDto> findByName(String name) {
        return User.findByName(name)
                .onItem()
                .ifNull()
                .failWith(() -> new CustomException(
                        CustomCode.USER_NOT_FOUND.getCode(),
                        "unable to find the corresponding user info, username: " + name
                ))
                .map(userMapper::toDto);
    }

}
