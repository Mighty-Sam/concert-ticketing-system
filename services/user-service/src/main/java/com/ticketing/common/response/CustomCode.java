package com.ticketing.common.response;

import lombok.Getter;

@Getter
public enum CustomCode {

    ILLEGAL_ARGUMENT_EXCEPTION (- 300001 ,  "業務邏輯中的參數無效或非法異常" ),
    SYS_UNKNOWN_EXCEPTION (- 300002 ,  "系統未知異常，請聯繫客服人員" ),

    //############## USER 模組 ##################
    USER_EMAIL_HAS_REGISTRY (- 400001 ,  "該使用者Email已註冊" ),
    USER_NOT_FOUND(- 400002, "找不到此使用者"),
    USER_EMAIL_OR_PASSWORD_ERROR (- 400009 ,  "帳號或密碼錯誤" );


    private final int code;
    private final String msg;

    CustomCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
