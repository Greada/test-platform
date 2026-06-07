package com.testplatform.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
public class JsonDiffResult {
    private boolean match;
    private List<DiffEntry> differences;
    private String suggestedExpected;
    private String expectedResult;
    private String actualResult;

    public JsonDiffResult() {
        this.differences = new ArrayList<>();
        this.match = true;
    }

    @Data
    public static class DiffEntry {
        private String fieldPath;
        private Object expectedValue;
        private Object actualValue;
        private String type; // MISMATCH MISSING EXTRA

        public DiffEntry(String fieldPath, Object expectedValue, Object actualValue, String type) {
            this.fieldPath = fieldPath;
            this.expectedValue = expectedValue;
            this.actualValue = actualValue;
            this.type = type;
        }
    }
}
