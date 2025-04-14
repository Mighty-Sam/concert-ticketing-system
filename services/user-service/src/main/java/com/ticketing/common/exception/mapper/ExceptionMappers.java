package com.ticketing.common.exception.mapper;

import com.ticketing.common.exception.CustomException;
import com.ticketing.common.exception.BaseException;
import com.ticketing.common.response.ApiResponse;
import com.ticketing.common.response.CustomCode;
import com.ticketing.common.response.HttpStatus;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.inject.Singleton;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.RestResponse;
import lombok.extern.slf4j.Slf4j;
import io.smallrye.mutiny.Uni;

/**
 * @Description: 全域異常攔截器
 * @Author: Sam
 * @Date: 2025/04/13
 */
@Slf4j
@Provider
@Singleton
public class ExceptionMappers {

    // 基礎異常
    @ServerExceptionMapper
    public Uni<RestResponse<ApiResponse<Object>>> mapException(BaseException baseException) {
        return Uni.createFrom().item(RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                ApiResponse.error(baseException.getCode(), ":" + baseException.getDefaultMessage())))
                .invoke(() -> log.error(baseException.getDefaultMessage()));
    }

    // 客製業務異常
    @ServerExceptionMapper(CustomException.class)
    public Uni<RestResponse<ApiResponse<Object>>> mapException(CustomException customException) {
        return Uni.createFrom().item(RestResponse.status(Response.Status.BAD_REQUEST,
                ApiResponse.error(customException.getCode(), customException.getDefaultMessage())))
                .invoke(() -> log.error(customException.getDefaultMessage()));
    }

    @ServerExceptionMapper(IllegalArgumentException.class)
    public Uni<RestResponse<ApiResponse<Object>>> mapException (IllegalArgumentException illegalArgumentException) {
        return Uni.createFrom().item(RestResponse.status(Response.Status.BAD_REQUEST,
                ApiResponse.error(CustomCode.ILLEGAL_ARGUMENT_EXCEPTION.getCode(), illegalArgumentException.getMessage())))
                .invoke(() -> log.error(illegalArgumentException.getMessage()));
    }

    // 404 異常
    @ServerExceptionMapper(NotFoundException.class)
    public Uni<RestResponse<ApiResponse<Object>>> mapException (NotFoundException  notFoundException) {
        return  Uni.createFrom().item(RestResponse.status(Response.Status.NOT_FOUND,
                ApiResponse.error(HttpStatus.NOT_FOUND.getCode(), HttpStatus.NOT_FOUND.getMsg())))
                .invoke(() -> log.error(HttpStatus.NOT_FOUND.getMsg()));
    }

    // 405 方法不支持異常
    @ServerExceptionMapper(NotAllowedException.class)
    public Uni<RestResponse<ApiResponse<Object>>> mapException (NotAllowedException notAllowedException) {
        return Uni.createFrom().item(RestResponse.status(Response.Status.METHOD_NOT_ALLOWED,
                ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED.getCode(), HttpStatus.METHOD_NOT_ALLOWED.getMsg())))
                .invoke(() -> log.error(HttpStatus.METHOD_NOT_ALLOWED.getMsg()));
    }

    // 全局攔截未處理異常，回傳 500 Internal Server Error
    @ServerExceptionMapper(Throwable.class)
    public Uni<RestResponse<ApiResponse<Object>>> mapException(Throwable throwable) {
        return Uni.createFrom().item(RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR,
                        ApiResponse.error(CustomCode.SYS_UNKNOWN_EXCEPTION.getCode(),
                                CustomCode.SYS_UNKNOWN_EXCEPTION.getMsg())))
                .invoke(() ->  log.error("Unknown exception, error: ", throwable));
    }

}
