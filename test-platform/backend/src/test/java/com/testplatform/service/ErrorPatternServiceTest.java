package com.testplatform.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.testplatform.common.ErrorPatternResult;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.TestCaseMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

/** ErrorPatternService unit tests */
@ExtendWith(MockitoExtension.class)
class ErrorPatternServiceTest {

    @Mock private ExecutionRecordMapper executionRecordMapper;
    @Mock private TestCaseMapper testCaseMapper;

    private ErrorPatternService errorPatternService;

    @BeforeEach
    void setUp() {
        errorPatternService = new ErrorPatternService(executionRecordMapper, testCaseMapper);
    }

    @Test
    @DisplayName("[ERR-PAT-01] empty records should return error pattern result with null items")
    void analyze_emptyRecords_shouldReturnErrorPatternResult() {
        // Arrange
        when(executionRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        Result<ErrorPatternResult> result = errorPatternService.analyze(1L);

        // Assert - items is null when no records exist (per source code)
        assertEquals(200, result.getCode());
        assertNull(result.getData().getItems());
        assertNull(result.getData().getWorstEndpoint());
    }

    @Test
    @DisplayName("[ERR-PAT-02] single PASS should count correctly")
    void analyze_singlePass_shouldCountCorrectly() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setStatus("PASS");
        record.setTestCaseId(1L);
        record.setExecuteTime(LocalDateTime.now());
        when(executionRecordMapper.selectList(any())).thenReturn(Arrays.asList(record));

        TestCase testCase = new TestCase();
        testCase.setRequestUrl("https://httpbin.org/get");
        testCase.setRequestMethod("GET");
        when(testCaseMapper.selectBatchIds(any())).thenReturn(Arrays.asList(testCase));

        // Act
        Result<ErrorPatternResult> result = errorPatternService.analyze(1L);

        // Assert
        assertEquals(1, result.getData().getItems().get(0).getTotal());
        assertEquals(1, result.getData().getItems().get(0).getPass());
        assertEquals(0, result.getData().getItems().get(0).getFail());
        assertEquals(0, result.getData().getItems().get(0).getError());
        assertEquals("100.00%", result.getData().getItems().get(0).getPassRate());
    }

    @Test
    @DisplayName("[ERR-PAT-03] single FAIL should count correctly")
    void analyze_singleFail_shouldCountCorrectly() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setStatus("FAIL");
        record.setTestCaseId(1L);
        record.setExecuteTime(LocalDateTime.now());
        when(executionRecordMapper.selectList(any())).thenReturn(Arrays.asList(record));

        TestCase testCase = new TestCase();
        testCase.setRequestUrl("https://httpbin.org/post");
        testCase.setRequestMethod("POST");
        when(testCaseMapper.selectBatchIds(any())).thenReturn(Arrays.asList(testCase));

        // Act
        Result<ErrorPatternResult> result = errorPatternService.analyze(1L);

