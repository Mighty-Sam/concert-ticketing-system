package com.ticketing.dto;

import org.bson.types.ObjectId;

public record TryQueueRequest(
        ObjectId eventId,
        ObjectId ticketTypeId,
        String userId,
        int maxQueueSize,
        String captchaToken
) {}