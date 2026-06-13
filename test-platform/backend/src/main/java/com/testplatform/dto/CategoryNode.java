package com.testplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
public class CategoryNode {
    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<CategoryNode> children;
}
