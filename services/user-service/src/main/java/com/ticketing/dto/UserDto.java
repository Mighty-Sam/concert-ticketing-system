package com.ticketing.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.experimental.Accessors;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import io.quarkus.runtime.annotations.RegisterForReflection;

@Data
@Accessors(chain = true)
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "email", "name", "roles", "createdAt"})
public class UserDto {

    private Long id;

    private String email;

    private String name;

    private Set<String> roles;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

}
