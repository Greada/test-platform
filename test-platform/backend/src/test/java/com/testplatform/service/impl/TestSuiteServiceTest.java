package com.testplatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.TestSuite;
import com.testplatform.entity.TestSuiteCase;
import com.testplatform.mapper.ExecutionReportMapper;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.mapper.TestSuiteCaseMapper;
import com.testplatform.mapper.TestSuiteMapper;
import com.testplatform.service.ExecutionService;
import com.testplatform.service.TestSuiteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

/** TestSuiteService unit tests */
@ExtendWith(MockitoExtension.class)
class TestSuiteServiceTest {

    @Mock private TestSuiteMapper testSuiteMapper;
    @Mock private TestSuiteCaseMapper testSuiteCaseMapper;
    @Mock private TestCaseMapper testCaseMapper;
    @Mock private ExecutionReportMapper executionReportMapper;
    @Mock private ExecutionService executionService;

    private TestSuiteService testSuiteService;

    @BeforeEach
    void setUp() {
        testSuiteService =
                new TestSuiteServiceImpl(
                        testSuiteMapper,
                        testSuiteCaseMapper,
                        testCaseMapper,
                        executionReportMapper,
                        executionService);
    }

    @Test
    @DisplayName("[SUITE-CRUD-01] save should insert suite")
    void save_shouldInsertSuite() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setName("Test Suite");
        when(testSuiteMapper.insert(any(TestSuite.class))).thenReturn(1);

