package com.csci201.backend.service;

import com.csci201.backend.dto.AuthResponse;
import com.csci201.backend.dto.AuthUserResponse;
import com.csci201.backend.dto.LoginRequest;
import com.csci201.backend.dto.SignupRequest;
import com.csci201.backend.entity.User;
import com.csci201.backend.entity.enums.UserRole;
import com.csci201.backend.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse signup(SignupRequest signupRequest) {
        String normalizedUserName = normalizeRequired(signupRequest.getUserName(), "userName");
        Optional<User> existingUser = userRepository.findByUserName(normalizedUserName);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUserName(normalizedUserName);
        user.setPasswordHash(passwordEncoder.encode(normalizeRequired(signupRequest.getPassword(), "password")));
        user.setFirstName(normalizeRequired(signupRequest.getFirstName(), "firstName"));
        user.setLastName(normalizeRequired(signupRequest.getLastName(), "lastName"));
        user.setRole(UserRole.STUDENT);
        user.setCreatedTimestamp(Instant.now());
        user.setLastLoginTimestamp(null);

        User savedUser = userRepository.save(user);
        return new AuthResponse("Account created successfully", toAuthUserResponse(savedUser));
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String normalizedUserName = normalizeRequired(loginRequest.getUserName(), "userName");
        String rawPassword = normalizeRequired(loginRequest.getPassword(), "password");

        Optional<User> userOpt = userRepository.findByUserName(normalizedUserName);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        user.setLastLoginTimestamp(Instant.now());
        User savedUser = userRepository.save(user);
        return new AuthResponse("Login successful", toAuthUserResponse(savedUser));
    }

    public AuthResponse continueAsGuest() {
        AuthUserResponse guestUser = new AuthUserResponse();
        guestUser.setUserName("guest_" + UUID.randomUUID().toString().substring(0, 8));
        guestUser.setFirstName("Guest");
        guestUser.setLastName("User");
        guestUser.setRole("GUEST");
        guestUser.setGuest(true);
        guestUser.setCreatedTimestamp(Instant.now());
        return new AuthResponse("Guest access granted", guestUser);
    }

    private static AuthUserResponse toAuthUserResponse(User user) {
        AuthUserResponse response = new AuthUserResponse();
        response.setUserId(user.getUserId());
        response.setUserName(user.getUserName());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole() == null ? null : user.getRole().name());
        response.setGuest(false);
        response.setCreatedTimestamp(user.getCreatedTimestamp());
        response.setLastLoginTimestamp(user.getLastLoginTimestamp());
        return response;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}