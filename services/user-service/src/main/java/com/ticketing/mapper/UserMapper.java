package com.ticketing.mapper;

import com.ticketing.dto.UserDto;
import com.ticketing.entity.User;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    @Mapping(target = "id",source = "userId")
    User toEntity(UserDto userDto);

    @Mapping(target = "userId", source = "id")
    UserDto toDto(User user);

}
