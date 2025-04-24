package com.ticketing.enums;

import lombok.Getter;

@Getter
public enum AcquireResult {
    SUCCESS(1, "搶票成功", 200),
    ALREADY_CLAIMED(0, "用戶已搶票", 409),
    SOLD_OUT(-1, "票已售罄", 410),
    SYSTEM_ERROR(-99, "系統錯誤", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    AcquireResult(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
