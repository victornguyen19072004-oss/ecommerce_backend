package com.nguyendinhphuoccao.ecommerce.dto;
import lombok.Data;

@Data
public class OAuth2Request {
    private String idToken; // Chuỗi token FE nhận được từ Google
    private String provider; // "GOOGLE"
}