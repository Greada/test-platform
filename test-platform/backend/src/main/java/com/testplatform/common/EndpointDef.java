package com.testplatform.common;

import lombok.Data;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
public class EndpointDef {
    private String name;
    private String requestUrl;
    private String requestMethod;
    private String requestHeaders;
    private String requestParams;
    private String responseSchema;
}
