package com.ticketing.entity;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.extern.slf4j.Slf4j;
import lombok.EqualsAndHashCode;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;

//避免超賣（Overselling）票券。
//限制每個使用者搶票頻率。
//提供搶票狀態的快速查詢（例如：是否成功、排隊中等）。

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@MongoEntity(collection = "ticket_type")
public class TicketType extends ReactivePanacheMongoEntity {

    private String name;

    private Event event;

    private BigDecimal price;

    private Integer totalQuantity;

    private Integer remaining;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

}
