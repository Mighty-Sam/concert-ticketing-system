package com.ticketing.common.exception;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Data;

@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("unused")
@Data
@AllArgsConstructor
public class CustomException extends BaseException {

    public CustomException(Integer code, String defaultMessage) {
        super(code, defaultMessage);
    }

}
