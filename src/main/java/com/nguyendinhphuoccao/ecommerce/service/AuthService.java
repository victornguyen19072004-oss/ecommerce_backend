package com.nguyendinhphuoccao.ecommerce.service;
import com.nguyendinhphuoccao.ecommerce.dto.AuthRequest;
import com.nguyendinhphuoccao.ecommerce.dto.AuthResponse;
import com.nguyendinhphuoccao.ecommerce.dto.OAuth2Request;
import com.nguyendinhphuoccao.ecommerce.dto.ResetPasswordRequest;

public interface AuthService {
    AuthResponse register(AuthRequest request);
    AuthResponse login(AuthRequest request);
    AuthResponse oauth2Login(OAuth2Request request);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
}