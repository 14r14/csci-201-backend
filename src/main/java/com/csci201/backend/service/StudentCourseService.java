package com.csci201.backend.service;

import com.csci201.backend.dto.StudentCoursesResponse;
import com.csci201.backend.entity.User;
import com.csci201.backend.entity.enums.UserRole;
import com.csci201.backend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentCourseService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public StudentCourseService(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public StudentCoursesResponse getCourses(Long userId) {
        User user =
                userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return new StudentCoursesResponse(userId, deserializeCourses(user.getCourses()));
    }

    @Transactional
    public StudentCoursesResponse setStudentCourses(Long userId, List<String> rawCourseCodes) {
        User user =
                userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getRole() != UserRole.STUDENT) {
            throw new IllegalArgumentException("Only students can set enrolled courses");
        }

        List<String> normalized = normalizeCourseCodes(rawCourseCodes);
        try {
            user.setCourses(objectMapper.writeValueAsString(normalized));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to persist courses", e);
        }
        User saved = userRepository.save(user);
        return new StudentCoursesResponse(saved.getUserId(), deserializeCourses(saved.getCourses()));
    }

    private static List<String> normalizeCourseCodes(List<String> rawCourseCodes) {
        List<String> result = new ArrayList<>();
        Set<String> seenUpper = new LinkedHashSet<>();
        for (String raw : rawCourseCodes) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String dedupeKey = trimmed.toUpperCase();
            if (seenUpper.add(dedupeKey)) {
                result.add(trimmed);
            }
        }
        return List.copyOf(result);
    }

    private List<String> deserializeCourses(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        String trimmed = stored.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed =
                        objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                return normalizeCourseCodes(parsed);
            } catch (JsonProcessingException ignored) {
                // fall through to legacy formats
            }
        }
        return legacyCommaSeparated(trimmed);
    }

    private static List<String> legacyCommaSeparated(String stored) {
        List<String> parts = new ArrayList<>();
        for (String piece : stored.split(",")) {
            parts.add(piece);
        }
        return normalizeCourseCodes(parts);
    }
}
