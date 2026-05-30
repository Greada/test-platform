package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.TestSuite;
import com.testplatform.service.TestSuiteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/test-suites")
public class TestSuiteController {
    private final TestSuiteService testSuiteService;

    @Autowired
    public TestSuiteController(TestSuiteService testSuiteService) {
        this.testSuiteService = testSuiteService;
    }

    @GetMapping
    public Result<List<TestSuite>> listAll() {
        return testSuiteService.listAll();
    }

    @GetMapping("/{id}")
    public Result<TestSuite> getById(@PathVariable Long id) {
        return testSuiteService.getById(id);
    }

    @PostMapping
    public Result<Void> save(@RequestBody TestSuite testSuite) {
        return testSuiteService.save(testSuite);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TestSuite testSuite) {
        testSuite.setId(id);
        return testSuiteService.update(testSuite);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return testSuiteService.deleteById(id);
    }

    @GetMapping("/{id}/cases")
    public Result<List<TestCase>> listCases(@PathVariable Long id) {
        return testSuiteService.listCases(id);
    }

    @PostMapping("/{id}/cases")
    public Result<Void> addCase(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        return testSuiteService.addCase(id, body.get("caseId"));
    }

    @DeleteMapping("/{id}/cases/{caseId}")
    public Result<Void> removeCase(@PathVariable Long id, @PathVariable Long caseId) {
        return testSuiteService.removeCase(id, caseId);
    }

    @PostMapping("/{id}/execute")
    public Result<ExecutionReport> executeSuite(@PathVariable Long id) {
        return testSuiteService.executeSuite(id);
    }

    @PostMapping("/{id}/cases/batch")
    public Result<Void> batchAddCases(
            @PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        return testSuiteService.batchAddCases(id, body.get("caseIds"));
    }
}
