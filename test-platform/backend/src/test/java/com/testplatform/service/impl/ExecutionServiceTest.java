package com.testplatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.service.ExecutionService;
import com.testplatform.service.HttpExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

/** ExecutionService unit tests */
@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock private ExecutionRecordMapper executionRecordMapper;
    @Mock private TestCaseMapper testCaseMapper;
    @Mock private HttpExecutor httpExecutor;

    private ExecutionService executionService;

    @BeforeEach
    void setUp() {
        executionService =
                new ExecutionServiceImpl(
                        executionRecordMapper,
                        testCaseMapper,
                        httpExecutor,
                        new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    @DisplayName("[EXEC-SVC-01] execute nonexistent case should return error")
    void execute_nonexistentCase_shouldReturnError() {
        // Arrange
        when(testCaseMapper.selectById(999L)).thenReturn(null);

        // Act
        Result<ExecutionRecord> result = executionService.execute(999L, null);

        // Assert
        assertEquals(404, result.getCode());
        assertEquals("testcase not found!", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("[EXEC-SVC-02] JSON exact match should PASS")
    void execute_jsonMatch_shouldReturnPASS() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "GET request", "{\"code\":200}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("{\"code\":200}", 150L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("PASS", result.getData().getStatus());
        ArgumentCaptor<ExecutionRecord> captor = ArgumentCaptor.forClass(ExecutionRecord.class);
        verify(executionRecordMapper).insert(captor.capture());
        assertEquals(150L, captor.getValue().getExecuteDuration());
    }

    @Test
    @DisplayName("[EXEC-SVC-03] JSON value mismatch should FAIL")
    void execute_jsonMismatch_shouldReturnFAIL() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "GET request", "{\"code\":200}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("{\"code\":400}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("FAIL", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-04] nested JSON match should PASS")
    void execute_nestedJsonMatch_shouldReturnPASS() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "nested", "{\"data\":{\"id\":1}}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult(
                        "{\"data\":{\"id\":1,\"name\":\"test\"}}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals("PASS", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-05] nested JSON mismatch should FAIL")
    void execute_nestedJsonMismatch_shouldReturnFAIL() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "nested", "{\"data\":{\"id\":1}}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("{\"data\":{\"id\":2}}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals("FAIL", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-06] extra fields should PASS (subset matching)")
    void execute_extraFields_shouldReturnPASS() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "subset", "{\"code\":200}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult(
                        "{\"code\":200,\"msg\":\"ok\",\"extra\":\"field\"}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals("PASS", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-07] integer comparison should match")
    void execute_integerMatch_shouldPass() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "integer", "{\"count\":42}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("{\"count\":42}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals("PASS", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-08] float comparison should match")
    void execute_floatMatch_shouldPass() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "float", "{\"price\":19.99}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("{\"price\":19.99}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals("PASS", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-09] non-JSON degrades to text contains match should PASS")
    void execute_textContainsMatch_shouldReturnPASS() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "text", "origin");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("your origin is 1.2.3.4", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals("PASS", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-10] non-JSON text mismatch should FAIL")
    void execute_textContainsMismatch_shouldReturnFAIL() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "text", "origin");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("{\"data\":\"other\"}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals("FAIL", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-11] null actualResult should FAIL")
    void execute_nullBody_shouldReturnFAIL() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "test", "{\"code\":200}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult(null, 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertEquals("FAIL", result.getData().getStatus());
    }

    @Test
    @DisplayName("[EXEC-SVC-12] HTTP execution exception should ERROR")
    void execute_httpException_shouldReturnERROR() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "Test Case", "{}");
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenThrow(new RuntimeException("Connection refused"));
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert - Result.error(500, msg) returns code=500, data=null
        assertEquals(500, result.getCode());
        assertEquals("Connection refused", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("[EXEC-SVC-13] execution record should include request detail")
    void execute_shouldIncludeRequestDetail() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "test", "{}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("{\"ok\":true}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L, null);

        // Assert
        assertNotNull(result.getData().getRequestDetail());
        assertFalse(result.getData().getRequestDetail().isEmpty());
    }

    @Test
    @DisplayName("[EXEC-SVC-14] listByTestCaseId should return desc-ordered list")
    void listByTestCaseId_shouldReturnDescOrderByTime() {
        // Arrange
        ExecutionRecord rec1 = new ExecutionRecord();
        rec1.setExecuteTime(java.time.LocalDateTime.of(2026, 6, 13, 10, 0));
        ExecutionRecord rec2 = new ExecutionRecord();
        rec2.setExecuteTime(java.time.LocalDateTime.of(2026, 6, 12, 10, 0));
        when(executionRecordMapper.selectList(any())).thenReturn(Arrays.asList(rec1, rec2));

        // Act
        Result<List<ExecutionRecord>> result = executionService.listByTestCaseId(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals(
                "2026-06-13T10:00",
                result.getData().get(0).getExecuteTime().toString().substring(0, 16));
    }

    @Test
    @DisplayName("[EXEC-SVC-15] execute without reportId should pass null correctly")
    void execute_withoutReportId_shouldPassNull() {
        // Arrange
        TestCase testCase = buildTestCase("TC-001", "test", "{}");
        com.testplatform.common.HttpResult httpResult =
                new com.testplatform.common.HttpResult("{\"ok\":true}", 100L, 200);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);
        when(httpExecutor.execute(any())).thenReturn(httpResult);
        when(executionRecordMapper.insert(any(ExecutionRecord.class))).thenReturn(1);

        // Act
        Result<ExecutionRecord> result = executionService.execute(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("PASS", result.getData().getStatus());
    }

    private TestCase buildTestCase(String testNo, String name, String expectedResult) {
        TestCase tc = new TestCase();
        tc.setId(1L);
        tc.setTestNo(testNo);
        tc.setName(name);
        tc.setExpectedResult(expectedResult);
        tc.setRequestUrl("https://httpbin.org/get");
        tc.setRequestMethod("GET");
        return tc;
    }
}
