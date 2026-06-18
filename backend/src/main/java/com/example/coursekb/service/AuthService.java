package com.example.coursekb.service;

import com.example.coursekb.dto.LoginRequest;
import com.example.coursekb.dto.RegisterRequest;
import com.example.coursekb.entity.UserAccount;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public UserAccount register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userAccountRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(normalize(request.getEmail()));
        user.setRole("USER");
        return userAccountRepository.save(user);
    }

    public UserAccount login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        return user;
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
