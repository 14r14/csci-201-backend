package com.csci201.backend.dto;

import jakarta.validation.constraints.NotNull;

public class JoinStudyGroupRequest {

    @NotNull
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
