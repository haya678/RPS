package com.xanwar.rps.exception;

public class TornApiException extends RuntimeException {

    public TornApiException(String message) {
        super(message);
    }

    public TornApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