        // Act
        Result<Void> result = testSuiteService.save(suite);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        verify(testSuiteMapper).insert(suite);
    }

    @Test
    @DisplayName("[SUITE-CRUD-02] update should update suite")
    void update_shouldUpdateSuite() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setId(1L);
        suite.setName("Updated Suite");
        when(testSuiteMapper.updateById(suite)).thenReturn(1);

        // Act
        Result<Void> result = testSuiteService.update(suite);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
    }

    @Test
    @DisplayName("[SUITE-CRUD-03] deleteById should delete suite and relations")
    void deleteById_shouldDeleteSuiteAndRelations() {
        // Arrange
        when(testSuiteMapper.deleteById(1L)).thenReturn(1);
        when(testSuiteCaseMapper.delete(any())).thenReturn(1);

        // Act
        Result<Void> result = testSuiteService.deleteById(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        verify(testSuiteCaseMapper).delete(any());
    }

    @Test
    @DisplayName("[SUITE-CRUD-04] getById should return suite details")
    void getById_shouldReturnSuite() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setId(1L);
        suite.setName("Test Suite");
        when(testSuiteMapper.selectById(1L)).thenReturn(suite);

        // Act
        Result<TestSuite> result = testSuiteService.getById(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("Test Suite", result.getData().getName());
    }

    @Test
    @DisplayName("[SUITE-CRUD-05] listAll should return all suites")
    void listAll_shouldReturnAll() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setId(1L);
        suite.setName("Suite 1");
        when(testSuiteMapper.selectList(any())).thenReturn(Collections.singletonList(suite));

        // Act
        Result<List<TestSuite>> result = testSuiteService.listAll();

        // Assert
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("[SUITE-ADD-01] addCase should insert relation")
    void addCase_shouldInsertRelation() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setId(1L);
        TestCase tc = new TestCase();
        tc.setId(10L);
        when(testSuiteMapper.selectById(1L)).thenReturn(suite);
        when(testCaseMapper.selectById(10L)).thenReturn(tc);
        when(testSuiteCaseMapper.selectCount(any())).thenReturn(0L);
        when(testSuiteCaseMapper.insert(any(TestSuiteCase.class))).thenReturn(1);

        // Act
        Result<Void> result = testSuiteService.addCase(1L, 10L);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        verify(testSuiteCaseMapper).insert(any(TestSuiteCase.class));
    }

    @Test
    @DisplayName("[SUITE-ADD-02] addCase suite not found should return error")
    void addCase_suiteNotFound_shouldReturnError() {
        // Arrange
        when(testSuiteMapper.selectById(999L)).thenReturn(null);

        // Act
        Result<Void> result = testSuiteService.addCase(999L, 10L);

        // Assert
        assertEquals(404, result.getCode());
        assertEquals("suite not found!", result.getMessage());
    }

    @Test
    @DisplayName("[SUITE-ADD-03] addCase testCase not found should return error")
    void addCase_testCaseNotFound_shouldReturnError() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setId(1L);
        when(testSuiteMapper.selectById(1L)).thenReturn(suite);
        when(testCaseMapper.selectById(999L)).thenReturn(null);

        // Act
        Result<Void> result = testSuiteService.addCase(1L, 999L);

        // Assert
        assertEquals(404, result.getCode());
        assertEquals("testCase not found", result.getMessage());
    }

    @Test
    @DisplayName("[SUITE-ADD-04] addCase already in suite should return error")
    void addCase_alreadyInSuite_shouldReturnError() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setId(1L);
        TestCase tc = new TestCase();
        tc.setId(10L);
        when(testSuiteMapper.selectById(1L)).thenReturn(suite);
        when(testCaseMapper.selectById(10L)).thenReturn(tc);
        when(testSuiteCaseMapper.selectCount(any())).thenReturn(1L);

        // Act
        Result<Void> result = testSuiteService.addCase(1L, 10L);

        // Assert
        assertEquals(409, result.getCode());
        assertEquals("case already in suite", result.getMessage());
    }

    @Test
    @DisplayName("[SUITE-REMOVE-01] removeCase should delete relation")
    void removeCase_shouldDeleteRelation() {
        // Arrange
        when(testSuiteCaseMapper.delete(any())).thenReturn(1);

        // Act
        Result<Void> result = testSuiteService.removeCase(1L, 10L);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
    }

    @Test
    @DisplayName("[SUITE-LIST-01] listCases should return ordered test cases")
    void listCases_shouldReturnOrderedCases() {
        // Arrange
        TestSuiteCase rel1 = new TestSuiteCase();
        rel1.setCaseId(10L);
        rel1.setSortOrder(1);
        TestSuiteCase rel2 = new TestSuiteCase();
        rel2.setCaseId(20L);
        rel2.setSortOrder(2);
        TestCase tc1 = new TestCase();
        tc1.setId(10L);
        tc1.setName("Case 1");
        TestCase tc2 = new TestCase();
        tc2.setId(20L);
        tc2.setName("Case 2");
        when(testSuiteCaseMapper.selectList(any())).thenReturn(Arrays.asList(rel1, rel2));
        when(testCaseMapper.selectBatchIds(any())).thenReturn(Arrays.asList(tc1, tc2));

        // Act
        Result<List<TestCase>> result = testSuiteService.listCases(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals("Case 1", result.getData().get(0).getName());
        assertEquals("Case 2", result.getData().get(1).getName());
    }

    @Test
    @DisplayName("[SUITE-LIST-02] listCases empty should return empty list")
    void listCases_empty_shouldReturnEmptyList() {
        // Arrange
        when(testSuiteCaseMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        Result<List<TestCase>> result = testSuiteService.listCases(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("[SUITE-BATCH-01] batchAddCases should insert all relations")
    void batchAddCases_shouldInsertAllRelations() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setId(1L);
        TestCase tc1 = new TestCase();
        tc1.setId(10L);
        TestCase tc2 = new TestCase();
        tc2.setId(20L);
        when(testSuiteMapper.selectById(1L)).thenReturn(suite);
        when(testCaseMapper.selectById(10L)).thenReturn(tc1);
        when(testCaseMapper.selectById(20L)).thenReturn(tc2);
        when(testSuiteCaseMapper.insert(any(TestSuiteCase.class))).thenReturn(1);

        // Act
        Result<Void> result = testSuiteService.batchAddCases(1L, Arrays.asList(10L, 20L));

        // Assert
        assertEquals(200, result.getCode());
        verify(testSuiteCaseMapper, times(2)).insert(any(TestSuiteCase.class));
    }

    @Test
    @DisplayName("[SUITE-BATCH-02] batchAddCases suite not found should return error")
    void batchAddCases_suiteNotFound_shouldReturnError() {
        // Arrange
        when(testSuiteMapper.selectById(999L)).thenReturn(null);

        // Act
        Result<Void> result = testSuiteService.batchAddCases(999L, Arrays.asList(10L));

        // Assert
        assertEquals(404, result.getCode());
        assertEquals("suite not found", result.getMessage());
    }

    @Test
    @DisplayName("[SUITE-BATCH-03] batchAddCases skips missing test cases")
    void batchAddCases_skipMissingCases() {
        // Arrange
        TestSuite suite = new TestSuite();
        suite.setId(1L);
        TestCase tc1 = new TestCase();
        tc1.setId(10L);
        when(testSuiteMapper.selectById(1L)).thenReturn(suite);
        when(testCaseMapper.selectById(10L)).thenReturn(tc1);
        when(testCaseMapper.selectById(999L)).thenReturn(null);
        when(testSuiteCaseMapper.insert(any(TestSuiteCase.class))).thenReturn(1);

        // Act
        Result<Void> result = testSuiteService.batchAddCases(1L, Arrays.asList(10L, 999L));

        // Assert
        assertEquals(200, result.getCode());
        verify(testSuiteCaseMapper, times(1)).insert(any(TestSuiteCase.class));
    }
}
