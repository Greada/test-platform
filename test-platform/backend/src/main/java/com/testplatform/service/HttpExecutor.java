package com.testplatform.service;

import com.testplatform.entity.TestCase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author admin
 * @version 1.0.0
 */
@Component
public class HttpExecutor {
    private final RestTemplate restTemplate;

    @Autowired
    public HttpExecutor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String execute(TestCase testCase) {
        String url = testCase.getRequestUrl();
        HttpHeaders headers = parseHeaders(testCase.getRequestHeaders());
        HttpEntity<String> entity = new HttpEntity<>(testCase.getRequestParams(), headers);
        switch (testCase.getRequestMethod().toUpperCase()) {
            case "POST":
                return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
            case "PUT":
                return restTemplate.exchange(url, HttpMethod.PUT, entity, String.class).getBody();
            case "DELETE":
                return restTemplate
                        .exchange(url, HttpMethod.DELETE, entity, String.class)
                        .getBody();
            default:
                return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
        }
    }

    private HttpHeaders parseHeaders(String headersJson) {
        HttpHeaders headers = new HttpHeaders();
        if (headersJson == null || headersJson.trim().isEmpty()) {
            return headers;
        }
        try {
            // 格式: {"Content-Type":"application/json","Authorization":"Bearer xxx"}
            String json = headersJson.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                for (String pair : json.split(",")) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replaceAll("^\"|\"$", "");
                        String val = kv[1].trim().replaceAll("^\"|\"$", "");
                        headers.add(key, val);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return headers;
    }
}
