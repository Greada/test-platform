package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
@TableName("test_suite_case")
public class TestSuiteCase {
    private Long id;
    private Long suiteId;
    private Long caseId;
    private Integer sortOrder;
}
