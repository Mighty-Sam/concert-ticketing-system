package com.ticketing.dto;

import org.bson.types.ObjectId;

public record TicketQueuePayload(ObjectId eventId, ObjectId ticketTypeId, String userId) {}
