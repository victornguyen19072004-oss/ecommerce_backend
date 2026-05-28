package com.nguyendinhphuoccao.ecommerce.dto;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}