package com.testplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.ErrorPatternItem;
import com.testplatform.common.ErrorPatternResult;
import com.testplatform.common.Result;
import com.testplatform.entity.ExecutionRecord;
import com.testplatform.entity.TestCase;
import com.testplatform.mapper.ExecutionRecordMapper;
import com.testplatform.mapper.TestCaseMapper;

import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class ErrorPatternService {
    private final ExecutionRecordMapper executionRecordMapper;
    private final TestCaseMapper testCaseMapper;

    public ErrorPatternService(
            ExecutionRecordMapper executionRecordMapper, TestCaseMapper testCaseMapper) {
        this.executionRecordMapper = executionRecordMapper;
        this.testCaseMapper = testCaseMapper;
    }

    public Result<ErrorPatternResult> analyze(Long reportId) {
        QueryWrapper<ExecutionRecord> rw = new QueryWrapper<>();
        rw.eq("report_id", reportId);
        List<ExecutionRecord> records = executionRecordMapper.selectList(rw);
        if (records.isEmpty()) {
            return Result.success(new ErrorPatternResult());
        }

        List<Long> caseIds =
                records.stream()
                        .map(ExecutionRecord::getTestCaseId)
                        .distinct()
                        .collect(Collectors.toList());
        List<TestCase> cases = testCaseMapper.selectBatchIds(caseIds);
        Map<Long, TestCase> caseMap =
                cases.stream().collect(Collectors.toMap(TestCase::getId, c -> c));

        Map<String, ErrorPatternItem> groupMap = new LinkedHashMap<>();
        DecimalFormat df = new DecimalFormat("0.00");

        for (ExecutionRecord record : records) {
            TestCase tc = caseMap.get(record.getTestCaseId());
            String url = tc != null ? tc.getRequestUrl() : "unknown";
            String method = tc != null ? tc.getRequestMethod() : "?";
            String key = url + "|" + method;

            ErrorPatternItem item =
                    groupMap.computeIfAbsent(
                            key,
                            k -> {
                                ErrorPatternItem i = new ErrorPatternItem();
                                i.setRequestUrl(url);
                                i.setRequestMethod(method);
                                return i;
                            });

            item.setTotal(item.getTotal() + 1);
            switch (record.getStatus()) {
                case "PASS":
                    item.setPass(item.getPass() + 1);
                    break;
                case "FAIL":
                    item.setFail(item.getFail() + 1);
                    break;
                default:
                    item.setError(item.getError() + 1);
                    break;
            }
        }

        List<ErrorPatternItem> items = new ArrayList<>(groupMap.values());
        String worstEndpoint = null;
        double worstRate = 100;

        for (ErrorPatternItem item : items) {
            double rate = item.getTotal() > 0 ? item.getPass() * 100.0 / item.getTotal() : 0;
            item.setPassRate(df.format(rate) + "%");
            if (rate < worstRate) {
                worstRate = rate;
                worstEndpoint = item.getRequestUrl() + " [" + item.getRequestMethod() + "]";
            }
        }

        ErrorPatternResult result = new ErrorPatternResult();
        result.setItems(items);
        result.setWorstEndpoint(worstEndpoint);
        return Result.success(result);
    }
}
