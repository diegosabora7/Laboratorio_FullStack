package com.taskmanager.userservice.application.service;

import com.taskmanager.userservice.application.dto.AuthResponse;
import com.taskmanager.userservice.application.dto.LoginRequest;
import com.taskmanager.userservice.application.dto.RegisterRequest;
import com.taskmanager.userservice.domain.model.Role;
import com.taskmanager.userservice.domain.model.User;
import com.taskmanager.userservice.domain.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Role userRole = Role.USER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            userRole = Role.valueOf(request.getRole());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .activo(true)
                .build();

        user = userRepository.save(user);
        String token = generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getActivo()) {
            throw new RuntimeException("User account is deactivated");
        }

        String token = generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse validate(String token) {
        Claims claims = parseToken(token);

        return AuthResponse.builder()
                .token(token)
                .userId(claims.get("userId", Long.class))
                .name(claims.get("name", String.class))
                .email(claims.getSubject())
                .role(claims.get("role", String.class))
                .build();
    }

    public AuthResponse refresh(String token) {
        Claims claims = parseTokenWithClockSkew(token);

        String email = claims.getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newToken = generateToken(user);

        return AuthResponse.builder()
                .token(newToken)
                .userId(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("name", user.getFullName())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private Claims parseTokenWithClockSkew(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).clockSkewSeconds(86400).build().parseSignedClaims(token).getPayload();
    }
}
