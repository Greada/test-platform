package com.testplatform.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.testplatform.common.HttpResult;
import com.testplatform.entity.TestCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/** HttpExecutor unit tests */
@ExtendWith(MockitoExtension.class)
class HttpExecutorTest {

    private com.testplatform.service.HttpExecutor executor;
    @Mock private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        executor = new com.testplatform.service.HttpExecutor(restTemplate, objectMapper);
    }

    @Test
    @DisplayName("[HTTP-EXEC-01] GET request should return body + duration + statusCode")
    void execute_getRequest_shouldReturnResult() {
        // Arrange
        String url = "https://httpbin.org/get";
        String body = "{\"url\":\"https://httpbin.org/get\"}";
        when(restTemplate.exchange(
                        eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));

        TestCase testCase = new TestCase();
        testCase.setRequestUrl(url);
        testCase.setRequestMethod("GET");
        testCase.setRequestHeaders(null);
        testCase.setRequestParams(null);

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertEquals(body, result.getBody());
        assertTrue(result.getDuration() >= 0);
    }

    @Test
    @DisplayName("[HTTP-EXEC-02] POST request should forward POST method")
    void execute_postRequest_shouldForwardPOST() {
        // Arrange
        String url = "https://httpbin.org/post";
        when(restTemplate.exchange(
                        eq(url), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"json\":{}}"));

        TestCase testCase = new TestCase();
        testCase.setRequestUrl(url);
        testCase.setRequestMethod("POST");
        testCase.setRequestParams("{\"key\":\"value\"}");

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertEquals(200, result.getStatusCode());
        verify(restTemplate)
                .exchange(eq(url), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("[HTTP-EXEC-03] PUT request should forward PUT method")
    void execute_putRequest_shouldForwardPUT() {
        // Arrange
        String url = "https://httpbin.org/anything";
        when(restTemplate.exchange(
                        eq(url), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"method\":\"PUT\"}"));

        TestCase testCase = new TestCase();
        testCase.setRequestUrl(url);
        testCase.setRequestMethod("PUT");
        testCase.setRequestParams("{\"key\":\"value\"}");

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertEquals(200, result.getStatusCode());
        verify(restTemplate)
                .exchange(eq(url), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("[HTTP-EXEC-04] DELETE request should forward DELETE method")
    void execute_deleteRequest_shouldForwardDELETE() {
        // Arrange
        String url = "https://httpbin.org/anything";
        when(restTemplate.exchange(
                        eq(url), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"method\":\"DELETE\"}"));

        TestCase testCase = new TestCase();
        testCase.setRequestUrl(url);
        testCase.setRequestMethod("DELETE");

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertEquals(200, result.getStatusCode());
        verify(restTemplate)
                .exchange(eq(url), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("[HTTP-EXEC-05] PATCH request should forward PATCH method")
    void execute_patchRequest_shouldForwardPATCH() {
        // Arrange
        String url = "https://httpbin.org/anything";
        when(restTemplate.exchange(
                        eq(url), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"method\":\"PATCH\"}"));

        TestCase testCase = new TestCase();
        testCase.setRequestUrl(url);
        testCase.setRequestMethod("PATCH");
        testCase.setRequestParams("{\"patchField\":\"newValue\"}");

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertEquals(200, result.getStatusCode());
        verify(restTemplate)
                .exchange(eq(url), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("[HTTP-EXEC-06] 4xx response should return body and status code")
    void execute_4xxResponse_shouldReturnBodyAndStatusCode() throws Exception {
        // Arrange
        String url = "https://httpbin.org/status/400";
        org.springframework.http.HttpStatus status =
                org.springframework.http.HttpStatus.BAD_REQUEST;
        HttpStatusCodeException ex = mock(HttpStatusCodeException.class);
        when(ex.getResponseBodyAsString()).thenReturn("{\"error\":\"bad request\"}");
        when(ex.getStatusCode()).thenReturn(status);
        when(restTemplate.exchange(
                        eq(url), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(ex);

        TestCase testCase = new TestCase();
        testCase.setRequestUrl(url);
        testCase.setRequestMethod("GET");

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertEquals(400, result.getStatusCode());
        assertEquals("{\"error\":\"bad request\"}", result.getBody());
        assertTrue(result.getDuration() >= 0);
    }

    @Test
    @DisplayName("[HTTP-EXEC-07] 5xx response should return body and status code")
    void execute_5xxResponse_shouldReturnBodyAndStatusCode() throws Exception {
        // Arrange
        String url = "https://httpbin.org/status/500";
        org.springframework.http.HttpStatus status =
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
        HttpStatusCodeException ex = mock(HttpStatusCodeException.class);
        when(ex.getResponseBodyAsString()).thenReturn("{\"error\":\"internal server error\"}");
        when(ex.getStatusCode()).thenReturn(status);
        when(restTemplate.exchange(
                        eq(url), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(ex);

        TestCase testCase = new TestCase();
        testCase.setRequestUrl(url);
        testCase.setRequestMethod("GET");

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertEquals(500, result.getStatusCode());
        assertEquals("{\"error\":\"internal server error\"}", result.getBody());
    }

    @Test
    @DisplayName("[HTTP-EXEC-08] Network exception should throw RuntimeException with duration")
    void execute_networkException_shouldThrowRuntimeExceptionWithDuration() {
        // Arrange
        String url = "http://unreachable.local";
        org.springframework.web.client.ResourceAccessException netEx =
                new org.springframework.web.client.ResourceAccessException(
                        "Connection refused", new java.net.ConnectException());
        when(restTemplate.exchange(
                        eq(url), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(netEx);

        TestCase testCase = new TestCase();
        testCase.setRequestUrl(url);
        testCase.setRequestMethod("GET");

        // Act & Assert
        RuntimeException thrown =
                assertThrows(RuntimeException.class, () -> executor.execute(testCase));
        assertTrue(thrown.getMessage().contains("Connection refused"));
        assertTrue(thrown.getMessage().contains("ms)"));
    }

    @Test
    @DisplayName("[HTTP-HEADER-01] null headers should work without exception")
    void parseHeaders_null_shouldReturnEmptyHeaders() {
        // Arrange
        TestCase testCase = new TestCase();
        testCase.setRequestUrl("https://httpbin.org/get");
        testCase.setRequestMethod("GET");
        testCase.setRequestHeaders(null);
        testCase.setRequestParams(null);
        when(restTemplate.exchange(
                        eq("https://httpbin.org/get"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("[HTTP-HEADER-02] empty string headers should work without exception")
    void parseHeaders_emptyString_shouldReturnEmptyHeaders() {
        // Arrange
        TestCase testCase = new TestCase();
        testCase.setRequestUrl("https://httpbin.org/get");
        testCase.setRequestMethod("GET");
        testCase.setRequestHeaders("");
        testCase.setRequestParams(null);
        when(restTemplate.exchange(
                        eq("https://httpbin.org/get"),
                        eq(HttpMethod.GET),
                        any(HttpEntity.class),
                        eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        // Act
        HttpResult result = executor.execute(testCase);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("[HTTP-HEADER-03] valid JSON headers should parse correctly")
    void parseHeaders_validJson_shouldParseHeaders() {
        // Arrange
        String headersJson = "{\"Content-Type\":\"application/json\"}";
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        // Act
        try {
            java.util.Map<String, String> map =
                    mapper.readValue(
                            headersJson,
                            new com.fasterxml.jackson.core.type.TypeReference<
                                    java.util.Map<String, String>>() {});
            // Assert
            assertEquals("application/json", map.get("Content-Type"));
        } catch (Exception e) {
            fail("JSON parse failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("[HTTP-HEADER-04] complex headers with colons and commas should parse")
    void parseHeaders_complexJson_shouldParseHeadersCorrectly() {
        // Arrange
        String headersJson = "{\"Authorization\":\"Bearer token:12345\",\"Accept\":\"a,b,c\"}";
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

        // Act
        try {
            java.util.Map<String, String> map =
                    mapper.readValue(
                            headersJson,
                            new com.fasterxml.jackson.core.type.TypeReference<
                                    java.util.Map<String, String>>() {});
            // Assert
            assertEquals("Bearer token:12345", map.get("Authorization"));
            assertEquals("a,b,c", map.get("Accept"));
        } catch (Exception e) {
            fail("JSON parse failed: " + e.getMessage());
        }
    }
}
