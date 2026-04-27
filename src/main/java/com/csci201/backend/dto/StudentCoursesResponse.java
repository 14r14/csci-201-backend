package com.csci201.backend.dto;

import java.util.List;

public class StudentCoursesResponse {
    private Long userId;
    private List<String> courseCodes;

    public StudentCoursesResponse() {}

    public StudentCoursesResponse(Long userId, List<String> courseCodes) {
        this.userId = userId;
        this.courseCodes = courseCodes;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getCourseCodes() {
        return courseCodes;
    }

    public void setCourseCodes(List<String> courseCodes) {
        this.courseCodes = courseCodes;
    }
}
