package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
@TableName("test_case")
public class TestCase {
    private Long id;
    private String name;
    private String requestUrl;
    private String requestMethod;
    private String requestHeaders;
    private String requestParams;
    private String expectedResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
