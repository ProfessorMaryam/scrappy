package com.scrapbh.marketplace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrapbh.marketplace.config.SupabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client for interacting with Supabase Auth API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseAuthClient {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sign up a new user with Supabase Auth
     * @param email user email (using username as email for this implementation)
     * @param password user password
     * @return Map containing user ID and access token
     */
    public Map<String, String> signUp(String email, String password) {
        String url = supabaseConfig.getUrl() + "/auth/v1/signup";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseConfig.getKey());
        
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String userId = jsonNode.get("user").get("id").asText();
            String accessToken = jsonNode.get("access_token").asText();
            
            Map<String, String> result = new HashMap<>();
            result.put("userId", userId);
            result.put("accessToken", accessToken);
            
            return result;
        } catch (Exception e) {
            log.error("Error signing up user: {}", e.getMessage());
            throw new RuntimeException("Failed to create auth user", e);
        }
    }

    /**
     * Sign in a user with Supabase Auth
     * @param email user email
     * @param password user password
     * @return Map containing user ID and access token
     */
    public Map<String, String> signIn(String email, String password) {
        String url = supabaseConfig.getUrl() + "/auth/v1/token?grant_type=password";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseConfig.getKey());
        
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String userId = jsonNode.get("user").get("id").asText();
            String accessToken = jsonNode.get("access_token").asText();
            
            Map<String, String> result = new HashMap<>();
            result.put("userId", userId);
            result.put("accessToken", accessToken);
            
            return result;
        } catch (HttpClientErrorException e) {
            log.error("Error signing in user: {}", e.getMessage());
            throw new RuntimeException("Invalid credentials", e);
        } catch (Exception e) {
            log.error("Error signing in user: {}", e.getMessage());
            throw new RuntimeException("Failed to sign in", e);
        }
    }

    /**
     * Get user information from JWT token
     * @param token JWT access token
     * @return Map containing user information
     */
    public Map<String, Object> getUser(String token) {
        String url = supabaseConfig.getUrl() + "/auth/v1/user";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", supabaseConfig.getKey());
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                request, 
                String.class
            );
            
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            Map<String, Object> result = new HashMap<>();
            result.put("id", jsonNode.get("id").asText());
            result.put("email", jsonNode.get("email").asText());
            
            return result;
        } catch (Exception e) {
            log.error("Error getting user: {}", e.getMessage());
            throw new RuntimeException("Failed to get user", e);
        }
    }

    /**
     * Verify JWT token validity
     * @param token JWT access token
     * @return true if token is valid, false otherwise
     */
    public boolean verifyToken(String token) {
        try {
            getUser(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
