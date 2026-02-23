package com.scrapbh.marketplace.service.impl;

import com.scrapbh.marketplace.dto.AuthResponse;
import com.scrapbh.marketplace.dto.LoginRequest;
import com.scrapbh.marketplace.dto.RegisterRequest;
import com.scrapbh.marketplace.entity.User;
import com.scrapbh.marketplace.exception.InvalidCredentialsException;
import com.scrapbh.marketplace.exception.UnauthorizedException;
import com.scrapbh.marketplace.exception.UserAlreadyExistsException;
import com.scrapbh.marketplace.repository.UserRepository;
import com.scrapbh.marketplace.service.AuthenticationService;
import com.scrapbh.marketplace.service.SupabaseAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final SupabaseAuthClient supabaseAuthClient;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) throws UserAlreadyExistsException {
        log.info("Registering new user with username: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken");
        }
        
        // Create user in Supabase Auth
        // Using username@scrapbh.local as email format since Supabase requires email
        String email = request.getUsername() + "@scrapbh.local";
        Map<String, String> authResult;
        
        try {
            authResult = supabaseAuthClient.signUp(email, request.getPassword());
        } catch (Exception e) {
            log.error("Failed to create auth user: {}", e.getMessage());
            throw new RuntimeException("Failed to create user account", e);
        }
        
        String authUserId = authResult.get("userId");
        String accessToken = authResult.get("accessToken");
        
        // Create user in public.users table
        User user = new User();
        user.setId(UUID.fromString(authUserId));
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        
        user = userRepository.save(user);
        
        log.info("Successfully registered user: {}", user.getUsername());
        
        return new AuthResponse(accessToken, user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) throws InvalidCredentialsException {
        log.info("User login attempt: {}", request.getUsername());
        
        // Verify username exists in our database
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        User user = userOpt.get();
        
        // Authenticate with Supabase Auth
        String email = request.getUsername() + "@scrapbh.local";
        Map<String, String> authResult;
        
        try {
            authResult = supabaseAuthClient.signIn(email, request.getPassword());
        } catch (Exception e) {
            log.error("Authentication failed for user {}: {}", request.getUsername(), e.getMessage());
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        String accessToken = authResult.get("accessToken");
        
        log.info("Successfully authenticated user: {}", user.getUsername());
        
        return new AuthResponse(accessToken, user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(String token) throws UnauthorizedException {
        log.debug("Getting current user from token");
        
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedException("No token provided");
        }
        
        // Get user info from Supabase Auth
        Map<String, Object> userInfo;
        try {
            userInfo = supabaseAuthClient.getUser(token);
        } catch (Exception e) {
            log.error("Failed to get user from token: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired token");
        }
        
        String userId = (String) userInfo.get("id");
        
        // Fetch user from database
        return userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        return supabaseAuthClient.verifyToken(token);
    }

    @Override
    @Transactional
    public User syncSupabaseAuthUser(String authUserId) {
        log.info("Syncing Supabase auth user: {}", authUserId);
        
        UUID userId = UUID.fromString(authUserId);
        
        // Check if user already exists
        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            log.debug("User already exists in public.users: {}", authUserId);
            return existingUser.get();
        }
        
        // This method is called when we need to sync an existing auth user
        // In practice, this would be called from a webhook or admin function
        // For now, we'll throw an exception as users should be created via register()
        throw new IllegalStateException("User sync should be done during registration");
    }
}
