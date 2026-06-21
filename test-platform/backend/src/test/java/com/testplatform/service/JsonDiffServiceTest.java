package com.testplatform.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.testplatform.common.JsonDiffResult;
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

/** JsonDiffService unit tests */
@ExtendWith(MockitoExtension.class)
class JsonDiffServiceTest {

    @Mock private ExecutionRecordMapper executionRecordMapper;
    @Mock private TestCaseMapper testCaseMapper;

    private JsonDiffService jsonDiffService;

    @BeforeEach
    void setUp() {
        jsonDiffService =
                new JsonDiffService(
                        executionRecordMapper,
                        testCaseMapper,
                        new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    @DisplayName("[DIFF-01] exact match should match=true with empty differences")
    void diff_exactMatch_shouldBeMatch() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"code\":200}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"code\":200}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isMatch());
        assertTrue(result.getData().getDifferences().isEmpty());
    }

    @Test
    @DisplayName("[DIFF-02] value mismatch should record MISMATCH")
    void diff_valueMismatch_shouldRecordMismatch() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"code\":400}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"code\":200}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertFalse(result.getData().isMatch());
        assertEquals(1, result.getData().getDifferences().size());
        assertEquals("MISMATCH", result.getData().getDifferences().get(0).getType());
    }

    @Test
    @DisplayName("[DIFF-03] missing field should record MISSING")
    void diff_missingField_shouldRecordMissing() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"code\":200}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"code\":200,\"data\":{}}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertFalse(result.getData().isMatch());
        assertTrue(
                result.getData().getDifferences().stream()
                        .anyMatch(
                                d ->
                                        "MISSING".equals(d.getType())
                                                && "data".equals(d.getFieldPath())));
    }

    @Test
    @DisplayName("[DIFF-04] extra field should record EXTRA")
    void diff_extraField_shouldRecordExtra() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"code\":200,\"extra\":\"x\"}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"code\":200}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert - EXTRA fields are recorded but match stays true per subset matching logic
        assertTrue(
                result.getData().getDifferences().stream()
                        .anyMatch(
                                d ->
                                        "EXTRA".equals(d.getType())
                                                && "extra".equals(d.getFieldPath())));
    }

    @Test
    @DisplayName("[DIFF-05] nested object mismatch")
    void diff_nestedObjectMismatch() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"data\":{\"id\":2}}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"data\":{\"id\":1}}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertFalse(result.getData().isMatch());
        JsonDiffResult.DiffEntry entry = result.getData().getDifferences().get(0);
        assertEquals("data.id", entry.getFieldPath());
        assertEquals("MISMATCH", entry.getType());
    }

    @Test
    @DisplayName("[DIFF-06] deep nested comparison")
    void diff_deepNested() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"a\":{\"b\":{\"c\":2}}}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"a\":{\"b\":{\"c\":1}}}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertFalse(result.getData().isMatch());
        assertEquals("a.b.c", result.getData().getDifferences().get(0).getFieldPath());
    }

    @Test
    @DisplayName("[DIFF-07] list extra item should record EXTRA")
    void diff_listExtraItem() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"items\":[1,2,3]}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"items\":[1,2]}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertFalse(result.getData().isMatch());
        assertTrue(
                result.getData().getDifferences().stream()
                        .anyMatch(
                                d ->
                                        "EXTRA".equals(d.getType())
                                                && "items[2]".equals(d.getFieldPath())));
    }

    @Test
    @DisplayName("[DIFF-08] list element mismatch should record MISMATCH")
    void diff_listElementMismatch() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"items\":[1,3]}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"items\":[1,2]}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertFalse(result.getData().isMatch());
        assertTrue(
                result.getData().getDifferences().stream()
                        .anyMatch(
                                d ->
                                        "MISMATCH".equals(d.getType())
                                                && "items[1]".equals(d.getFieldPath())));
    }

    @Test
    @DisplayName("[DIFF-09] list nested object comparison")
    void diff_listNestedObject() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"data\":[{\"id\":2}]}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"data\":[{\"id\":1}]}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertFalse(result.getData().isMatch());
        JsonDiffResult.DiffEntry entry = result.getData().getDifferences().get(0);
        assertTrue(entry.getFieldPath().contains("data[0].id"));
        assertEquals("MISMATCH", entry.getType());
    }

    @Test
    @DisplayName("[DIFF-10] integer comparison should match")
    void diff_integerMatch() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"count\":42}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"count\":42}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertTrue(result.getData().isMatch());
    }

    @Test
    @DisplayName("[DIFF-11] float comparison should match")
    void diff_floatMatch() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"price\":19.99}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"price\":19.99}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertTrue(result.getData().isMatch());
    }

    @Test
    @DisplayName("[DIFF-12] execution record not found should return error")
    void diff_recordNotFound() {
        // Arrange
        when(executionRecordMapper.selectById(999L)).thenReturn(null);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(999L);

        // Assert
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("[DIFF-13] test case not found should return error")
    void diff_caseNotFound() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setTestCaseId(1L);
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(null);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("[DIFF-14] non-JSON degrades to exact comparison")
    void diff_nonJson() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("origin");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("origin");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertTrue(result.getData().isMatch());
    }

    @Test
    @DisplayName("[DIFF-15] suggestedExpected should not be empty")
    void diff_suggestedExpected_shouldNotBeEmpty() {
        // Arrange
        ExecutionRecord record = new ExecutionRecord();
        record.setActualResult("{\"code\":200}");
        record.setTestCaseId(1L);
        TestCase testCase = new TestCase();
        testCase.setExpectedResult("{\"code\":200}");
        when(executionRecordMapper.selectById(1L)).thenReturn(record);
        when(testCaseMapper.selectById(1L)).thenReturn(testCase);

        // Act
        Result<JsonDiffResult> result = jsonDiffService.diff(1L);

        // Assert
        assertNotNull(result.getData().getSuggestedExpected());
        assertFalse(result.getData().getSuggestedExpected().isEmpty());
    }
}
