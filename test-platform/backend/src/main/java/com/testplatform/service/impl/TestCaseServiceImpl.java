package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.service.TestCaseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class TestCaseServiceImpl implements TestCaseService {
    private final TestCaseMapper testCaseMapper;

    @Autowired
    public TestCaseServiceImpl(TestCaseMapper testCaseMapper) {
        this.testCaseMapper = testCaseMapper;
    }

    @Override
    public Result<List<TestCase>> listAll() {
        try {
            List<TestCase> testCaseList = testCaseMapper.selectList(new QueryWrapper<>());
            return Result.success(testCaseList);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<TestCase> getById(Long id) {
        try {
            TestCase testCase = testCaseMapper.selectById(id);
            return Result.success(testCase);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> save(TestCase testCase) {
        try {
            testCaseMapper.insert(testCase);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> update(TestCase testCase) {
        try {
            testCaseMapper.updateById(testCase);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteById(Long id) {
        try {
            testCaseMapper.deleteById(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
