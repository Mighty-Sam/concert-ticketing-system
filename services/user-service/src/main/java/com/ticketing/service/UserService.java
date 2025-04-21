package com.ticketing.service;

import com.ticketing.dto.UserCreateDto;
import com.ticketing.dto.UserDto;
import io.smallrye.mutiny.Uni;

public interface UserService {

    Uni<UserDto> register(UserCreateDto userCreateDto);

    Uni<String> login(UserCreateDto userCreateDto);

    Uni<UserDto> findByName(String name);

}
