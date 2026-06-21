package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@TableName("test_case")
public class TestCase {
    private Long id;
    private String testNo;
    private String name;
    private Long categoryId;

    @NotBlank(message = "Request URL is required")
    private String requestUrl;

    @NotBlank(message = "Request method is required")
    @Pattern(
            regexp = "^(GET|POST|PUT|PATCH|DELETE)$",
            message = "Only GET, POST, PUT, PATCH, DELETE are allowed")
    private String requestMethod;

    private String requestHeaders;
    private String requestParams;

    @NotBlank(message = "expect result is required")
    private String expectedResult;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
