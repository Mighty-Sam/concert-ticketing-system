package com.ticketing.entity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.EqualsAndHashCode;
import lombok.Data;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import com.ticketing.enums.EventStatus;
import jakarta.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Entity
@Table(name = "events")
public class Event extends PanacheEntity {

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketType> ticketTypes;

    @PrePersist
    public void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }


}
