package com.taskmanager.userservice.infrastructure.web;

import com.taskmanager.userservice.application.dto.UserResponse;
import com.taskmanager.userservice.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllActiveUsers() {
        return ResponseEntity.ok(userService.getAllActiveUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/name")
    public ResponseEntity<Map<String, String>> getUserNameById(@PathVariable Long id) {
        String name = userService.getUserNameById(id);
        return ResponseEntity.ok(Map.of("name", name));
    }
}
