package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.common.HttpResult;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.service.ExecutionService;
import com.testplatform.service.HttpExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class ExecutionServiceImpl implements ExecutionService {
    private final ExecutionRecordMapper executionRecordMapper;
    private final TestCaseMapper testCaseMapper;
    private final HttpExecutor httpExecutor;
    private final ObjectMapper objectMapper;

    @Autowired
    public ExecutionServiceImpl(
            ExecutionRecordMapper executionRecordMapper,
            TestCaseMapper testCaseMapper,
            HttpExecutor httpExecutor,
            ObjectMapper objectMapper) {
        this.executionRecordMapper = executionRecordMapper;
        this.testCaseMapper = testCaseMapper;
        this.httpExecutor = httpExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<ExecutionRecord> execute(Long testCaseId, Long reportId) {
        // 1.find testcase
        TestCase testCase = testCaseMapper.selectById(testCaseId);
        if (testCase == null) {
            return Result.notFound("testcase not found!");
        }

        // 2.execution http
        HttpResult httpResult;
        try {
            httpResult = httpExecutor.execute(testCase);
        } catch (Exception e) {
            return buildErrorResult(testCaseId, reportId, testCase, e);
        }

        // 3.build record
        ExecutionRecord record =
                buildBaseRecord(testCaseId, reportId, testCase, httpResult.getDuration());
        record.setActualResult(httpResult.getBody());
        record.setResponseDetail(httpResult.getBody());
        record.setStatus(resolveStatus(testCase.getExpectedResult(), httpResult.getBody()));
        executionRecordMapper.insert(record);
        return Result.success(record);
    }

    private String resolveStatus(String expected, String actual) {
        if (actual == null) {
            return "FAIL";
        }
        try {
            Map<String, Object> expectedMap =
                    objectMapper.readValue(expected, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> actualMap =
                    objectMapper.readValue(actual, new TypeReference<Map<String, Object>>() {});
            boolean allMatch =
                    expectedMap.entrySet().stream()
                            .allMatch(e -> matchValue(e.getValue(), actualMap.get(e.getKey())));
            return allMatch ? "PASS" : "FAIL";
        } catch (Exception e) {
            return actual.contains(expected) ? "PASS" : "FAIL";
        }
    }

    private Result<ExecutionRecord> buildErrorResult(
            Long testCaseId, Long reportId, TestCase testCase, Exception e) {
        String errMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
        ExecutionRecord record = buildBaseRecord(testCaseId, reportId, testCase, 0L);
        record.setStatus("ERROR");
        record.setActualResult(errMsg);
        record.setResponseDetail(buildErrorDetailJson(errMsg));
        executionRecordMapper.insert(record);
        return Result.error(500, errMsg);
    }

    private String buildErrorDetailJson(String errMsg) {
        Map<String, Object> map = new HashMap<>();
        map.put("error", errMsg);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"unknown\"}";
        }
    }

    private boolean matchValue(Object expectedValue, Object actualValue) {
        if (expectedValue == null) {
            return actualValue == null;
        }
        if (actualValue == null) {
            return false;
        }

        if (expectedValue instanceof Map && actualValue instanceof Map) {
            Map<?, ?> expMap = (Map<?, ?>) expectedValue;
            Map<?, ?> actMap = (Map<?, ?>) actualValue;
            return expMap.entrySet().stream()
                    .allMatch(
                            subEntry ->
                                    matchValue(subEntry.getValue(), actMap.get(subEntry.getKey())));
        }

        if (expectedValue instanceof Number && actualValue instanceof Number) {
            return ((Number) expectedValue).doubleValue() == ((Number) actualValue).doubleValue();
        }

        return expectedValue.equals(actualValue);
    }

    @Override
    public Result<List<ExecutionRecord>> listByTestCaseId(Long testCaseId) {
        try {
            QueryWrapper<ExecutionRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("test_case_id", testCaseId).orderByDesc("execute_time");
            List<ExecutionRecord> recordList = executionRecordMapper.selectList(wrapper);
            return Result.success(recordList);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<ExecutionRecord> execute(Long testcaseId) {
        return execute(testcaseId, null);
    }

    // build base record
    private ExecutionRecord buildBaseRecord(
            Long testCaseId, Long reportId, TestCase testCase, Long duration) {
        ExecutionRecord record = new ExecutionRecord();
        record.setTestCaseId(testCaseId);
        record.setReportId(reportId);
        record.setTestNo(testCase.getTestNo());
        record.setCaseName(testCase.getName());
        record.setExecuteDuration(duration);
        record.setRequestDetail(buildRequestDetailJson(testCase));
        return record;
    }

    // build request detail JSON
    private String buildRequestDetailJson(TestCase testCase) {
        Map<String, Object> map = new HashMap<>();
        map.put("requestMethod", testCase.getRequestMethod());
        map.put("requestUrl", testCase.getRequestUrl());
        map.put("requestHeaders", testCase.getRequestHeaders());
        map.put("requestParams", testCase.getRequestParams());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
