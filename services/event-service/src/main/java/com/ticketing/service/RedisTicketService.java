package com.ticketing.service;

import com.ticketing.entity.Event;
import com.ticketing.enums.AcquireResult;
import org.bson.types.ObjectId;
import io.smallrye.mutiny.Uni;

public interface RedisTicketService {

    Uni<Boolean> isAllowedByRateLimit(ObjectId eventId, String userId, String ip);

    Uni<Long> getQueueLength(String queueKey);

    Uni<Boolean> tryEnterQueue(ObjectId eventId, ObjectId ticketTypeId, String userId, int maxQueueSize, boolean isVip);

    Uni<Boolean> initTicketStock(ObjectId eventId, ObjectId ticketTypeId, long stock);

    Uni<AcquireResult> tryAcquireTicketAtomic(ObjectId eventId, ObjectId ticketTypeId, String userId);

    Uni<Void> cacheEvent(ObjectId eventId, Event event);

    Uni<Void> cacheEvent(ObjectId eventId, Event event, long ttlSeconds);

    Uni<Event> getCachedEvent(ObjectId eventId);

    Uni<Void> clearAllTicketDataForEvent(ObjectId eventId, ObjectId ticketTypeId);

    Uni<Long> getRemainingTickets(ObjectId eventId, ObjectId ticketTypeId);

}
