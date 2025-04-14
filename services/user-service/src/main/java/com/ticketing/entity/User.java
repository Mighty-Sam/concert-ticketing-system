package com.ticketing.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketing.dto.UserDto;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.smallrye.mutiny.Uni;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j ;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@MongoEntity(collection = "user")
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Slf4j
public class User extends ReactivePanacheMongoEntity {

    @BsonProperty("email")
    public String email;

    @BsonProperty("password")
    public String password;

    @BsonProperty("name")
    public String name;

    @BsonProperty("roles")
    private Set<String> roles;

    @BsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime createdAt;

    public static Uni<User> findByEmail(String email){
        return find("email", email)
                .firstResult();
    }

    public static Uni<User> findByName(String name){
        return find("name", name)
                .firstResult();
    }

    public static Uni<User> create(UserDto userDto) {
        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(BCrypt.hashpw(userDto.getPassword(), BCrypt.gensalt()));
        user.setRoles(Set.of("USER"));
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        log.info("newUser: {}", user);
        return Uni.createFrom().item(user);
    }

}
