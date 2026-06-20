package com.testplatform.controller;

import com.testplatform.common.JsonDiffResult;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.service.ExecutionService;
import com.testplatform.service.JsonDiffService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/execution-records")
public class ExecutionController {
    private final ExecutionService executionService;
    private final JsonDiffService jsonDiffService;

    public ExecutionController(ExecutionService executionService, JsonDiffService jsonDiffService) {
        this.executionService = executionService;
        this.jsonDiffService = jsonDiffService;
    }

    @PostMapping("/{testCaseId}/execute")
    public Result<ExecutionRecord> execute(@PathVariable Long testCaseId) {
        return executionService.execute(testCaseId);
    }

    @GetMapping
    public Result<List<ExecutionRecord>> listByTestCaseId(@RequestParam Long testCaseId) {
        return executionService.listByTestCaseId(testCaseId);
    }

    @GetMapping("/{id}/diff")
    public Result<JsonDiffResult> diff(@PathVariable Long id) {
        return jsonDiffService.diff(id);
    }
}
