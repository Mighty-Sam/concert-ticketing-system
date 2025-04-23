package com.ticketing.mq;

import com.ticketing.service.impl.RedisTicketService;
import com.ticketing.dto.TicketQueuePayload;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import lombok.extern.slf4j.Slf4j;
import io.smallrye.mutiny.Uni;

@Slf4j
@ApplicationScoped
public class TicketQueueConsumer {

    @Inject
    RedisTicketService redisTicketService;

    @Inject
    TicketQueueDlqProducer ticketQueueDlqProducer;

    @Incoming("ticket-process-queue")
    public Uni<Void> onMessage(TicketQueuePayload payload) {
        return redisTicketService.tryAcquireTicketAtomic(
                payload.eventId(),
                payload.ticketTypeId(),
                payload.userId()
        ).invoke(result ->
                log.info("User {} 搶票結果: {}", payload.userId(), result)
        ).onFailure().invoke(ex ->
                log.error("User {} 處理失敗: {}", payload.userId(), ex.getMessage())
        ).onFailure().call(() ->
                ticketQueueDlqProducer.sendToDlq(payload)
        ).replaceWithVoid();
    }

}
