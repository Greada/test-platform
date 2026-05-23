package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
public interface ExecutionService {
    Result<Void> execute(Long testCaseId);

    Result<List<ExecutionRecord>> listByTestCaseId(Long testCaseId);
}
