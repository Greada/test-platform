package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.ExecutionReportMapper;
import com.testplatform.service.ExecutionReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class ExecutionReportServiceImpl implements ExecutionReportService {
    private final ExecutionReportMapper executionReportMapper;
    private final ExecutionRecordMapper executionRecordMapper;

    @Autowired
    public ExecutionReportServiceImpl(
            ExecutionReportMapper executionReportMapper,
            ExecutionRecordMapper executionRecordMapper) {
        this.executionReportMapper = executionReportMapper;
        this.executionRecordMapper = executionRecordMapper;
    }

    @Override
    public Result<List<ExecutionReport>> listBySuiteId(Long suiteId) {
        try {
            QueryWrapper<ExecutionReport> qw = new QueryWrapper<>();
            qw.eq("suite_id", suiteId).orderByDesc("execute_time");
            List<ExecutionReport> reports = executionReportMapper.selectList(qw);
            return Result.success(reports);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<List<ExecutionReport>> listAll() {
        try {
            QueryWrapper<ExecutionReport> qw = new QueryWrapper<>();
            qw.orderByDesc("execute_time");
            List<ExecutionReport> reports = executionReportMapper.selectList(qw);
            return Result.success(reports);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<ExecutionReport> getById(Long id) {
        try {
            return Result.success(executionReportMapper.selectById(id));
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    @Override
    public Result<List<ExecutionRecord>> getReportDetails(Long reportId) {
        try {
            QueryWrapper<ExecutionRecord> qw = new QueryWrapper<>();
            qw.eq("report_id", reportId).orderByDesc("execute_time");
            List<ExecutionRecord> records = executionRecordMapper.selectList(qw);
            return Result.success(records);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
}
