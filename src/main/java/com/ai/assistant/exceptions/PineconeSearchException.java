package com.ai.assistant.exceptions;

public class PineconeSearchException extends RuntimeException {
    public PineconeSearchException(String message) {
        super(message);
    }

    public PineconeSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}