package com.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.testplatform.common.Result;
import com.testplatform.config.JwtUtil;
import com.testplatform.entity.User;
import com.testplatform.mapper.UserMapper;
import com.testplatform.service.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(
            UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Result<?> login(String username, String password) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            return Result.error("username or password error");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("username or password error");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        user.setPassword(null);
        data.put("user", user);
        return Result.success(data);
    }

    @Override
    public Result<Void> register(User user) {
        User exist =
                userMapper.selectOne(new QueryWrapper<User>().eq("username", user.getUsername()));
        if (exist != null) {
            return Result.error("username already exist");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userMapper.insert(user);
        return Result.success("register success", null);
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
