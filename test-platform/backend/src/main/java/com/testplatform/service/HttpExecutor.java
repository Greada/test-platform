package com.testplatform.service;

import com.testplatform.entity.TestCase;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author admin
 * @version 1.0.0
 */
@Component
public class HttpExecutor {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String execute(TestCase testCase) {
        RestTemplate restTemplate = new RestTemplate();
        String url = testCase.getRequestUrl();
        switch (testCase.getRequestMethod().toUpperCase()) {
            case "POST":
                return restTemplate.postForObject(url, null, String.class);
            case "PUT":
                restTemplate.put(url, null);
                return "";
            case "DELETE":
                restTemplate.delete(url);
                return "";
            default:
                return restTemplate.getForObject(url, String.class);
        }
    }
}
