package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    @Autowired
    public ExecutionServiceImpl(
            ExecutionRecordMapper executionRecordMapper,
            TestCaseMapper testCaseMapper,
            HttpExecutor httpExecutor) {
        this.executionRecordMapper = executionRecordMapper;
        this.testCaseMapper = testCaseMapper;
        this.httpExecutor = httpExecutor;
    }

    @Override
    public Result<ExecutionRecord> execute(Long testCaseId, Long reportId) {
        TestCase testCase = testCaseMapper.selectById(testCaseId);
        if (testCase == null) {
            return Result.error("testcase not found!");
        }
        try {
            HttpResult httpResult = httpExecutor.execute(testCase);
            ExecutionRecord record = new ExecutionRecord();
            record.setTestCaseId(testCaseId);
            record.setReportId(reportId);
            record.setTestNo(testCase.getTestNo());
            record.setCaseName(testCase.getName());
            record.setExecuteDuration(httpResult.getDuration());
            record.setActualResult(httpResult.getBody());
            record.setRequestDetail(
                    String.format(
                            "[requestMethod] %s [requestUrl] %s \n [requestHeaders] %s \n [requestParams] %s \n",
                            testCase.getRequestMethod(),
                            testCase.getRequestUrl(),
                            testCase.getRequestHeaders(),
                            testCase.getRequestParams()));
            record.setResponseDetail(String.format("[responseBody] \n %s", httpResult.getBody()));

            String expected = testCase.getExpectedResult();
            String actual = httpResult.getBody();
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> expectedMap =
                        mapper.readValue(expected, new TypeReference<Map<String, Object>>() {});
                Map<String, Object> actualMap =
                        mapper.readValue(actual, new TypeReference<Map<String, Object>>() {});
                boolean allMatch =
                        expectedMap.entrySet().stream()
                                .allMatch(
                                        entry -> {
                                            Object expectedValue = entry.getValue();
                                            Object actualValue = actualMap.get(entry.getKey());
                                            return matchValue(expectedValue, actualValue);
                                        });
                record.setStatus(allMatch ? "PASS" : "FAIL");
            } catch (Exception e) {
                record.setStatus(actual != null && actual.contains(expected) ? "PASS" : "FAIL");
            }

            executionRecordMapper.insert(record);
            return Result.success(record);
        } catch (Exception e) {
            ExecutionRecord record = new ExecutionRecord();
            record.setTestCaseId(testCaseId);
            record.setReportId(reportId);
            record.setTestNo(testCase.getTestNo());
            record.setCaseName(testCase.getName());
            record.setRequestDetail(
                    String.format(
                            "[requestMethod] %s [requestUrl] %s \n [requestHeaders] %s \n [requestParams] %s \n",
                            testCase.getRequestMethod(),
                            testCase.getRequestUrl(),
                            testCase.getRequestHeaders(),
                            testCase.getRequestParams()));
            record.setStatus("ERROR");
            record.setActualResult(e.getMessage());
            executionRecordMapper.insert(record);
            return Result.error(500, e.getMessage());
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
}
