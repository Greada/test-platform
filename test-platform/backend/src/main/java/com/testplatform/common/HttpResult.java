package com.testplatform.common;

/**
 * @author admin
 * @version 1.0.0
 */
public class HttpResult {
    private String body;
    private long duration;
    private int statusCode;

    public HttpResult(String body, long duration, int statusCode) {
        this.body = body;
        this.duration = duration;
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public long getDuration() {
        return duration;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
