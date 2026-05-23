package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.TestCaseMapper;
import com.testplatform.service.ExecutionService;
import com.testplatform.service.HttpExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class ExecutionServiceImpl implements ExecutionService {
    private final ExecutionRecordMapper executionRecordMapper;
    private final TestCaseMapper testCaseMapper;
    private final HttpExecutor httpExecutor;

    @Autowired
    public ExecutionServiceImpl(
            ExecutionRecordMapper executionRecordMapper,
            TestCaseMapper testCaseMapper,
            HttpExecutor httpExecutor) {
        this.executionRecordMapper = executionRecordMapper;
        this.testCaseMapper = testCaseMapper;
        this.httpExecutor = httpExecutor;
    }

    @Override
    public Result<Void> execute(Long testCaseId) {
        try {
            TestCase testCase = testCaseMapper.selectById(testCaseId);
            if (testCase == null) {
                return Result.error("testcase not found!");
            }
            String actualResult = httpExecutor.execute(testCase);
            ExecutionRecord executionRecord = new ExecutionRecord();
            executionRecord.setTestCaseId(testCaseId);
            executionRecord.setActualResult(actualResult);

            String executed = testCase.getExpectedResult();
            if (executed != null && executed.equals(actualResult)) {
                executionRecord.setStatus("PASS");
            } else {
                executionRecord.setStatus("FAIL");
            }

            executionRecordMapper.insert(executionRecord);
            return Result.success(null);
        } catch (Exception e) {
            ExecutionRecord record = new ExecutionRecord();
            record.setTestCaseId(testCaseId);
            record.setStatus("ERROR");
            record.setActualResult(e.getMessage());
            executionRecordMapper.insert(record);
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<List<ExecutionRecord>> listByTestCaseId(Long testCaseId) {
        try {
            QueryWrapper<ExecutionRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("test_case_id", testCaseId).orderByDesc("execute_time");
            List<ExecutionRecord> recordList = executionRecordMapper.selectList(wrapper);
            return Result.success(recordList);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
