package com.utp.restacontrol.service;

public class OperacionBusinessException extends RuntimeException {

    private final String code;
    private final Object details;

    public OperacionBusinessException(String message, String code) {
        this(message, code, null);
    }

    public OperacionBusinessException(String message, String code, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }
}
