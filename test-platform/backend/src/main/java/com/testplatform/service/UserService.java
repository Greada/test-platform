package com.testplatform.service;

import com.testplatform.common.Result;
import com.testplatform.entity.User;

/**
 * @author admin
 * @version 1.0.0
 */
public interface UserService {
    Result<?> login(String username, String password);

    Result<Void> register(User user);

    User getById(Long id);
}
