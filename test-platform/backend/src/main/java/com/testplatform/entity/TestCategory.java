package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
@TableName("test_category")
public class TestCategory {
    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<TestCategory> children;
}
