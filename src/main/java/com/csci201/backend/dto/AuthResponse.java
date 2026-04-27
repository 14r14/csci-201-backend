package com.csci201.backend.dto;

public class AuthResponse {
    private String message;
    private AuthUserResponse user;

    public AuthResponse() {}

    public AuthResponse(String message, AuthUserResponse user) {
        this.message = message;
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AuthUserResponse getUser() {
        return user;
    }

    public void setUser(AuthUserResponse user) {
        this.user = user;
    }
}
