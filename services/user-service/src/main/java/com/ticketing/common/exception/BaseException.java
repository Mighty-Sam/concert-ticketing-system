package com.ticketing.common.exception;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Data;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuppressWarnings("unused")
public class BaseException extends Exception {
    private  Integer code;
    private  String defaultMessage;

}