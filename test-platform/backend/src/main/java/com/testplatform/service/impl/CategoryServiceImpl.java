package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.dto.CategoryNode;
import com.testplatform.entity.TestCategory;
import com.testplatform.mapper.TestCategoryMapper;
import com.testplatform.service.CategoryService;
import com.testplatform.util.SecurityUtils;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final TestCategoryMapper testCategoryMapper;

    public CategoryServiceImpl(TestCategoryMapper testCategoryMapper) {
        this.testCategoryMapper = testCategoryMapper;
    }

    @Override
    public Result<List<CategoryNode>> listTree() {
        QueryWrapper<TestCategory> listQw = new QueryWrapper<>();
        listQw.eq("creator_id", SecurityUtils.getCurrentUserId());
        List<TestCategory> all = testCategoryMapper.selectList(listQw);

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
        QueryWrapper<TestCategory> qw = new QueryWrapper<>();
        qw.eq("creator_id", SecurityUtils.getCurrentUserId());
        List<TestCategory> list = testCategoryMapper.selectList(qw);
        return Result.success(list);
    }

    @Override
    public Result<Void> save(TestCategory category) {
        if (category.getLevel() == null || category.getLevel() > 3) {
            return Result.badRequest("level must not exceed 3");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        category.setCreatorId(currentUserId);

        if (category.getParentId() != null && category.getParentId() != 0) {
            TestCategory parent = testCategoryMapper.selectById(category.getParentId());
            if (parent == null) {
                return Result.notFound("parent not found");
            }
            if (!Objects.equals(parent.getCreatorId(), currentUserId)) {
                return Result.error(404, "父分类不存在");
            }
            if (parent.getLevel() >= 3) {
                return Result.badRequest("already the highest level");
            }
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setLevel(1);
        }

        QueryWrapper<TestCategory> qw = new QueryWrapper<>();
        qw.eq("parent_id", category.getParentId())
                .eq("name", category.getName())
                .eq("creator_id", currentUserId);
        if (testCategoryMapper.selectCount(qw) > 0) {
            return Result.conflict(
                    "Classification names under the same parent level cannot be duplicated");
        }

        testCategoryMapper.insert(category);
        return Result.success(null);
    }

    @Override
    public Result<Void> update(TestCategory category) {
        TestCategory existing = testCategoryMapper.selectById(category.getId());
        if (existing == null) {
            return Result.error(404, "分类不存在");
        }
        if (!Objects.equals(existing.getCreatorId(), SecurityUtils.getCurrentUserId())) {
            return Result.error(404, "分类不存在");
        }

        QueryWrapper<TestCategory> qw = new QueryWrapper<>();
        qw.eq("parent_id", category.getParentId())
                .eq("name", category.getName())
                .ne("id", category.getId())
                .eq("creator_id", SecurityUtils.getCurrentUserId());
        if (testCategoryMapper.selectCount(qw) > 0) {
            return Result.conflict(
                    "Classification names under the same parent level cannot be duplicated");
        }

        category.setCreatorId(null);
        testCategoryMapper.updateById(category);
        return Result.success(null);
    }

    @Override
    public Result<Void> deleteById(Long id) {
        TestCategory existing = testCategoryMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "分类不存在");
        }
        if (!Objects.equals(existing.getCreatorId(), SecurityUtils.getCurrentUserId())) {
            return Result.error(404, "分类不存在");
        }

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
