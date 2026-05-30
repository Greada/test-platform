package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
@TableName("test_suite")
public class TestSuite {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
