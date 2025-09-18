package io.monosense.synergyflow.testsupport.api;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Helper class for API testing with TestRestTemplate.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class ApiTestHelper {
    
    private final TestRestTemplate restTemplate;
    private final String baseUrl;
    
    public ApiTestHelper(TestRestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Performs a GET request to the specified path.
     * 
     * @param path the path to request
     * @param responseType the response type
     * @param <T> the response type
     * @return the response entity
     */
    public <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return restTemplate.getForEntity(baseUrl + path, responseType);
    }
    
    /**
     * Performs a POST request to the specified path.
     * 
     * @param path the path to request
     * @param body the request body
     * @param responseType the response type
     * @param <T> the response type
     * @param <R> the request body type
     * @return the response entity
     */
    public <T, R> ResponseEntity<T> post(String path, R body, Class<T> responseType) {
        return restTemplate.postForEntity(baseUrl + path, body, responseType);
    }
    
    /**
     * Performs a PUT request to the specified path.
     * 
     * @param path the path to request
     * @param body the request body
     * @param responseType the response type
     * @param <T> the response type
     * @param <R> the request body type
     * @return the response entity
     */
    public <T, R> ResponseEntity<T> put(String path, R body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        HttpEntity<R> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(baseUrl + path, HttpMethod.PUT, entity, responseType);
    }
    
    /**
     * Performs a DELETE request to the specified path.
     * 
     * @param path the path to request
     * @param responseType the response type
     * @param <T> the response type
     * @return the response entity
     */
    public <T> ResponseEntity<T> delete(String path, Class<T> responseType) {
        return restTemplate.exchange(baseUrl + path, HttpMethod.DELETE, null, responseType);
    }
}