package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.dto.CategoryNode;
import com.testplatform.entity.TestCategory;
import com.testplatform.mapper.TestCategoryMapper;
import com.testplatform.service.CategoryService;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class CategoryServiceImpl implements CategoryService {
    private final TestCategoryMapper testCategoryMapper;

    @Autowired
    public CategoryServiceImpl(TestCategoryMapper testCategoryMapper) {
        this.testCategoryMapper = testCategoryMapper;
    }

    @Override
    public Result<List<CategoryNode>> listTree() {
        try {
            // 1.query all category
            List<TestCategory> all = testCategoryMapper.selectList(new QueryWrapper<>());

            // 2.build node mapping
            Map<Long, CategoryNode> nodeMap = new HashMap<>();
            ArrayList<CategoryNode> roots = new ArrayList<>();

            for (TestCategory testCategory : all) {
                CategoryNode node = new CategoryNode();
                BeanUtils.copyProperties(testCategory, node);
                node.setChildren(new ArrayList<>());
                nodeMap.put(testCategory.getId(), node);
            }

            // 3.build a tree structure
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
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<List<TestCategory>> listAll() {
        try {
            List<TestCategory> list = testCategoryMapper.selectList(new QueryWrapper<>());
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> save(TestCategory category) {
        try {
            // check level
            if (category.getLevel() == null || category.getLevel() > 3) {
                return Result.error("level must not exceed 3");
            }

            // check parent
            if (category.getParentId() != null && category.getParentId() != 0) {
                TestCategory parent = testCategoryMapper.selectById(category.getParentId());
                if (parent == null) {
                    return Result.error("parent not found");
                }
                if (parent.getLevel() >= 3) {
                    return Result.error("already the highest level");
                }
                category.setLevel(parent.getLevel() + 1);
            } else {
                category.setLevel(1);
            }

            // check unique under the same parental name
            QueryWrapper<TestCategory> qw = new QueryWrapper<>();
            qw.eq("parent_id", category.getParentId()).eq("name", category.getName());
            if (testCategoryMapper.selectCount(qw) > 0) {
                return Result.error(
                        "Classification names under the same parent level cannot be duplicated");
            }

            testCategoryMapper.insert(category);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> update(TestCategory category) {
        try {
            // check unique under the same parental name
            QueryWrapper<TestCategory> qw = new QueryWrapper<>();
            qw.eq("parent_id", category.getParentId())
                    .eq("name", category.getName())
                    .ne("id", category.getId());
            if (testCategoryMapper.selectCount(qw) > 0) {
                return Result.error(
                        "Classification names under the same parent level cannot be duplicated");
            }

            testCategoryMapper.updateById(category);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteById(Long id) {
        try {
            // check for subcategories
            Long childCount =
                    testCategoryMapper.selectCount(
                            new QueryWrapper<TestCategory>().eq("parent_id", id));
            if (childCount > 0) {
                return Result.error(
                        "This category includes: " + childCount + "and cannot be deleted");
            }

            testCategoryMapper.deleteById(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
