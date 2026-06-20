package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.service.TestCaseService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestCaseServiceImpl implements TestCaseService {
    private final TestCaseMapper testCaseMapper;

    public TestCaseServiceImpl(TestCaseMapper testCaseMapper) {
        this.testCaseMapper = testCaseMapper;
    }

    @Override
    public Result<List<TestCase>> listAll() {
        List<TestCase> testCaseList = testCaseMapper.selectList(new QueryWrapper<>());
        return Result.success(testCaseList);
    }

    @Override
    public Result<List<TestCase>> listByCategoryId(Long categoryId) {
        QueryWrapper<TestCase> qw = new QueryWrapper<>();
        qw.eq("category_id", categoryId);
        List<TestCase> testCaseList = testCaseMapper.selectList(qw);
        return Result.success(testCaseList);
    }

    @Override
    public Result<TestCase> getById(Long id) {
        TestCase testCase = testCaseMapper.selectById(id);
        return Result.success(testCase);
    }

    @Override
    public Result<Void> save(TestCase testCase) {
        testCaseMapper.insert(testCase);
        return Result.success(null);
    }

    @Override
    public Result<Void> update(TestCase testCase) {
        testCaseMapper.updateById(testCase);
        return Result.success(null);
    }

    @Override
    public Result<Void> deleteById(Long id) {
        testCaseMapper.deleteById(id);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<Void> batchSave(List<TestCase> cases) {
        for (TestCase tc : cases) {
            testCaseMapper.insert(tc);
        }
        return Result.success(null);
    }
}
