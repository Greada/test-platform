package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author admin
 * @version 1.0.0
 */
@TableName("ci_build")
@Data
public class CiBuild {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer buildNumber;
    private Integer totalTests;
    private Integer passed;
    private Integer failed;
    private BigDecimal passRate;
    private String status;
    private String buildUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
