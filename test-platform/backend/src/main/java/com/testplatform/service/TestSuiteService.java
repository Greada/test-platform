package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.TestSuite;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
public interface TestSuiteService {
    Result<List<TestSuite>> listAll();

    Result<TestSuite> getById(Long id);

    Result<Void> save(TestSuite testSuite);

    Result<Void> update(TestSuite testSuite);

    Result<Void> deleteById(Long id);

    // testsuite-testcases
    Result<List<TestCase>> listCases(Long suiteId);

    Result<Void> addCase(Long suiteId, Long caseId);

    Result<Void> removeCase(Long suiteId, Long caseId);

    // batch execution
    Result<ExecutionReport> executeSuite(Long suiteId);

    // batch add
    Result<Void> batchAddCases(Long suiteId, List<Long> caseIds);
}
