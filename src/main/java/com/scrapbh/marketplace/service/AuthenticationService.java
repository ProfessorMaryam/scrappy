package com.scrapbh.marketplace.service;

import com.scrapbh.marketplace.dto.AuthResponse;
import com.scrapbh.marketplace.dto.LoginRequest;
import com.scrapbh.marketplace.dto.RegisterRequest;
import com.scrapbh.marketplace.entity.User;
import com.scrapbh.marketplace.exception.InvalidCredentialsException;
import com.scrapbh.marketplace.exception.UnauthorizedException;
import com.scrapbh.marketplace.exception.UserAlreadyExistsException;

public interface AuthenticationService {
    
    /**
     * Register a new user with Supabase Auth and sync to public.users
     * @param request registration details
     * @return AuthResponse with JWT token and user profile
     * @throws UserAlreadyExistsException if username already exists
     */
    AuthResponse register(RegisterRequest request) throws UserAlreadyExistsException;
    
    /**
     * Login user with Supabase Auth
     * @param request login credentials
     * @return AuthResponse with JWT token and user profile
     * @throws InvalidCredentialsException if credentials are invalid
     */
    AuthResponse login(LoginRequest request) throws InvalidCredentialsException;
    
    /**
     * Get current user from JWT token
     * @param token JWT token
     * @return User profile
     * @throws UnauthorizedException if token is invalid
     */
    User getCurrentUser(String token) throws UnauthorizedException;
    
    /**
     * Validate JWT token
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token);
    
    /**
     * Sync Supabase auth user to public.users table
     * @param authUserId Supabase auth user ID
     * @return User profile from public.users
     */
    User syncSupabaseAuthUser(String authUserId);
}
