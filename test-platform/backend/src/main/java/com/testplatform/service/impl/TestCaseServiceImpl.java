package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.service.TestCaseService;
import com.testplatform.util.SecurityUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class TestCaseServiceImpl implements TestCaseService {
    private final TestCaseMapper testCaseMapper;

    public TestCaseServiceImpl(TestCaseMapper testCaseMapper) {
        this.testCaseMapper = testCaseMapper;
    }

    @Override
    public Result<List<TestCase>> listAll() {
        QueryWrapper<TestCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("creator_id", SecurityUtils.getCurrentUserId());
        List<TestCase> testCaseList = testCaseMapper.selectList(queryWrapper);
        return Result.success(testCaseList);
    }

    @Override
    public Result<List<TestCase>> listByCategoryId(Long categoryId) {
        QueryWrapper<TestCase> qw = new QueryWrapper<>();
        qw.eq("category_id", categoryId).eq("creator_id", SecurityUtils.getCurrentUserId());
        List<TestCase> testCaseList = testCaseMapper.selectList(qw);
        return Result.success(testCaseList);
    }

    @Override
    public Result<TestCase> getById(Long id) {
        TestCase testCase = testCaseMapper.selectById(id);
        if (testCase == null) {
            return Result.error(404, "用例不存在");
        }
        if (!Objects.equals(SecurityUtils.getCurrentUserId(), testCase.getCreatorId())) {
            return Result.error(404, "用例不存在");
        }
        return Result.success(testCase);
    }

    @Override
    public Result<Void> save(TestCase testCase) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error(401, "未登录");
        }

        testCase.setCreatorId(currentUserId);
        testCaseMapper.insert(testCase);
        return Result.success(null);
    }

    @Override
    public Result<Void> update(TestCase testCase) {
        TestCase existing = testCaseMapper.selectById(testCase.getId());
        if (existing == null) {
            return Result.error(404, "用例不存在");
        }
        if (!Objects.equals(SecurityUtils.getCurrentUserId(), existing.getCreatorId())) {
            return Result.error(404, "用例不存在");
        }
        // 不能修改用例创建者
        testCase.setCreatorId(null);
        testCaseMapper.updateById(testCase);
        return Result.success(null);
    }

    @Override
    public Result<Void> deleteById(Long id) {
        TestCase existing = testCaseMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "用例不存在");
        }
        if (!Objects.equals(SecurityUtils.getCurrentUserId(), existing.getCreatorId())) {
            return Result.error(404, "用例不存在");
        }
        testCaseMapper.deleteById(id);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<Void> batchSave(List<TestCase> cases) {
        for (TestCase tc : cases) {
            tc.setCreatorId(SecurityUtils.getCurrentUserId());
            testCaseMapper.insert(tc);
        }
        return Result.success(null);
    }
}
