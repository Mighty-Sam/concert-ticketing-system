package com.ticketing.common.response;

import lombok.EqualsAndHashCode;
import lombok.Data;
import java.util.LinkedHashMap;

@EqualsAndHashCode(callSuper = true)
@Data
@SuppressWarnings("unused")
public class ApiResponse<T> extends LinkedHashMap<String, Object> {

    private static final String CODE_TAG = "code";
    private static final String MSG_TAG = "msg";
    private static final String DATA_TAG = "data";
    private static final String PAGE_TAG = "pageInfo";

    // 建構子：不帶 PageInfo
    public ApiResponse(Integer code, String msg, T data) {
        super(3);
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        super.put(DATA_TAG, data);
    }

    // 建構子：帶 PageInfo
    public ApiResponse(Integer code, String msg, PageInfo pageInfo, T data) {
        super(4);
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        super.put(PAGE_TAG, pageInfo);
        super.put(DATA_TAG, data);
    }

    // 回傳單一資料的成功回應
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.SUCCESS.getCode(), HttpStatus.SUCCESS.getMsg(), data);
    }

    // 回傳 LIST 資料的成功回應，包含分頁資訊
    public static <T> ApiResponse<T> success(PageInfo pageInfo, T data) {
        return new ApiResponse<>(HttpStatus.SUCCESS.getCode(), HttpStatus.SUCCESS.getMsg(), pageInfo, data);
    }

    // 回傳錯誤訊息，包含資料
    public static <T> ApiResponse<T> error(T data) {
        return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                HttpStatus.INTERNAL_SERVER_ERROR.getMsg(), data);
    }

    // 回傳錯誤訊息，自訂錯誤碼與訊息
    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

}
