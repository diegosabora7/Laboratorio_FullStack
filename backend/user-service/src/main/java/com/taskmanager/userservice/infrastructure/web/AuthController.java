package com.taskmanager.userservice.infrastructure.web;

import com.taskmanager.userservice.application.dto.AuthResponse;
import com.taskmanager.userservice.application.dto.LoginRequest;
import com.taskmanager.userservice.application.dto.RegisterRequest;
import com.taskmanager.userservice.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        AuthResponse response = authService.refresh(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validate(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        AuthResponse response = authService.validate(token);
        return ResponseEntity.ok(response);
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Invalid Authorization header");
    }
}
