package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.service.ExecutionReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/execution-reports")
public class ExecutionReportController {
    private final ExecutionReportService executionReportService;

    @Autowired
    public ExecutionReportController(ExecutionReportService executionReportService) {
        this.executionReportService = executionReportService;
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
}
