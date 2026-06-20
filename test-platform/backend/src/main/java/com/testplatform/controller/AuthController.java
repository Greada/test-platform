package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.entity.User;
import com.testplatform.service.UserService;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> req) {
        return userService.login(req.get("username"), req.get("password"));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody User user) {
        return userService.register(user);
    }

    @GetMapping("/me")
    public Result<User> me() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.getById(userId);
        return Result.success(user);
    }
}
