package com.csci201.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class StudentCoursesRequest {

    @NotNull(message = "courseCodes is required (use an empty list to clear)")
    private List<@NotBlank(message = "course code cannot be blank") @Size(max = 128) String> courseCodes;

    public StudentCoursesRequest() {}

    public List<String> getCourseCodes() {
        return courseCodes;
    }

    public void setCourseCodes(List<String> courseCodes) {
        this.courseCodes = courseCodes;
    }
}
