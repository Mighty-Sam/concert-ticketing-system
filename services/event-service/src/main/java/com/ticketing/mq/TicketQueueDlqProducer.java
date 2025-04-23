package com.ticketing.mq;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.enterprise.context.ApplicationScoped;
import com.ticketing.dto.TicketQueuePayload;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class TicketQueueDlqProducer {

    @Channel("ticket-dlq")
    Emitter<TicketQueuePayload> emitter;

    public Uni<Void> sendToDlq(TicketQueuePayload payload) {
        return Uni.createFrom().voidItem()
                .invoke(() -> emitter.send(payload));
    }

}
