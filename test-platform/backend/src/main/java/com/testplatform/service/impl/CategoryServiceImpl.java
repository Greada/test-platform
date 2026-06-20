package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.dto.CategoryNode;
import com.testplatform.entity.TestCategory;
import com.testplatform.mapper.TestCategoryMapper;
import com.testplatform.service.CategoryService;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final TestCategoryMapper testCategoryMapper;

    public CategoryServiceImpl(TestCategoryMapper testCategoryMapper) {
        this.testCategoryMapper = testCategoryMapper;
    }

    @Override
    public Result<List<CategoryNode>> listTree() {
        List<TestCategory> all = testCategoryMapper.selectList(new QueryWrapper<>());

        Map<Long, CategoryNode> nodeMap = new HashMap<>();
        ArrayList<CategoryNode> roots = new ArrayList<>();

        for (TestCategory testCategory : all) {
            CategoryNode node = new CategoryNode();
            BeanUtils.copyProperties(testCategory, node);
            node.setChildren(new ArrayList<>());
            nodeMap.put(testCategory.getId(), node);
        }

        for (TestCategory testCategory : all) {
            CategoryNode node = nodeMap.get(testCategory.getId());
            if (testCategory.getParentId() == 0) {
                roots.add(node);
            } else {
                CategoryNode parent = nodeMap.get(testCategory.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        return Result.success(roots);
    }

    @Override
    public Result<List<TestCategory>> listAll() {
        List<TestCategory> list = testCategoryMapper.selectList(new QueryWrapper<>());
        return Result.success(list);
    }

    @Override
    public Result<Void> save(TestCategory category) {
        if (category.getLevel() == null || category.getLevel() > 3) {
            return Result.badRequest("level must not exceed 3");
        }

        if (category.getParentId() != null && category.getParentId() != 0) {
            TestCategory parent = testCategoryMapper.selectById(category.getParentId());
            if (parent == null) {
                return Result.notFound("parent not found");
            }
            if (parent.getLevel() >= 3) {
                return Result.badRequest("already the highest level");
            }
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setLevel(1);
        }

        QueryWrapper<TestCategory> qw = new QueryWrapper<>();
        qw.eq("parent_id", category.getParentId()).eq("name", category.getName());
        if (testCategoryMapper.selectCount(qw) > 0) {
            return Result.conflict(
                    "Classification names under the same parent level cannot be duplicated");
        }

        testCategoryMapper.insert(category);
        return Result.success(null);
    }

    @Override
    public Result<Void> update(TestCategory category) {
        QueryWrapper<TestCategory> qw = new QueryWrapper<>();
        qw.eq("parent_id", category.getParentId())
                .eq("name", category.getName())
                .ne("id", category.getId());
        if (testCategoryMapper.selectCount(qw) > 0) {
            return Result.conflict(
                    "Classification names under the same parent level cannot be duplicated");
        }

        testCategoryMapper.updateById(category);
        return Result.success(null);
    }

    @Override
    public Result<Void> deleteById(Long id) {
        Long childCount =
                testCategoryMapper.selectCount(
                        new QueryWrapper<TestCategory>().eq("parent_id", id));
        if (childCount > 0) {
            return Result.conflict(
                    "This category includes: " + childCount + " and cannot be deleted");
        }

        testCategoryMapper.deleteById(id);
        return Result.success(null);
    }
}
