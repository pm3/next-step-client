package io.aston.nextstep.service;

public class RetryException extends Exception {
    public RetryException(String message) {
        super(message);
    }
}
