package com.example.coursekb.controller;

import com.example.coursekb.dto.LoginRequest;
import com.example.coursekb.dto.RegisterRequest;
import com.example.coursekb.entity.UserAccount;
import com.example.coursekb.service.AuthService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserAccount register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserAccount login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
