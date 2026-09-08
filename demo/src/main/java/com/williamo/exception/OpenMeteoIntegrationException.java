package com.williamo.exception;

import org.springframework.http.HttpStatus;

public class OpenMeteoIntegrationException extends RuntimeException {

    private final HttpStatus status;
    private final String source;
    private final Integer remoteStatus;

    public OpenMeteoIntegrationException(HttpStatus status, String source, Integer remoteStatus, String message) {
        super(message);
        this.status = status;
        this.source = source;
        this.remoteStatus = remoteStatus;
    }

    public OpenMeteoIntegrationException(
            HttpStatus status,
            String source,
            Integer remoteStatus,
            String message,
            Throwable cause) {
        super(message, cause);
        this.status = status;
        this.source = source;
        this.remoteStatus = remoteStatus;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public Integer getRemoteStatus() {
        return remoteStatus;
    }
}
