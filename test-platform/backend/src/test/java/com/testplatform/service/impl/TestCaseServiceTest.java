package com.testplatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.service.TestCaseService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

/** TestCaseService CRUD unit tests */
@ExtendWith(MockitoExtension.class)
class TestCaseServiceTest {

    @Mock private TestCaseMapper testCaseMapper;

    private TestCaseService testCaseService;

    @BeforeEach
    void setUp() {
        testCaseService = new TestCaseServiceImpl(testCaseMapper);
    }

    @Test
    @DisplayName("[TC-CRUD-01] save should insert and return success")
    void save_shouldInsertAndReturnSuccess() {
        // Arrange
        TestCase tc = new TestCase();
        tc.setName("Test Create");
        tc.setRequestUrl("https://httpbin.org/get");
        tc.setRequestMethod("GET");
        tc.setExpectedResult("{}");
        when(testCaseMapper.insert(any(TestCase.class))).thenReturn(1);

        // Act
        Result<Void> result = testCaseService.save(tc);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        verify(testCaseMapper).insert(tc);
    }

    @Test
    @DisplayName("[TC-CRUD-02] update should call mapper updateById")
    void update_shouldCallMapperUpdate() {
        // Arrange
        TestCase tc = new TestCase();
        tc.setId(1L);
        tc.setName("Updated");
        when(testCaseMapper.updateById(tc)).thenReturn(1);

        // Act
        Result<Void> result = testCaseService.update(tc);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        verify(testCaseMapper).updateById(tc);
    }

    @Test
    @DisplayName("[TC-CRUD-03] deleteById should succeed")
    void deleteById_shouldSucceed() {
        // Arrange
        when(testCaseMapper.deleteById(1L)).thenReturn(1);

        // Act
        Result<Void> result = testCaseService.deleteById(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
    }

    @Test
    @DisplayName("[TC-CRUD-04] deleteById exception should propagate")
    void deleteById_shouldPropagateException() {
        // Arrange
        when(testCaseMapper.deleteById(1L)).thenThrow(new RuntimeException("db error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> testCaseService.deleteById(1L));
    }

    @Test
    @DisplayName("[TC-CRUD-05] getById should return test case details")
    void getById_shouldReturnTestCase() {
        // Arrange
        TestCase tc = new TestCase();
        tc.setId(1L);
        tc.setName("Test Case");
        when(testCaseMapper.selectById(1L)).thenReturn(tc);

        // Act
        Result<TestCase> result = testCaseService.getById(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("Test Case", result.getData().getName());
    }

    @Test
    @DisplayName("[TC-CRUD-06] getById not found should return success with null")
    void getById_notFound_shouldReturnNull() {
        // Arrange
        when(testCaseMapper.selectById(999L)).thenReturn(null);

        // Act
        Result<TestCase> result = testCaseService.getById(999L);

        // Assert
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("[TC-CRUD-07] listAll should return all test cases")
    void listAll_shouldReturnAll() {
        // Arrange
        TestCase tc = new TestCase();
        tc.setId(1L);
        tc.setName("Test Case");
        when(testCaseMapper.selectList(any())).thenReturn(Collections.singletonList(tc));

        // Act
        Result<List<TestCase>> result = testCaseService.listAll();

        // Assert
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("[TC-CRUD-08] save exception should propagate")
    void save_exception_shouldPropagate() {
        // Arrange
        TestCase tc = new TestCase();
        tc.setName("Test");
        when(testCaseMapper.insert(any(TestCase.class))).thenThrow(new RuntimeException("constraint violation"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> testCaseService.save(tc));
    }

    @Test
    @DisplayName("[TC-CRUD-09] update exception should propagate")
    void update_exception_shouldPropagate() {
        // Arrange
        TestCase tc = new TestCase();
        tc.setId(1L);
        when(testCaseMapper.updateById(any(TestCase.class))).thenThrow(new RuntimeException("db error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> testCaseService.update(tc));
    }

    @Test
    @DisplayName("[TC-CRUD-10] listAll exception should propagate")
    void listAll_exception_shouldPropagate() {
        // Arrange
        when(testCaseMapper.selectList(any())).thenThrow(new RuntimeException("db error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> testCaseService.listAll());
    }
}
