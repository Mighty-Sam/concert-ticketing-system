package com.ticketing.entity;

import lombok.extern.slf4j.Slf4j;
import lombok.EqualsAndHashCode;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

//避免超賣（Overselling）票券。
//限制每個使用者搶票頻率。
//提供搶票狀態的快速查詢（例如：是否成功、排隊中等）。

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Entity
@Table(name = "ticket_types")
public class TicketType extends PanacheEntity {

    private String name;

    private BigDecimal price;

    private Integer totalQuantity;

    private Integer remaining;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @PrePersist
    public void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
