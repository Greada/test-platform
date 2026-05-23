package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
@TableName("execution_record")
public class ExecutionRecord {
    private Long id;
    private Long testCaseId;
    private String status;
    private String requestDetail;
    private String responseDetail;
    private String actualResult;
    private LocalDateTime executeTime;
}
