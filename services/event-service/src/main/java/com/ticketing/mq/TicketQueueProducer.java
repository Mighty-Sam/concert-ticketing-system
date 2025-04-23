package com.ticketing.mq;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.enterprise.context.ApplicationScoped;
import com.ticketing.dto.TicketQueuePayload;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class TicketQueueProducer {

    @Channel("ticket-process-queue")
    Emitter<TicketQueuePayload> emitter;

    public Uni<Void> send(String eventId, String ticketTypeId, String userId) {
        TicketQueuePayload message = new TicketQueuePayload(eventId, ticketTypeId, userId);
        return Uni.createFrom().voidItem()
                .invoke(() -> emitter.send(message));
    }

}
