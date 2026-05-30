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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class TestSuiteServiceImpl implements TestSuiteService {
    private final TestSuiteMapper testSuiteMapper;
    private final TestSuiteCaseMapper testSuiteCaseMapper;
    private final TestCaseMapper testCaseMapper;
    private final ExecutionReportMapper executionReportMapper;
    private final ExecutionService executionService;

    @Autowired
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
        try {
            List<TestSuite> testSuites = testSuiteMapper.selectList(new QueryWrapper<>());
            return Result.success(testSuites);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<TestSuite> getById(Long id) {
        try {
            TestSuite testSuite = testSuiteMapper.selectById(id);
            return Result.success(testSuite);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> save(TestSuite testSuite) {
        try {
            testSuiteMapper.insert(testSuite);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> update(TestSuite testSuite) {
        try {
            testSuiteMapper.updateById(testSuite);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> deleteById(Long id) {
        try {
            QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
            qw.eq("suite_id", id);
            testSuiteCaseMapper.delete(qw);
            testSuiteMapper.deleteById(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<List<TestCase>> listCases(Long suiteId) {
        try {
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
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> addCase(Long suiteId, Long caseId) {
        try {
            if (testSuiteMapper.selectById(suiteId) == null) {
                return Result.error("suite not found!");
            }
            if (testCaseMapper.selectById(caseId) == null) {
                return Result.error("testCase not found");
            }

            QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
            qw.eq("suite_id", suiteId).eq("case_id", caseId);
            if (testSuiteCaseMapper.selectCount(qw) > 0) {
                return Result.error("case already in suite");
            }

            TestSuiteCase testSuiteCase = new TestSuiteCase();
            testSuiteCase.setSuiteId(suiteId);
            testSuiteCase.setCaseId(caseId);
            testSuiteCase.setSortOrder(0);
            testSuiteCaseMapper.insert(testSuiteCase);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> removeCase(Long suiteId, Long caseId) {
        try {
            QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
            qw.eq("suite_id", suiteId).eq("case_id", caseId);
            testSuiteCaseMapper.delete(qw);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<ExecutionReport> executeSuite(Long suiteId) {
        try {
            TestSuite testSuite = testSuiteMapper.selectById(suiteId);
            if (testSuite == null) {
                return Result.error("suite not found");
            }

            QueryWrapper<TestSuiteCase> qw = new QueryWrapper<>();
            qw.eq("suite_id", suiteId);
            List<TestSuiteCase> links = testSuiteCaseMapper.selectList(qw);
            if (links.isEmpty()) {
                return Result.error("suite has no testcases");
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
                    switch (executeResult.getData().getStatus()) {
                        case "PASS":
                            passed++;
                            break;
                        case "FAIL":
                            failed++;
                            break;
                        default:
                            errored++;
                            break;
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
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<Void> batchAddCases(Long suiteId, List<Long> caseIds) {
        try {
            if (testSuiteMapper.selectById(suiteId) == null) {
                return Result.error(500, "suite not found");
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
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
