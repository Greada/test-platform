package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.service.TestCaseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {
    private final TestCaseService testCaseService;

    @Autowired
    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @GetMapping
    public Result<List<TestCase>> listAll() {
        return testCaseService.listAll();
    }

    @GetMapping("/{id}")
    public Result<TestCase> getById(@PathVariable Long id) {
        return testCaseService.getById(id);
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody TestCase testCase) {
        return testCaseService.save(testCase);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TestCase testCase) {
        testCase.setId(id);
        return testCaseService.update(testCase);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return testCaseService.deleteById(id);
    }
}
