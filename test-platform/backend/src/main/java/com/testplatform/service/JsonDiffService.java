package com.testplatform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.common.JsonDiffResult;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.TestCaseMapper;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class JsonDiffService {
    private final ExecutionRecordMapper executionRecordMapper;
    private final TestCaseMapper testCaseMapper;
    private final ObjectMapper objectMapper;

    public JsonDiffService(
            ExecutionRecordMapper executionRecordMapper,
            TestCaseMapper testCaseMapper,
            ObjectMapper objectMapper) {
        this.executionRecordMapper = executionRecordMapper;
        this.testCaseMapper = testCaseMapper;
        this.objectMapper = objectMapper;
    }

    public Result<JsonDiffResult> diff(Long executionRecordId) {
        try {
            ExecutionRecord record = executionRecordMapper.selectById(executionRecordId);
            if (record == null) {
                return Result.error("execution record not found");
            }

            TestCase testCase = testCaseMapper.selectById(record.getTestCaseId());
            if (testCase == null) {
                return Result.error("test case not found");
            }

            String expected = testCase.getExpectedResult();
            String actual = record.getActualResult();

            JsonDiffResult jsonDiffResult = new JsonDiffResult();
            try {
                Map<String, Object> expectedMap =
                        objectMapper.readValue(
                                expected, new TypeReference<Map<String, Object>>() {});
                Map<String, Object> actualMap =
                        objectMapper.readValue(actual, new TypeReference<Map<String, Object>>() {});
                compareMaps(expectedMap, actualMap, "", jsonDiffResult);
                jsonDiffResult.setSuggestedExpected(formatJson(actual));
            } catch (Exception e) {
                JsonDiffResult.DiffEntry diffEntry =
                        new JsonDiffResult.DiffEntry("", expected, actual, "MISMATCH");
                jsonDiffResult.getDifferences().add(diffEntry);
                jsonDiffResult.setMatch(expected != null && expected.equals(actual));
                jsonDiffResult.setSuggestedExpected(actual);
            }

            jsonDiffResult.setExpectedResult(expected);
            jsonDiffResult.setActualResult(actual);

            return Result.success(jsonDiffResult);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    private String formatJson(String json) {
        try {
            Object object = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (Exception e) {
            return json;
        }
    }

    private void compareMaps(
            Map<String, Object> expectedMap,
            Map<String, Object> actualMap,
            String prefix,
            JsonDiffResult jsonDiffResult) {
        for (Map.Entry<String, Object> expEntry : expectedMap.entrySet()) {
            String key = expEntry.getKey();
            Object expVal = expEntry.getValue();
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            Object actVal = actualMap.get(key);

            if (!actualMap.containsKey(key)) {
                jsonDiffResult
                        .getDifferences()
                        .add(new JsonDiffResult.DiffEntry(fullPath, expVal, null, "MISSING"));
                jsonDiffResult.setMatch(false);
            } else if (expVal instanceof Map && actVal instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> expMap = (Map<String, Object>) expVal;
                @SuppressWarnings("unchecked")
                Map<String, Object> actMap = (Map<String, Object>) actVal;
                compareMaps(expMap, actMap, fullPath, jsonDiffResult);
            } else if (expVal instanceof List && actVal instanceof List) {
                compareLists((List<?>) expVal, (List<?>) actVal, fullPath, jsonDiffResult);
            } else if (!valuesMatch(expVal, actVal)) {
                jsonDiffResult
                        .getDifferences()
                        .add(new JsonDiffResult.DiffEntry(fullPath, expVal, actVal, "MISMATCH"));
                jsonDiffResult.setMatch(false);
            }
        }

        for (Map.Entry<String, Object> actEntry : actualMap.entrySet()) {
            if (!expectedMap.containsKey(actEntry.getKey())) {
                String fullPath =
                        prefix.isEmpty() ? actEntry.getKey() : prefix + "." + actEntry.getKey();
                jsonDiffResult
                        .getDifferences()
                        .add(
                                new JsonDiffResult.DiffEntry(
                                        fullPath, null, actEntry.getValue(), "EXTRA"));
            }
        }
    }

    private void compareLists(
            List<?> expVal, List<?> actVal, String fullPath, JsonDiffResult jsonDiffResult) {
        int maxSize = Math.max(expVal.size(), actVal.size());
        for (int i = 0; i < maxSize; i++) {
            String itemPath = fullPath + "[" + i + "]";
            if (i >= expVal.size()) {
                jsonDiffResult
                        .getDifferences()
                        .add(new JsonDiffResult.DiffEntry(itemPath, null, actVal.get(i), "EXTRA"));
                jsonDiffResult.setMatch(false);
            } else if (i >= actVal.size()) {
                jsonDiffResult
                        .getDifferences()
                        .add(
                                new JsonDiffResult.DiffEntry(
                                        itemPath, expVal.get(i), null, "MISSING"));
            } else {
                Object expItem = expVal.get(i);
                Object actItem = actVal.get(i);
                if (expItem instanceof Map && actItem instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> expMap = (Map<String, Object>) expItem;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> actMap = (Map<String, Object>) actItem;

                    compareMaps(expMap, actMap, itemPath, jsonDiffResult);
                } else if (!valuesMatch(expItem, actItem)) {
                    jsonDiffResult
                            .getDifferences()
                            .add(
                                    new JsonDiffResult.DiffEntry(
                                            itemPath, expItem, actItem, "MISMATCH"));
                    jsonDiffResult.setMatch(false);
                }
            }
        }
    }

    private boolean valuesMatch(Object expVal, Object actVal) {
        if (expVal == null) {
            return actVal == null;
        }
        if (actVal == null) {
            return false;
        }
        if (expVal instanceof Number && actVal instanceof Number) {
            return ((Number) expVal).doubleValue() == ((Number) actVal).doubleValue();
        }
        return expVal.equals(actVal);
    }
}
