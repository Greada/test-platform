package com.testplatform.common;

import lombok.Data;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
public class ErrorPatternResult {
    private List<ErrorPatternItem> items;
    private String worstEndpoint;
}
