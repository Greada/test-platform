package com.testplatform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author admin
 * @version 1.0.0
 */
@Data
@TableName("user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String displayName;
    private String role;
    private LocalDateTime createTime;
}
