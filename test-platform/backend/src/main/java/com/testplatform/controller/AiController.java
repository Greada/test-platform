package com.testplatform.controller;

import com.testplatform.common.Result;
import com.testplatform.service.AiService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author admin
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService aiService;

    @Autowired
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/expected")
    public Result<String> generateExpected(@RequestBody Map<String, String> request) {
        String result =
                aiService.generateExpectedResult(
                        request.get("requestUrl"),
                        request.get("requestMethod"),
                        request.get("requestHeaders"),
                        request.get("requestParams"));
        return Result.success(result);
    }
}
