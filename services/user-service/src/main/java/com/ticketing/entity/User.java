package com.ticketing.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketing.dto.UserCreateDto;
import lombok.extern.slf4j.Slf4j;
import lombok.EqualsAndHashCode;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.mindrot.jbcrypt.BCrypt;
import jakarta.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    public static Uni<User> findByEmail(String email) {
        return find("email", email)
                .firstResult();
    }

    public static Uni<User> findByName(String name){
        return find("name", name)
                .firstResult();
    }

    public static Uni<User> create(UserCreateDto userCreateDto) {
        User user = new User();
        user.setName(userCreateDto.getName());
        user.setEmail(userCreateDto.getEmail());
        user.setPassword(BCrypt.hashpw(userCreateDto.getPassword(), BCrypt.gensalt()));
        user.setRoles(Set.of("USER"));
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        return Uni.createFrom().item(user);
    }
}