        // Assert
        assertEquals(1, result.getData().getItems().get(0).getTotal());
        assertEquals(0, result.getData().getItems().get(0).getPass());
        assertEquals(1, result.getData().getItems().get(0).getFail());
        assertEquals("0.00%", result.getData().getItems().get(0).getPassRate());
    }

    @Test
    @DisplayName("[ERR-PAT-04] same URL+Method should aggregate into one group")
    void analyze_sameEndpoint_shouldAggregate() {
        // Arrange
        ExecutionRecord rec1 = new ExecutionRecord();
        rec1.setStatus("PASS");
        rec1.setTestCaseId(1L);
        ExecutionRecord rec2 = new ExecutionRecord();
        rec2.setStatus("FAIL");
        rec2.setTestCaseId(2L);
        when(executionRecordMapper.selectList(any())).thenReturn(Arrays.asList(rec1, rec2));

        TestCase tc1 = new TestCase();
        tc1.setId(1L);
        tc1.setRequestUrl("https://httpbin.org/get");
        tc1.setRequestMethod("GET");
        TestCase tc2 = new TestCase();
        tc2.setId(2L);
        tc2.setRequestUrl("https://httpbin.org/get");
        tc2.setRequestMethod("GET");
        when(testCaseMapper.selectBatchIds(any())).thenReturn(Arrays.asList(tc1, tc2));

        // Act
        Result<ErrorPatternResult> result = errorPatternService.analyze(1L);

        // Assert
        assertEquals(1, result.getData().getItems().size());
        assertEquals(2, result.getData().getItems().get(0).getTotal());
        assertEquals(1, result.getData().getItems().get(0).getPass());
        assertEquals(1, result.getData().getItems().get(0).getFail());
    }

    @Test
    @DisplayName("[ERR-PAT-05] mixed records should count correctly")
    void analyze_mixedStatus_shouldCountCorrectly() {
        // Arrange
        ExecutionRecord rec1 = new ExecutionRecord();
        rec1.setStatus("PASS");
        rec1.setTestCaseId(1L);
        ExecutionRecord rec2 = new ExecutionRecord();
        rec2.setStatus("PASS");
        rec2.setTestCaseId(1L);
        ExecutionRecord rec3 = new ExecutionRecord();
        rec3.setStatus("PASS");
        rec3.setTestCaseId(1L);
        ExecutionRecord rec4 = new ExecutionRecord();
        rec4.setStatus("FAIL");
        rec4.setTestCaseId(2L);
        ExecutionRecord rec5 = new ExecutionRecord();
        rec5.setStatus("FAIL");
        rec5.setTestCaseId(2L);
        ExecutionRecord rec6 = new ExecutionRecord();
        rec6.setStatus("ERROR");
        rec6.setTestCaseId(3L);
        when(executionRecordMapper.selectList(any()))
                .thenReturn(Arrays.asList(rec1, rec2, rec3, rec4, rec5, rec6));

        TestCase tc1 = new TestCase();
        tc1.setId(1L);
        tc1.setRequestUrl("https://httpbin.org/get");
        tc1.setRequestMethod("GET");
        TestCase tc2 = new TestCase();
        tc2.setId(2L);
        tc2.setRequestUrl("https://httpbin.org/post");
        tc2.setRequestMethod("POST");
        TestCase tc3 = new TestCase();
        tc3.setId(3L);
        tc3.setRequestUrl("https://httpbin.org/put");
        tc3.setRequestMethod("PUT");
        when(testCaseMapper.selectBatchIds(any())).thenReturn(Arrays.asList(tc1, tc2, tc3));

        // Act
        Result<ErrorPatternResult> result = errorPatternService.analyze(1L);

        // Assert - item 0 = get (3 PASS = 100%), item 1 = post (2 FAIL = 0%), item 2 = put (1
        // ERROR)
        assertEquals(3, result.getData().getItems().size());
        assertEquals("100.00%", result.getData().getItems().get(0).getPassRate());
        assertEquals("0.00%", result.getData().getItems().get(1).getPassRate());
    }

    @Test
    @DisplayName("[ERR-PAT-06] worst endpoint should be identified (lowest pass rate)")
    void analyze_worstEndpoint_shouldIdentifyLowestRate() {
        // Arrange
        ExecutionRecord rec1 = new ExecutionRecord();
        rec1.setStatus("PASS");
        rec1.setTestCaseId(1L);
        ExecutionRecord rec2 = new ExecutionRecord();
        rec2.setStatus("FAIL");
        rec2.setTestCaseId(2L);
        when(executionRecordMapper.selectList(any())).thenReturn(Arrays.asList(rec1, rec2));

        TestCase tc1 = new TestCase();
        tc1.setId(1L);
        tc1.setRequestUrl("https://httpbin.org/healthy");
        tc1.setRequestMethod("GET");
        TestCase tc2 = new TestCase();
        tc2.setId(2L);
        tc2.setRequestUrl("https://httpbin.org/broken");
        tc2.setRequestMethod("GET");
        when(testCaseMapper.selectBatchIds(any())).thenReturn(Arrays.asList(tc1, tc2));

        // Act
        Result<ErrorPatternResult> result = errorPatternService.analyze(1L);

        // Assert
        assertNotNull(result.getData().getWorstEndpoint());
        assertTrue(result.getData().getWorstEndpoint().contains("broken"));
    }

    @Test
    @DisplayName("[ERR-PAT-07] pass rate format should keep two decimal places")
    void analyze_passRateFormat_shouldBeTwoDecimalPlaces() {
        // Arrange: 1 pass / 3 total = 33.333...%
        ExecutionRecord rec1 = new ExecutionRecord();
        rec1.setStatus("PASS");
        rec1.setTestCaseId(1L);
        ExecutionRecord rec2 = new ExecutionRecord();
        rec2.setStatus("FAIL");
        rec2.setTestCaseId(2L);
        ExecutionRecord rec3 = new ExecutionRecord();
        rec3.setStatus("FAIL");
        rec3.setTestCaseId(2L);
        when(executionRecordMapper.selectList(any())).thenReturn(Arrays.asList(rec1, rec2, rec3));

        TestCase tc1 = new TestCase();
        tc1.setId(1L);
        tc1.setRequestUrl("https://httpbin.org/api");
        tc1.setRequestMethod("GET");
        TestCase tc2 = new TestCase();
        tc2.setId(2L);
        tc2.setRequestUrl("https://httpbin.org/api");
        tc2.setRequestMethod("GET");
        when(testCaseMapper.selectBatchIds(any())).thenReturn(Arrays.asList(tc1, tc2));

        // Act
        Result<ErrorPatternResult> result = errorPatternService.analyze(1L);

        // Assert
        assertEquals("33.33%", result.getData().getItems().get(0).getPassRate());
    }

    @Test
    @DisplayName("[ERR-PAT-08] deleted case should show unknown URL")
    void analyze_deletedCase_shouldUseUnknown() {
        // Arrange: record exists but testCase does not
        ExecutionRecord record = new ExecutionRecord();
        record.setStatus("ERROR");
        record.setTestCaseId(999L);
        when(executionRecordMapper.selectList(any())).thenReturn(Arrays.asList(record));
        when(testCaseMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        // Act
        Result<ErrorPatternResult> result = errorPatternService.analyze(1L);

        // Assert
        assertEquals(1, result.getData().getItems().size());
        assertEquals("unknown", result.getData().getItems().get(0).getRequestUrl());
        assertEquals("?", result.getData().getItems().get(0).getRequestMethod());
    }
}
