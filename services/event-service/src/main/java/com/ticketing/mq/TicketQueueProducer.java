package com.ticketing.mq;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.bson.types.ObjectId;
import jakarta.enterprise.context.ApplicationScoped;
import com.ticketing.dto.TicketQueuePayload;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class TicketQueueProducer {


    @Channel("ticket-process-queue")
    Emitter<TicketQueuePayload> normalEmitter;

    @Channel("ticket-process-queue-vip")
    Emitter<TicketQueuePayload> vipEmitter;

    public Uni<Void> send(ObjectId eventId, ObjectId ticketTypeId, String userId, String priority) {
        TicketQueuePayload message = new TicketQueuePayload(eventId, ticketTypeId, userId);
        Emitter<TicketQueuePayload> emitter = "vip".equals(priority) ? vipEmitter : normalEmitter;
        return Uni.createFrom().voidItem()
                .invoke(() -> emitter.send(message));
    }

}
