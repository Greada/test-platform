package com.testplatform.common;

/**
 * @author admin
 * @version 1.0.0
 */
import lombok.Data;

@Data
public class ErrorPatternItem {
    private String requestUrl;
    private String requestMethod;
    private int total;
    private int pass;
    private int fail;
    private int error;
    private String passRate;
}
