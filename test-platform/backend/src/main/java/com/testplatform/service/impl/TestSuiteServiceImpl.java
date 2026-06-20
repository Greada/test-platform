package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.entity.*;
import com.testplatform.mapper.ExecutionReportMapper;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.mapper.TestSuiteCaseMapper;
import com.testplatform.mapper.TestSuiteMapper;
import com.testplatform.service.ExecutionService;
import com.testplatform.service.TestSuiteService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TestSuiteServiceImpl implements TestSuiteService {
    private final TestSuiteMapper testSuiteMapper;
    private final TestSuiteCaseMapper testSuiteCaseMapper;
    private final TestCaseMapper testCaseMapper;
    private final ExecutionReportMapper executionReportMapper;
    private final ExecutionService executionService;

    public TestSuiteServiceImpl(
            TestSuiteMapper testSuiteMapper,
            TestSuiteCaseMapper testSuiteCaseMapper,
            TestCaseMapper testCaseMapper,
            ExecutionReportMapper executionReportMapper,
            ExecutionService executionService) {
        this.testSuiteMapper = testSuiteMapper;
        this.testSuiteCaseMapper = testSuiteCaseMapper;
        this.testCaseMapper = testCaseMapper;
        this.executionReportMapper = executionReportMapper;
        this.executionService = executionService;
    }

    @Override
    public Result<List<TestSuite>> listAll() {
        List<TestSuite> testSuites = testSuiteMapper.selectList(new QueryWrapper<>());
        return Result.success(testSuites);
    }

    @Override
    public Result<TestSuite> getById(Long id) {
        TestSuite testSuite = testSuiteMapper.selectById(id);
        return Result.success(testSuite);
    }

    @Override
    public Result<Void> save(TestSuite testSuite) {
        testSuiteMapper.insert(testSuite);
        return Result.success(null);
    }

    @Override
    public Result<Void> update(TestSuite testSuite) {
        testSuiteMapper.updateById(testSuite);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<Void> deleteById(Long id) {
        QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
        qw.eq("suite_id", id);
        testSuiteCaseMapper.delete(qw);
        testSuiteMapper.deleteById(id);
        return Result.success(null);
    }

    @Override
    public Result<List<TestCase>> listCases(Long suiteId) {
        QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
        qw.eq("suite_id", suiteId).orderByAsc("sort_order");
        List<TestSuiteCase> testSuiteCases = testSuiteCaseMapper.selectList(qw);
        if (testSuiteCases.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<Long> caseIds =
                testSuiteCases.stream()
                        .map(TestSuiteCase::getCaseId)
                        .collect(Collectors.toList());
        List<TestCase> testCaseList = testCaseMapper.selectBatchIds(caseIds);
        Map<Long, TestCase> caseMap =
                testCaseList.stream().collect(Collectors.toMap(TestCase::getId, c -> c));
        List<TestCase> ordered =
                testSuiteCases.stream()
                        .map(testSuiteCase -> caseMap.get(testSuiteCase.getCaseId()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        return Result.success(ordered);
    }

    @Override
    @Transactional
    public Result<Void> addCase(Long suiteId, Long caseId) {
        if (testSuiteMapper.selectById(suiteId) == null) {
            return Result.notFound("suite not found!");
        }
        if (testCaseMapper.selectById(caseId) == null) {
            return Result.notFound("testCase not found");
        }

        QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
        qw.eq("suite_id", suiteId).eq("case_id", caseId);
        if (testSuiteCaseMapper.selectCount(qw) > 0) {
            return Result.conflict("case already in suite");
        }

        TestSuiteCase testSuiteCase = new TestSuiteCase();
        testSuiteCase.setSuiteId(suiteId);
        testSuiteCase.setCaseId(caseId);
        testSuiteCase.setSortOrder(0);
        testSuiteCaseMapper.insert(testSuiteCase);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<Void> removeCase(Long suiteId, Long caseId) {
        QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
        qw.eq("suite_id", suiteId).eq("case_id", caseId);
        testSuiteCaseMapper.delete(qw);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<ExecutionReport> executeSuite(Long suiteId) {
        TestSuite testSuite = testSuiteMapper.selectById(suiteId);
        if (testSuite == null) {
            return Result.notFound("suite not found");
        }

        QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
        qw.eq("suite_id", suiteId);
        List<TestSuiteCase> links = testSuiteCaseMapper.selectList(qw);
        if (links.isEmpty()) {
            return Result.badRequest("suite has no testcases");
        }

        ExecutionReport report = new ExecutionReport();
        report.setSuiteId(suiteId);
        report.setReportName(
                testSuite.getName()
                        + "-"
                        + LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        report.setTotal(links.size());
        report.setPassed(0);
        report.setFailed(0);
        report.setErrored(0);
        report.setPassRate(0.0);
        report.setStatus("RUNNING");
        report.setExecuteTime(LocalDateTime.now());
        executionReportMapper.insert(report);

        int passed = 0, failed = 0, errored = 0;
        for (TestSuiteCase link : links) {
            Result<ExecutionRecord> executeResult =
                    executionService.execute(link.getCaseId(), report.getId());
            if (executeResult.getCode() == 200 && executeResult.getData() != null) {
                String status = executeResult.getData().getStatus();
                if ("PASS".equals(status)) {
                    passed++;
                } else if ("FAIL".equals(status)) {
                    failed++;
                } else {
                    errored++;
                }
            } else {
                errored++;
            }
        }

        double rate = !links.isEmpty() ? (double) passed / (links.size()) * 100 : 0.0;
        report.setPassed(passed);
        report.setFailed(failed);
        report.setErrored(errored);
        report.setPassRate(Math.round(rate * 100.0) / 100.0);
        report.setStatus("COMPLETED");
        executionReportMapper.updateById(report);
        return Result.success(report);
    }

    @Override
    public Result<Void> batchAddCases(Long suiteId, List<Long> caseIds) {
        if (testSuiteMapper.selectById(suiteId) == null) {
            return Result.notFound("suite not found");
        }
        for (Long caseId : caseIds) {
            if (testCaseMapper.selectById(caseId) == null) {
                continue;
            }
            TestSuiteCase link = new TestSuiteCase();
            link.setCaseId(caseId);
            link.setSuiteId(suiteId);
            link.setSortOrder(0);
            testSuiteCaseMapper.insert(link);
        }
        return Result.success(null);
    }
}
