package com.ticketing.service;

import com.ticketing.dto.UserDto;
import io.smallrye.mutiny.Uni;

public interface UserService {

    Uni<UserDto> register(UserDto userDto);

    Uni<String> login(UserDto userDto);

    Uni<UserDto> findByName(String name);

}
