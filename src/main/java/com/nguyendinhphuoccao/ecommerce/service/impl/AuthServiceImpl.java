package com.nguyendinhphuoccao.ecommerce.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper; // Đã thêm
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nguyendinhphuoccao.ecommerce.dto.AuthRequest;
import com.nguyendinhphuoccao.ecommerce.dto.AuthResponse;
import com.nguyendinhphuoccao.ecommerce.dto.OAuth2Request;
import com.nguyendinhphuoccao.ecommerce.dto.ResetPasswordRequest;
import com.nguyendinhphuoccao.ecommerce.entity.StaffAccount;
import com.nguyendinhphuoccao.ecommerce.repository.StaffAccountRepository;
import com.nguyendinhphuoccao.ecommerce.security.JwtUtil;
import com.nguyendinhphuoccao.ecommerce.service.AuthService;

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

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String facebookAppSecret;

    @Override
    public AuthResponse oauth2Login(OAuth2Request request) {
        String provider = request.getProvider().toUpperCase();

        if ("GOOGLE".equals(provider)) {
            try {
                GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                        .setAudience(Collections.singletonList(googleClientId))
                        .build();

                GoogleIdToken idToken = verifier.verify(request.getIdToken());
                if (idToken == null) throw new RuntimeException("Google ID Token không hợp lệ!");

                GoogleIdToken.Payload payload = idToken.getPayload();
                return processOAuth2User(payload.getEmail(), (String) payload.get("name"), payload.getSubject(), provider);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi xác thực Google: " + e.getMessage());
            }
        } 
        else if ("FACEBOOK".equals(provider)) {
            try {
                String cleanToken = request.getIdToken().trim().replaceAll("[\n\r]", "");
                System.out.println("🚨 [DEBUG FB TOKEN NHẬN TỪ FE]: " + cleanToken);

                String providerId = null;
                String name = null;
                String email = null;

                // 1. Nếu token từ iPhone (Chế độ Limited Login sinh ra JWT)
                if (cleanToken.startsWith("eyJ")) {
                    // Cắt lấy phần Payload (phần thứ 2 của JWT)
                    String[] chunks = cleanToken.split("\\.");
                    java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
                    String payloadJson = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
                    
                    // Chuyển JSON thành Map
                    ObjectMapper mapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = mapper.readValue(payloadJson, Map.class);
                    
                    providerId = (String) payload.get("sub");
                    name = (String) payload.get("name");
                    email = (String) payload.get("email");
                } 
                // 2. Nếu token từ Android/Web (Access Token truyền thống)
                else {
                    String appSecretProof = generateAppSecretProof(cleanToken, facebookAppSecret);
                    RestTemplate restTemplate = new RestTemplate();
                    String facebookGraphApiUrl = org.springframework.web.util.UriComponentsBuilder
                            .fromHttpUrl("https://graph.facebook.com/me")
                            .queryParam("fields", "id,name,email")
                            .queryParam("access_token", cleanToken)
                            .queryParam("appsecret_proof", appSecretProof)
                            .toUriString();
                    
                    @SuppressWarnings("rawtypes")
                    ResponseEntity<Map> response = restTemplate.getForEntity(facebookGraphApiUrl, Map.class);
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = response.getBody();

                    if (payload == null || !payload.containsKey("id")) {
                        throw new RuntimeException("Facebook Token không hợp lệ!");
                    }

                    providerId = (String) payload.get("id");
                    name = (String) payload.get("name");
                    email = (String) payload.get("email");
                }

                if (providerId == null) {
                    throw new RuntimeException("Không trích xuất được ID từ Facebook.");
                }

                if (email == null || email.isEmpty()) {
                    email = providerId + "@facebook.com"; 
                }

                return processOAuth2User(email, name, providerId, provider);
            } catch (Exception e) {
                System.err.println("🚨 [LỖI FACEBOOK GRAPH API/JWT]: " + e.getMessage());
                throw new RuntimeException("Lỗi xác thực Facebook: " + e.getMessage());
            }
        }
        else {
            throw new RuntimeException("Chưa hỗ trợ provider: " + provider);
        }
    }

    private String generateAppSecretProof(String accessToken, String appSecret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] digest = mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private AuthResponse processOAuth2User(String email, String name, String providerId, String provider) {
        Optional<StaffAccount> existingAccount = repository.findByEmail(email);
        StaffAccount account;

        if (existingAccount.isPresent()) {
            account = existingAccount.get();
            if (account.getProvider().equals("LOCAL")) {
                account.setProvider(provider);
                account.setProviderId(providerId);
                repository.save(account);
            }
        } else {
            String[] nameParts = name != null ? name.split(" ", 2) : new String[]{provider, "User"};
            account = StaffAccount.builder()
                    .firstName(nameParts[0])
                    .lastName(nameParts.length > 1 ? nameParts[1] : "")
                    .email(email)
                    .passwordHash(passwordEncoder.encode(providerId)) 
                    .provider(provider)
                    .providerId(providerId)
                    .active(true)
                    .build();
            repository.save(account);
        }

        String token = jwtUtil.generateToken(account.getEmail());
        return new AuthResponse(token, "Đăng nhập " + provider + " thành công!");
    }

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Override
    public void forgotPassword(String email) {
        StaffAccount account = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy email này trong hệ thống."));
        
        String resetToken = Jwts.builder()
                .setSubject(account.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + 1000 * 60 * 15)) 
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Yêu cầu khôi phục mật khẩu - E-Commerce App");
        
        String resetLink = "myapp://ecommerce.com/reset-password?token=" + resetToken;
        
        message.setText("Xin chào " + account.getFirstName() + ",\n\n"
                + "Bạn vừa yêu cầu khôi phục mật khẩu. Vui lòng sử dụng đường dẫn sau để đặt lại mật khẩu của bạn (có hiệu lực trong 15 phút):\n\n" 
                + resetLink + "\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.");
        
        mailSender.send(message);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseClaimsJws(request.getToken())
                    .getBody();
            
            String email = claims.getSubject();

            StaffAccount account = repository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại."));
                    
            account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            repository.save(account);
            
        } catch (Exception e) {
            throw new RuntimeException("Token không hợp lệ hoặc đã hết hạn!");
        }
    }
}
