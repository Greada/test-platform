package com.testplatform.common;

import lombok.Getter;

/**
 * @author admin
 * @version 1.0.0
 */
@Getter
public class HttpResult {
    private final String body;
    private final long duration;
    private final int statusCode;

    public HttpResult(String body, long duration, int statusCode) {
        this.body = body;
        this.duration = duration;
        this.statusCode = statusCode;
    }
}
