package com.testplatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.testplatform.common.PageResult;
import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.service.TestCaseService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

/** TestCaseService CRUD unit tests */
@ExtendWith(MockitoExtension.class)
class TestCaseServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TestCaseServiceTest.class);
    @Mock private TestCaseMapper testCaseMapper;

    private TestCaseService testCaseService;

    @BeforeEach
    void setUp() {
        testCaseService = new TestCaseServiceImpl(testCaseMapper);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
        TestCase existing = new TestCase();
        existing.setId(1L);
        existing.setName("Original");
        existing.setCreatorId(1L);
        when(testCaseMapper.selectById(1L)).thenReturn(existing);
        when(testCaseMapper.updateById(any(TestCase.class))).thenReturn(1);
        // Arrange
        TestCase tc = new TestCase();
        tc.setId(1L);
        tc.setName("Updated");
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
        TestCase existing = new TestCase();
        existing.setId(1L);
        existing.setCreatorId(1L);
        when(testCaseMapper.selectById(1L)).thenReturn(existing);
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
        TestCase existing = new TestCase();
        existing.setId(1L);
        existing.setCreatorId(1L);
        when(testCaseMapper.selectById(1L)).thenReturn(existing);
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
        tc.setCreatorId(1L);
        when(testCaseMapper.selectById(1L)).thenReturn(tc);

        // Act
        Result<TestCase> result = testCaseService.getById(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("Test Case", result.getData().getName());
    }

    @Test
    @DisplayName("[TC-CRUD-06] getById not found should return 404")
    void getById_notFound_shouldReturn404() {
        // Arrange
        when(testCaseMapper.selectById(999L)).thenReturn(null);

        // Act
        Result<TestCase> result = testCaseService.getById(999L);

        // Assert
        assertEquals(404, result.getCode());
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
        when(testCaseMapper.insert(any(TestCase.class)))
                .thenThrow(new RuntimeException("constraint violation"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> testCaseService.save(tc));
    }

    @Test
    @DisplayName("[TC-CRUD-09] update exception should propagate")
    void update_exception_shouldPropagate() {
        // Arrange
        TestCase tc = new TestCase();
        tc.setId(1L);
        tc.setCreatorId(1L);
        when(testCaseMapper.selectById(1L)).thenReturn(tc);
        when(testCaseMapper.updateById(any(TestCase.class)))
                .thenThrow(new RuntimeException("db error"));

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

    @Test
    @DisplayName("[TC-CRUD-11] page should return first page")
    void page_shouldReturnFirstPage() {
        // Arrange
        TestCase tc1 = new TestCase();
        tc1.setCreatorId(1L);
        tc1.setId(1L);
        tc1.setName("tc1");
        TestCase tc2 = new TestCase();
        tc2.setId(2L);
        tc2.setName("tc2");
        tc2.setCreatorId(1L);
        when(testCaseMapper.selectPage(any(), any()))
                .thenAnswer(
                        invocation -> {
                            Page<TestCase> p = invocation.getArgument(0);
                            p.setRecords(List.of(tc1, tc2));
                            p.setTotal(12);
                            return p;
                        });

        // Act
        Result<PageResult<TestCase>> page = testCaseService.page(1, 2, "", 1L);

        // Assert
        assertEquals(200, page.getCode());
        assertEquals(1, page.getData().getPage());
        assertEquals(2, page.getData().getSize());
        assertEquals(12, page.getData().getTotal());
        assertEquals(2, page.getData().getRecords().size());
    }

    @Test
    @DisplayName("[TC-CRUD-12] page should clamp size to 100 when size exceeds limit")
    @SuppressWarnings("unchecked")
    void page_shouldClampSizeToUpperBound() {
        // Arrange
        when(testCaseMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Act
        testCaseService.page(1, 1000, null, null);

        // Assert
        ArgumentCaptor<Page<TestCase>> captor = ArgumentCaptor.forClass(Page.class);
        verify(testCaseMapper).selectPage(captor.capture(), any());
        assertEquals(100L, captor.getValue().getSize());
    }

    @Test
    @DisplayName("[TC-CRUD-13] page should clamp page to 1 when page is zero")
    @SuppressWarnings("unchecked")
    void page_shouldClampPageToLowerBound() {
        // Arrange
        when(testCaseMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        testCaseService.page(0, 10, null, null);

        // Assert
        ArgumentCaptor<Page<TestCase>> captor = ArgumentCaptor.forClass(Page.class);
        verify(testCaseMapper).selectPage(captor.capture(), any());
        assertEquals(1L, captor.getValue().getCurrent());
    }

    @Test
    @DisplayName("[TC-CRUD-14] page should clamp size to 1 when size is zero")
    @SuppressWarnings("unchecked")
    void page_shouldClampSizeToLowerBound() {
        // Arrange
        when(testCaseMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        testCaseService.page(10, 0, null, null);

        // Assert
        ArgumentCaptor<Page<TestCase>> captor = ArgumentCaptor.forClass(Page.class);
        verify(testCaseMapper).selectPage(captor.capture(), any());
        assertEquals(1L, captor.getValue().getSize());
    }

    @Test
    @DisplayName("[TC-CRUD-15] page should always filter by current user creator_id")
    @SuppressWarnings("unchecked")
    void page_shouldAlwaysFilterByCreatorId() {
        // Arrange
        when(testCaseMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        testCaseService.page(1, 10, null, null);

        // Assert
        ArgumentCaptor<QueryWrapper<TestCase>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(testCaseMapper).selectPage(any(), captor.capture());
        QueryWrapper<TestCase> qw = captor.getValue();
        assertEquals("(creator_id = ?) ORDER BY id DESC", qw.getTargetSql());
    }

    @Test
    @DisplayName("[TC-CRUD-16] page should apply keyword filter on test_no and name")
    @SuppressWarnings("unchecked")
    void page_shouldApplyKeywordFilter() {
        // Arrange
        when(testCaseMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        testCaseService.page(1, 10, "login", null);

        // Assert
        ArgumentCaptor<QueryWrapper<TestCase>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(testCaseMapper).selectPage(any(), captor.capture());
        QueryWrapper<TestCase> qw = captor.getValue();
        assertEquals(
                "(creator_id = ? AND (test_no LIKE ? OR name LIKE ?)) ORDER BY id DESC",
                qw.getTargetSql());
        assertTrue(qw.getParamNameValuePairs().containsValue("%login%"));
    }

    @Test
    @DisplayName("[TC-CRUD-17] page should apply category filter when categoryId provided")
    @SuppressWarnings("unchecked")
    void page_shouldApplyCategoryFilter() {
        // Arrange
        when(testCaseMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        testCaseService.page(1, 10, null, 5L);

        // Assert
        ArgumentCaptor<QueryWrapper<TestCase>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(testCaseMapper).selectPage(any(), captor.capture());
        QueryWrapper<TestCase> qw = captor.getValue();
        assertEquals("(creator_id = ? AND category_id = ?) ORDER BY id DESC", qw.getTargetSql());
        assertTrue(qw.getParamNameValuePairs().containsValue(5L));
    }

    @Test
    @DisplayName("[TC-CRUD-18] page should skip keyword filter when keyword is blank")
    @SuppressWarnings("unchecked")
    void page_shouldSkipKeywordFilterWhenBlank() {
        // Arrange
        when(testCaseMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        testCaseService.page(1, 10, "     ", null);

        // Assert
        ArgumentCaptor<QueryWrapper<TestCase>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(testCaseMapper).selectPage(any(), captor.capture());
        QueryWrapper<TestCase> qw = captor.getValue();
        assertEquals("(creator_id = ?) ORDER BY id DESC", qw.getTargetSql());
        assertTrue(qw.getParamNameValuePairs().containsValue(1L));
    }
}
