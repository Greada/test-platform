package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.TestCase;

import java.util.List;

public interface TestCaseService {
    Result<List<TestCase>> listAll();

    Result<List<TestCase>> listByCategoryId(Long categoryId);

    Result<TestCase> getById(Long id);

    Result<Void> save(TestCase testCase);

    Result<Void> update(TestCase testCase);

    Result<Void> deleteById(Long id);

    Result<Void> batchSave(List<TestCase> cases);
}
