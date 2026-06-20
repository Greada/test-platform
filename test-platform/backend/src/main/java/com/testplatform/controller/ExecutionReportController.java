package com.testplatform.controller;

import com.testplatform.common.ErrorPatternResult;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.service.ErrorPatternService;
import com.testplatform.service.ExecutionReportService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/execution-reports")
public class ExecutionReportController {
    private final ExecutionReportService executionReportService;
    private final ErrorPatternService errorPatternService;

    public ExecutionReportController(
            ExecutionReportService executionReportService,
            ErrorPatternService errorPatternService) {
        this.executionReportService = executionReportService;
        this.errorPatternService = errorPatternService;
    }

    @GetMapping
    public Result<List<ExecutionReport>> list(@RequestParam(required = false) Long suiteId) {
        if (suiteId != null) {
            return executionReportService.listBySuiteId(suiteId);
        }
        return executionReportService.listAll();
    }

    @GetMapping("/{id}")
    public Result<ExecutionReport> getById(@PathVariable Long id) {
        return executionReportService.getById(id);
    }

    @GetMapping("/{id}/details")
    public Result<List<ExecutionRecord>> getDetails(@PathVariable Long id) {
        return executionReportService.getReportDetails(id);
    }

    @GetMapping("/{id}/error-patterns")
    public Result<ErrorPatternResult> errorPatterns(@PathVariable Long id) {
        return errorPatternService.analyze(id);
    }
}
