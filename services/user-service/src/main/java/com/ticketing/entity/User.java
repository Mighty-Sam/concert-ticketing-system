package com.ticketing.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ticketing.dto.UserCreateDto;
import lombok.extern.slf4j.Slf4j;
import lombok.EqualsAndHashCode;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import org.mindrot.jbcrypt.BCrypt;
import jakarta.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private Set<String> roles;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    public static Uni<User> findByEmail(String email) {
        return find("email", email)
                .project(User.class)
                .firstResult();
    }

    public static Uni<User> findByName(String name){
        return find("name", name)
                .project(User.class)
                .firstResult();
    }

    public static Uni<User> create(UserCreateDto userCreateDto) {
        User user = new User();
        user.setName(userCreateDto.getName());
        user.setEmail(userCreateDto.getEmail());
        user.setPassword(BCrypt.hashpw(userCreateDto.getPassword(), BCrypt.gensalt()));
        user.setRoles(Set.of("USER"));
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        log.info("newUser: {}", user);
        return Uni.createFrom().item(user);
    }
}
