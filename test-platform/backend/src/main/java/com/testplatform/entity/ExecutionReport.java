package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
@TableName("execution_report")
public class ExecutionReport {
    private Long id;
    private Long suiteId;
    private String reportName;
    private Integer total;
    private Integer passed;
    private Integer failed;
    private Integer errored;
    private Double passRate;
    private String status;
    private LocalDateTime executeTime;
    private LocalDateTime createTime;
}
