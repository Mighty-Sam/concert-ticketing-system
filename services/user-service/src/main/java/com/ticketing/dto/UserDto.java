package com.ticketing.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.experimental.Accessors;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.bson.types.ObjectId;

@Data
@Accessors(chain = true)
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"userId", "email"," password", "name", "roles", "createdAt"})
public class UserDto {

    private ObjectId userId;

    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String name;

    private Set<String> roles;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

}
