package com.ticketing.entity;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.extern.slf4j.Slf4j;
import lombok.EqualsAndHashCode;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.ticketing.enums.EventStatus;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@MongoEntity(collection = "events")
public class Event extends ReactivePanacheMongoEntity {

    private String title;

    private String description;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private EventStatus status;

    private List<TicketType> ticketTypes;

}
