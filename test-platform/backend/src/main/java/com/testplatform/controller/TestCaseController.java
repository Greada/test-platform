package com.testplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.common.EndpointDef;
import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;
import com.testplatform.service.AiService;
import com.testplatform.service.TestCaseService;
import com.testplatform.util.OpenApiParser;
import com.testplatform.util.SchemaToJsonGenerator;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

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
    public Result<List<TestCase>> importOpenapi(@RequestBody Map<String, Object> req) {
        try {
            String openapiJson = (String) req.get("openapi");
            boolean useAi = req.containsKey("useAi") && Boolean.TRUE.equals(req.get("useAi"));

            List<EndpointDef> endpoints = OpenApiParser.parse(openapiJson);
            List<TestCase> cases = new ArrayList<>();

            List<String> schemas =
                    endpoints.stream()
                            .map(EndpointDef::getResponseSchema)
                            .collect(Collectors.toList());

            List<String> expectedResults;
            if (useAi) {
                expectedResults = aiService.generateExpectedBatch(schemas);
            } else {
                expectedResults = SchemaToJsonGenerator.batchGenerate(schemas);
            }

            int seq = 1;
            for (int i = 0; i < endpoints.size(); i++) {
                EndpointDef def = endpoints.get(i);
                TestCase tc = new TestCase();
                tc.setTestNo("AUTO-" + String.format("%03d", seq++));
                tc.setName(def.getName());
                tc.setRequestUrl(def.getRequestUrl());
                tc.setRequestMethod(def.getRequestMethod());
                tc.setRequestHeaders(def.getRequestHeaders());
                tc.setRequestParams(def.getRequestParams());
                tc.setExpectedResult(expectedResults.get(i));
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
