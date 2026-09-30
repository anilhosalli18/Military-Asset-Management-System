package com.mams.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedBaseAccessException extends RuntimeException {
    public UnauthorizedBaseAccessException(String message) {
        super(message);
    }
}
