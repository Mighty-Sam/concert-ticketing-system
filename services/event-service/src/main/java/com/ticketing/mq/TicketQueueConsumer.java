package com.ticketing.mq;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.faulttolerance.Retry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.ticketing.service.impl.RedisTicketServiceImpl;
import com.ticketing.dto.TicketQueuePayload;
import com.ticketing.enums.AcquireResult;
import io.vertx.core.json.JsonObject;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class TicketQueueConsumer {

    @Inject
    RedisTicketServiceImpl redisTicketServiceImpl;

    @Inject
    TicketQueueDlqProducer ticketQueueDlqProducer;

    @Incoming("ticket-process-queue")
    @Incoming("ticket-process-queue-vip")
    @Retry(maxRetries = 3, delay = 1000)
    public Uni<Void> onMessage(TicketQueuePayload payload) {
        return redisTicketServiceImpl.tryAcquireTicketAtomic(
                payload.eventId(),
                payload.ticketTypeId(),
                payload.userId()
        ).flatMap(result -> {
            JsonObject logData = new JsonObject()
                    .put("eventId", payload.eventId())
                    .put("ticketTypeId", payload.ticketTypeId())
                    .put("userId", payload.userId())
                    .put("result", result.name())
                    .put("message", result.getMessage());
            log.info("搶票結果: {}", logData.encode());

            if (result == AcquireResult.ALREADY_CLAIMED || result == AcquireResult.SOLD_OUT) {
                return ticketQueueDlqProducer.sendToDlq(payload);
            }
            return Uni.createFrom().voidItem();
        }).onFailure().invoke(ex -> {
            JsonObject logData = new JsonObject()
                    .put("eventId", payload.eventId().toHexString())
                    .put("ticketTypeId", payload.ticketTypeId())
                    .put("userId", payload.userId())
                    .put("error", ex.getMessage());
            log.error("搶票失敗: {}", logData.encode());
        }).onFailure().recoverWithUni(() -> ticketQueueDlqProducer.sendToDlq(payload));
    }

}
