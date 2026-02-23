package com.scrapbh.marketplace.controller;

import com.scrapbh.marketplace.dto.AuthResponse;
import com.scrapbh.marketplace.dto.LoginRequest;
import com.scrapbh.marketplace.dto.RegisterRequest;
import com.scrapbh.marketplace.entity.User;
import com.scrapbh.marketplace.exception.InvalidCredentialsException;
import com.scrapbh.marketplace.exception.UnauthorizedException;
import com.scrapbh.marketplace.exception.UserAlreadyExistsException;
import com.scrapbh.marketplace.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new user
     * POST /api/auth/register
     * 
     * @param request RegisterRequest containing username, password, full_name, and role
     * @return AuthResponse with JWT token and user profile
     * @throws UserAlreadyExistsException if username already exists
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) 
            throws UserAlreadyExistsException {
        log.info("Registration request received for username: {}", request.getUsername());
        AuthResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user
     * POST /api/auth/login
     * 
     * @param request LoginRequest containing username and password
     * @return AuthResponse with JWT token and user profile
     * @throws InvalidCredentialsException if credentials are invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) 
            throws InvalidCredentialsException {
        log.info("Login request received for username: {}", request.getUsername());
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user
     * GET /api/auth/me
     * 
     * @param authorization Authorization header with Bearer token
     * @return User profile of the authenticated user
     * @throws UnauthorizedException if token is invalid or expired
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) 
            throws UnauthorizedException {
        log.debug("Get current user request received");
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("No token provided");
        }
        
        String token = authorization.substring(7); // Remove "Bearer " prefix
        User user = authenticationService.getCurrentUser(token);
        return ResponseEntity.ok(user);
    }

    /**
     * Logout user
     * POST /api/auth/logout
     * 
     * Note: Since we use stateless JWT authentication, logout is handled client-side
     * by removing the token from storage. This endpoint is provided for consistency
     * and can be extended in the future for token blacklisting if needed.
     * 
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        log.info("Logout request received");
        // With stateless JWT, logout is handled client-side
        // Future enhancement: implement token blacklisting if needed
        return ResponseEntity.ok("Logged out successfully");
    }
}
