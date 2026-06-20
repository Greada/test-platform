package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.ExecutionReportMapper;
import com.testplatform.service.ExecutionReportService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExecutionReportServiceImpl implements ExecutionReportService {
    private final ExecutionReportMapper executionReportMapper;
    private final ExecutionRecordMapper executionRecordMapper;

    public ExecutionReportServiceImpl(
            ExecutionReportMapper executionReportMapper,
            ExecutionRecordMapper executionRecordMapper) {
        this.executionReportMapper = executionReportMapper;
        this.executionRecordMapper = executionRecordMapper;
    }

    @Override
    public Result<List<ExecutionReport>> listBySuiteId(Long suiteId) {
        QueryWrapper<ExecutionReport> qw = new QueryWrapper<>();
        qw.eq("suite_id", suiteId).orderByDesc("execute_time");
        List<ExecutionReport> reports = executionReportMapper.selectList(qw);
        return Result.success(reports);
    }

    @Override
    public Result<List<ExecutionReport>> listAll() {
        QueryWrapper<ExecutionReport> qw = new QueryWrapper<>();
        qw.orderByDesc("execute_time");
        List<ExecutionReport> reports = executionReportMapper.selectList(qw);
        return Result.success(reports);
    }

    @Override
    public Result<ExecutionReport> getById(Long id) {
        return Result.success(executionReportMapper.selectById(id));
    }

    @Override
    public Result<List<ExecutionRecord>> getReportDetails(Long reportId) {
        QueryWrapper<ExecutionRecord> qw = new QueryWrapper<>();
        qw.eq("report_id", reportId).orderByDesc("execute_time");
        List<ExecutionRecord> records = executionRecordMapper.selectList(qw);
        return Result.success(records);
    }
}
