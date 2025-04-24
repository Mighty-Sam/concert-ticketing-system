package com.ticketing.resource;

import com.ticketing.service.RedisTicketService;
import com.ticketing.dto.TryAcquireRequest;
import com.ticketing.dto.TryQueueRequest;
import com.ticketing.entity.Event;
import io.vertx.core.json.JsonObject;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.bson.types.ObjectId;

@Path("/tickets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TicketResource {

    @Inject
    RedisTicketService ticketService;

    // 1. 嘗試進入隊列
    @POST
    @Path("/queue")
    public Uni<Response> tryEnterQueue(TryQueueRequest request) {
        return verifyCaptcha(request.captchaToken())
                .flatMap(valid -> {
                    if (!valid) {
                        return Uni.createFrom().item(
                                Response.status(Response.Status.BAD_REQUEST)
                                        .entity(new JsonObject().put("error", "無效的驗證碼").encode())
                                        .build()
                        );
                    }
                    String queueKey = String.format("ticket:queue:event:%s:ticketType:%s:priority:%s",
                            request.eventId(), request.ticketTypeId(), "normal");
                    return ticketService.getQueueLength(queueKey)
                            .flatMap(currentLength -> ticketService.tryEnterQueue(
                                    request.eventId(),
                                    request.ticketTypeId(),
                                    request.userId(),
                                    request.maxQueueSize(),
                                    false // 非 VIP
                            ).map(success -> {
                                if (success) {
                                    return Response.ok().build();
                                }
                                JsonObject error = new JsonObject()
                                        .put("error", "隊列已滿")
                                        .put("currentQueueLength", currentLength)
                                        .put("maxQueueSize", request.maxQueueSize())
                                        .put("retryAfterSeconds", 30);
                                return Response.status(Response.Status.TOO_MANY_REQUESTS)
                                        .entity(error.encode())
                                        .build();
                            }));
                });
    }

    /**
     * 模擬驗證碼檢查
     */
    private Uni<Boolean> verifyCaptcha(String captchaToken) {
        // 實際應調用外部驗證碼服務（例如 reCAPTCHA API）
        return Uni.createFrom().item(captchaToken != null && !captchaToken.isEmpty());
    }

    // 2. 嘗試搶票
    @POST
    @Path("/acquire")
    public Uni<Response> tryAcquire(TryAcquireRequest request) {
        return ticketService.tryAcquireTicketAtomic(
                request.eventId(),
                request.ticketTypeId(),
                request.userId()
        ).map(result -> Response.status(result.getHttpStatus())
                .entity(result.getMessage())
                .build());
    }

    // 3. 查詢活動快取
    @GET
    @Path("/{eventId}/cache")
    public Uni<Event> getCachedEvent(@PathParam("eventId") ObjectId eventId) {
        return ticketService.getCachedEvent(eventId);
    }

    // 4. 查詢剩餘票數
    @GET
    @Path("/{eventId}/remaining")
    public Uni<Long> getRemainingTickets(
            @PathParam("eventId") ObjectId eventId,
            @QueryParam("ticketTypeId") ObjectId ticketTypeId
    ) {
        return ticketService.getRemainingTickets(eventId, ticketTypeId);
    }

    // 5. 清除資料
    @DELETE
    @Path("/{eventId}/{ticketTypeId}")
    public Uni<Void> clearEventData(
            @PathParam("eventId") ObjectId eventId,
            @PathParam("ticketTypeId") ObjectId ticketTypeId
    ) {
        return ticketService.clearAllTicketDataForEvent(eventId, ticketTypeId);
    }

}
