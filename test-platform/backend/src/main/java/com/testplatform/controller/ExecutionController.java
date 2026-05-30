package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.service.ExecutionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/execution-records")
public class ExecutionController {
    private final ExecutionService executionService;

    @Autowired
    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/{testCaseId}/execute")
    public Result<ExecutionRecord> execute(@PathVariable Long testCaseId) {
        return executionService.execute(testCaseId);
    }

    @GetMapping
    public Result<List<ExecutionRecord>> listByTestCaseId(@RequestParam Long testCaseId) {
        return executionService.listByTestCaseId(testCaseId);
    }
}
