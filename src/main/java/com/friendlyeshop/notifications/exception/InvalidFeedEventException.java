package com.friendlyeshop.notifications.exception;

public class InvalidFeedEventException extends RuntimeException {

    public InvalidFeedEventException(String message) {
        super(message);
    }

    public InvalidFeedEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
