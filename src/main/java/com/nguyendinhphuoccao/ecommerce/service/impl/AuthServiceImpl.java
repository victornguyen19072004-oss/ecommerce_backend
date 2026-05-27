package com.nguyendinhphuoccao.ecommerce.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nguyendinhphuoccao.ecommerce.dto.AuthRequest;
import com.nguyendinhphuoccao.ecommerce.dto.AuthResponse;
import com.nguyendinhphuoccao.ecommerce.dto.OAuth2Request;
import com.nguyendinhphuoccao.ecommerce.dto.ResetPasswordRequest;
import com.nguyendinhphuoccao.ecommerce.entity.StaffAccount;
import com.nguyendinhphuoccao.ecommerce.repository.StaffAccountRepository;
import com.nguyendinhphuoccao.ecommerce.security.JwtUtil;
import com.nguyendinhphuoccao.ecommerce.service.AuthService;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final StaffAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    @Override
    public AuthResponse register(AuthRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại!");
        }

        StaffAccount account = StaffAccount.builder()
                .firstName(request.getFirstName() != null ? request.getFirstName() : "User")
                .lastName(request.getLastName() != null ? request.getLastName() : "")
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .active(true)
                .build();
        repository.save(account);

        String token = jwtUtil.generateToken(account.getEmail());
        return new AuthResponse(token, "Đăng ký thành công");
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        StaffAccount account = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new RuntimeException("Sai mật khẩu!");
        }

        String token = jwtUtil.generateToken(account.getEmail());
        return new AuthResponse(token, "Đăng nhập thành công");
    }

    // Đọc Client ID từ application.properties
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Override
    public AuthResponse oauth2Login(OAuth2Request request) {
        if (!"GOOGLE".equalsIgnoreCase(request.getProvider())) {
            throw new RuntimeException("Chưa hỗ trợ provider: " + request.getProvider());
        }

        try {
            // Khởi tạo bộ xác minh token của Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // Xác minh token FE gửi lên
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            
            if (idToken == null) {
                throw new RuntimeException("Google ID Token không hợp lệ hoặc đã hết hạn!");
            }

            // Bóc tách thông tin từ Token
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String providerId = payload.getSubject(); // ID duy nhất của user trên hệ thống Google

            Optional<StaffAccount> existingAccount = repository.findByEmail(email);
            StaffAccount account;

            if (existingAccount.isPresent()) {
                account = existingAccount.get();
                if (account.getProvider().equals("LOCAL")) {
                    account.setProvider(request.getProvider());
                    account.setProviderId(providerId);
                    repository.save(account);
                }
            } else {
                String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"Google", "User"};
                account = StaffAccount.builder()
                        .firstName(nameParts[0])
                        .lastName(nameParts.length > 1 ? nameParts[1] : "")
                        .email(email)
                        .passwordHash(passwordEncoder.encode(providerId)) 
                        .provider(request.getProvider())
                        .providerId(providerId)
                        .active(true)
                        .build();
                repository.save(account);
            }

            String token = jwtUtil.generateToken(account.getEmail());
            return new AuthResponse(token, "Đăng nhập Google thành công!");

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xác thực Google: " + e.getMessage());
        }
    }

    @Value("${app.jwt.secret}")
    private String jwtSecret; // Lấy secret từ application.properties để tự sinh token ngắn hạn

    @Override
    public void forgotPassword(String email) {
        StaffAccount account = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy email này trong hệ thống."));
        
        // Sinh ra một Reset Token có thời hạn ngắn (VD: 15 phút) thay vì dùng Access Token thông thường
        String resetToken = Jwts.builder()
                .setSubject(account.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + 1000 * 60 * 15)) // Hết hạn sau 15 phút
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
        
        // Soạn nội dung Email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Yêu cầu khôi phục mật khẩu - E-Commerce App");
        
        // Tạm thời để link web. Nếu app Flutter có cấu hình DeepLink, đường link này sẽ kích hoạt mở app.
        String resetLink = "myapp://ecommerce.com/reset-password?token=" + resetToken;
        
        message.setText("Xin chào " + account.getFirstName() + ",\n\n"
                + "Bạn vừa yêu cầu khôi phục mật khẩu. Vui lòng sử dụng đường dẫn sau để đặt lại mật khẩu của bạn (có hiệu lực trong 15 phút):\n\n" 
                + resetLink + "\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.");
        
        mailSender.send(message);
    }

    // BỔ SUNG PHƯƠNG THỨC MỚI NÀY
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        try {
            // 1. Giải mã token để lấy email
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseClaimsJws(request.getToken())
                    .getBody();
            
            String email = claims.getSubject();

            // 2. Tìm User và cập nhật mật khẩu mới
            StaffAccount account = repository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));
                    
            account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            repository.save(account);
            
        } catch (Exception e) {
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn!");
        }
    }
}