package io.aston.nextstep.service;

public class HttpException extends RuntimeException {
    private final int status;
    private final String body;
    public HttpException(int status, String body) {
        super("status "+status);
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
