package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@TableName("test_case")
public class TestCase {
    private Long id;
    private String testNo;
    private String name;
    private String requestUrl;
    private String requestMethod;
    private String requestHeaders;
    private String requestParams;

    @NotBlank(message = "预期结果不能为空")
    private String expectedResult;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
