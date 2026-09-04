package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.ExecutionReport;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.ExecutionReportMapper;
import com.testplatform.service.ExecutionReportService;

import com.testplatform.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
        qw.eq("suite_id", suiteId)
                .eq("creator_id", SecurityUtils.getCurrentUserId())
                .orderByDesc("execute_time");
        List<ExecutionReport> reports = executionReportMapper.selectList(qw);
        return Result.success(reports);
    }

    @Override
    public Result<List<ExecutionReport>> listAll() {
        QueryWrapper<ExecutionReport> qw = new QueryWrapper<>();
        qw.eq("creator_id", SecurityUtils.getCurrentUserId())
                .orderByDesc("execute_time");
        List<ExecutionReport> reports = executionReportMapper.selectList(qw);
        return Result.success(reports);
    }

    @Override
    public Result<ExecutionReport> getById(Long id) {
        ExecutionReport report = executionReportMapper.selectById(id);
        if (report == null) {
            return Result.error(404, "报告不存在");
        }
        if (!Objects.equals(report.getCreatorId(), SecurityUtils.getCurrentUserId())) {
            return Result.error(404, "报告不存在");
        }
        return Result.success(report);
    }

    @Override
    public Result<List<ExecutionRecord>> getReportDetails(Long reportId) {
        ExecutionReport report = executionReportMapper.selectById(reportId);
        if (report == null) {
            return Result.error(404, "报告不存在");
        }
        if (!Objects.equals(report.getCreatorId(), SecurityUtils.getCurrentUserId())) {
            return Result.error(404, "报告不存在");
        }
        QueryWrapper<ExecutionRecord> qw = new QueryWrapper<>();
        qw.eq("report_id", reportId).orderByDesc("execute_time");
        List<ExecutionRecord> records = executionRecordMapper.selectList(qw);
        return Result.success(records);
    }
}
