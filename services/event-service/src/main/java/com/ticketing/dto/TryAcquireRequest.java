package com.ticketing.dto;

import org.bson.types.ObjectId;

public record TryAcquireRequest(
        ObjectId eventId,
        ObjectId ticketTypeId,
        String userId
) {}