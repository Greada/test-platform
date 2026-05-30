package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.ExecutionReport;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
public interface ExecutionReportService {
    Result<List<ExecutionReport>> listBySuiteId(Long suiteId);

    Result<List<ExecutionReport>> listAll();

    Result<ExecutionReport> getById(Long id);

    Result<List<ExecutionRecord>> getReportDetails(Long reportId);
}
