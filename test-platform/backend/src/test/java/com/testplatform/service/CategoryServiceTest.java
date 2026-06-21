package com.testplatform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.testplatform.common.Result;
import com.testplatform.dto.CategoryNode;
import com.testplatform.entity.TestCategory;
import com.testplatform.mapper.TestCategoryMapper;
import com.testplatform.service.impl.CategoryServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** CategoryService unit tests */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private TestCategoryMapper testCategoryMapper;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(testCategoryMapper);
    }

    @Test
    @DisplayName("[CAT-TREE-01] listTree should return full tree structure")
    void listTree_shouldReturnFullTree() {
        // Arrange
        TestCategory root = new TestCategory();
        root.setId(1L);
        root.setName("Root");
        root.setParentId(0L);
        root.setLevel(1);
        TestCategory child = new TestCategory();
        child.setId(2L);
        child.setName("Child");
        child.setParentId(1L);
        child.setLevel(2);
        when(testCategoryMapper.selectList(any())).thenReturn(Arrays.asList(root, child));

        // Act
        Result<List<CategoryNode>> result = categoryService.listTree();

        // Assert
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("Root", result.getData().get(0).getName());
        assertEquals(1, result.getData().get(0).getChildren().size());
        assertEquals("Child", result.getData().get(0).getChildren().get(0).getName());
    }

    @Test
    @DisplayName("[CAT-TREE-02] empty database should return empty list")
    void listTree_emptyShouldReturnEmpty() {
        // Arrange
        when(testCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        Result<List<CategoryNode>> result = categoryService.listTree();

        // Assert
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("[CAT-TREE-03] three level tree should build correctly")
    void listTree_threeLevels_shouldBuildCorrectly() {
        // Arrange
        TestCategory l1 = new TestCategory();
        l1.setId(1L);
        l1.setName("L1");
        l1.setParentId(0L);
        l1.setLevel(1);
        TestCategory l2 = new TestCategory();
        l2.setId(2L);
        l2.setName("L2");
        l2.setParentId(1L);
        l2.setLevel(2);
        TestCategory l3 = new TestCategory();
        l3.setId(3L);
        l3.setName("L3");
        l3.setParentId(2L);
        l3.setLevel(3);
        when(testCategoryMapper.selectList(any())).thenReturn(Arrays.asList(l1, l2, l3));

        // Act
        Result<List<CategoryNode>> result = categoryService.listTree();

        // Assert
        assertEquals(1, result.getData().size());
        assertEquals("L2", result.getData().get(0).getChildren().get(0).getName());
        assertEquals(
                "L3", result.getData().get(0).getChildren().get(0).getChildren().get(0).getName());
    }

    @Test
    @DisplayName("[CAT-LIST-01] listAll should return flat list")
    void listAll_shouldReturnFlatList() {
        // Arrange
        TestCategory cat = new TestCategory();
        cat.setId(1L);
        cat.setName("Category");
        when(testCategoryMapper.selectList(any())).thenReturn(Collections.singletonList(cat));

        // Act
        Result<List<TestCategory>> result = categoryService.listAll();

        // Assert
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("[CAT-SAVE-01] save root node should set level=1")
    void save_rootNode_shouldSetLevelOne() {
        // Arrange - source checks getLevel() first, so must set it
        TestCategory cat = new TestCategory();
        cat.setName("New Root");
        cat.setParentId(0L);
        cat.setLevel(1);
        when(testCategoryMapper.selectCount(any())).thenReturn(0L);
        when(testCategoryMapper.insert(any(TestCategory.class))).thenReturn(1);

        // Act
        Result<Void> result = categoryService.save(cat);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
    }

    @Test
    @DisplayName("[CAT-SAVE-02] save child node should auto set level from parent")
    void save_childNode_shouldAutoSetParentAndLevel() {
        // Arrange
        TestCategory parent = new TestCategory();
        parent.setId(1L);
        parent.setLevel(1);
        when(testCategoryMapper.selectById(1L)).thenReturn(parent);
        when(testCategoryMapper.selectCount(any())).thenReturn(0L);
        when(testCategoryMapper.insert(any(TestCategory.class))).thenReturn(1);

        TestCategory cat = new TestCategory();
        cat.setName("Child");
        cat.setParentId(1L);
        cat.setLevel(2);

        // Act
        Result<Void> result = categoryService.save(cat);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
    }

    @Test
    @DisplayName("[CAT-SAVE-03] save third level child should succeed")
    void save_thirdLevel_shouldSucceed() {
        // Arrange
        TestCategory parent = new TestCategory();
        parent.setLevel(2);
        when(testCategoryMapper.selectById(1L)).thenReturn(parent);
        when(testCategoryMapper.selectCount(any())).thenReturn(0L);
        when(testCategoryMapper.insert(any(TestCategory.class))).thenReturn(1);

        TestCategory cat = new TestCategory();
        cat.setParentId(1L);
        cat.setLevel(3);

        // Act
        Result<Void> result = categoryService.save(cat);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
    }

    @Test
    @DisplayName("[CAT-SAVE-04] null level should fail")
    void save_nullLevel_shouldFail() {
        // Arrange
        TestCategory cat = new TestCategory();
        cat.setName("Test");

        // Act
        Result<Void> result = categoryService.save(cat);

        // Assert
        assertEquals(400, result.getCode());
        assertEquals("level must not exceed 3", result.getMessage());
    }

    @Test
    @DisplayName("[CAT-SAVE-05] level > 3 should fail")
    void save_levelOverThree_shouldFail() {
        // Arrange
        TestCategory cat = new TestCategory();
        cat.setName("Test");
        cat.setLevel(4);

        // Act
        Result<Void> result = categoryService.save(cat);

        // Assert
        assertEquals(400, result.getCode());
        assertEquals("level must not exceed 3", result.getMessage());
    }

    @Test
    @DisplayName("[CAT-SAVE-06] duplicate name under same parent should fail")
    void save_duplicateName_shouldFail() {
        // Arrange - need selectById to return valid parent so code reaches selectCount
        TestCategory parent = new TestCategory();
        parent.setId(1L);
        parent.setLevel(1);
        when(testCategoryMapper.selectById(1L)).thenReturn(parent);
        when(testCategoryMapper.selectCount(any())).thenReturn(1L);

        TestCategory cat = new TestCategory();
        cat.setName("Duplicate");
        cat.setParentId(1L);
        cat.setLevel(2);

        // Act
        Result<Void> result = categoryService.save(cat);

        // Assert
        assertEquals(409, result.getCode());
    }

    @Test
    @DisplayName("[CAT-SAVE-07] parent not found should fail")
    void save_parentNotFound_shouldFail() {
        // Arrange
        when(testCategoryMapper.selectById(1L)).thenReturn(null);

        TestCategory cat = new TestCategory();
        cat.setName("Child");
        cat.setParentId(1L);
        cat.setLevel(2);

        // Act
        Result<Void> result = categoryService.save(cat);

        // Assert
        assertEquals(404, result.getCode());
        assertEquals("parent not found", result.getMessage());
    }

    @Test
    @DisplayName("[CAT-SAVE-07] parent level >= 3 should fail")
    void save_parentLevelThree_shouldFail() {
        // Arrange
        TestCategory parent = new TestCategory();
        parent.setLevel(3);
        when(testCategoryMapper.selectById(1L)).thenReturn(parent);

        TestCategory cat = new TestCategory();
        cat.setParentId(1L);
        cat.setLevel(2);

        // Act
        Result<Void> result = categoryService.save(cat);

        // Assert
        assertEquals(400, result.getCode());
        assertEquals("already the highest level", result.getMessage());
    }

    @Test
    @DisplayName("[CAT-UPDATE-01] update name should succeed")
    void updateName_shouldSucceed() {
        // Arrange
        TestCategory cat = new TestCategory();
        cat.setId(1L);
        cat.setName("Updated");
        cat.setParentId(0L);
        cat.setLevel(1);
        when(testCategoryMapper.selectCount(any())).thenReturn(0L);
        when(testCategoryMapper.updateById(any(TestCategory.class))).thenReturn(1);

        // Act
        Result<Void> result = categoryService.update(cat);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
    }

    @Test
    @DisplayName("[CAT-UPDATE-02] duplicate name on update should fail")
    void update_duplicateName_shouldFail() {
        // Arrange
        TestCategory cat = new TestCategory();
        cat.setId(1L);
        cat.setName("Dup");
        cat.setParentId(0L);
        when(testCategoryMapper.selectCount(any())).thenReturn(1L);

        // Act
        Result<Void> result = categoryService.update(cat);

        // Assert
        assertEquals(409, result.getCode());
    }

    @Test
    @DisplayName("[CAT-SAVE-10] delete has children should fail")
    void delete_hasChildren_shouldFail() {
        // Arrange
        when(testCategoryMapper.selectCount(any())).thenReturn(2L);

        // Act
        Result<Void> result = categoryService.deleteById(1L);

        // Assert
        assertEquals(409, result.getCode());
    }

    @Test
    @DisplayName("[CAT-DEL-02] delete without children should succeed")
    void delete_noChildren_shouldSucceed() {
        // Arrange
        when(testCategoryMapper.selectCount(any())).thenReturn(0L);
        when(testCategoryMapper.deleteById(1L)).thenReturn(1);

        // Act
        Result<Void> result = categoryService.deleteById(1L);

        // Assert
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
    }
}
