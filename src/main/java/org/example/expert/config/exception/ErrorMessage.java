package org.example.expert.config.exception;

import lombok.Getter;

@Getter
public class ErrorMessage {
    private final int Status;
    private final String error;
    private final String message;

    public ErrorMessage(int status, String error, String message) {
        Status = status;
        this.error = error;
        this.message = message;
    }
}
