package com.testplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.common.EndpointDef;
import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.service.AiService;
import com.testplatform.service.TestCaseService;
import com.testplatform.util.OpenApiParser;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/testcases")
public class TestCaseController {
    private final TestCaseService testCaseService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public TestCaseController(
            TestCaseService testCaseService, AiService aiService, ObjectMapper objectMapper) {
        this.testCaseService = testCaseService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Result<List<TestCase>> listAll(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return testCaseService.listByCategoryId(categoryId);
        }
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

    @PostMapping("/import-openapi")
    public Result<List<TestCase>> importOpenapi(@RequestBody Map<String, String> req) {
        try {
            String openapiJson = req.get("openapi");
            List<EndpointDef> endpoints = OpenApiParser.parse(openapiJson);
            List<TestCase> cases = new ArrayList<>();
            int seq = 1;
            for (EndpointDef def : endpoints) {
                TestCase tc = new TestCase();
                tc.setTestNo("AUTO-" + String.format("%03d", seq++));
                tc.setName(def.getName());
                tc.setRequestUrl(def.getRequestUrl());
                tc.setRequestMethod(def.getRequestMethod());
                tc.setRequestHeaders(def.getRequestHeaders());
                tc.setRequestParams(def.getRequestParams());
                String expected = aiService.generateExpectedFromSchema(def.getResponseSchema());
                tc.setExpectedResult(expected);
                cases.add(tc);
            }
            return Result.success(cases);
        } catch (Exception e) {
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-save")
    public Result<Void> batchSave(@RequestBody List<TestCase> cases) {
        return testCaseService.batchSave(cases);
    }
}
