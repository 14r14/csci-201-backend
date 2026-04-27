package com.csci201.backend.controller;

import com.csci201.backend.dto.StudentCoursesRequest;
import com.csci201.backend.dto.StudentCoursesResponse;
import com.csci201.backend.service.StudentCourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/courses")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    public StudentCourseController(StudentCourseService studentCourseService) {
        this.studentCourseService = studentCourseService;
    }

    /** Onboarding: replace the student's enrolled course list (stored as JSON on {@code users.courses}). */
    @PutMapping
    public StudentCoursesResponse setCourses(
            @PathVariable Long userId, @Valid @RequestBody StudentCoursesRequest request) {
        return studentCourseService.setStudentCourses(userId, request.getCourseCodes());
    }

    /** Current selections for the profile UI or downstream matching jobs. */
    @GetMapping
    public StudentCoursesResponse getCourses(@PathVariable Long userId) {
        return studentCourseService.getCourses(userId);
    }
}
