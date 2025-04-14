package com.ticketing.common.response;

import lombok.Getter;

@Getter
public enum HttpStatus {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "業務異常，請檢查輸入參數是否正確"),
    UNAUTHORIZED(401, "未授權 - 需要身份驗證"),
    FORBIDDEN(403, "禁止訪問"),
    NOT_FOUND(404, "資源不存在，請檢查請求路徑是否正確"),
    METHOD_NOT_ALLOWED(405, "請求方法不支持，請檢查請求法訪是否正確"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒體類型"),
    INTERNAL_SERVER_ERROR(500, "內部伺服器錯誤 - 發生意外錯誤");

    private final int code;
    private final String msg;

    HttpStatus(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
