package com.csci201.backend.service;

import com.csci201.backend.dto.MatchSuggestionResponse;
import com.csci201.backend.entity.User;
import com.csci201.backend.entity.UserMatch;
import com.csci201.backend.entity.enums.UserRole;
import com.csci201.backend.repository.UserMatchRepository;
import com.csci201.backend.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchingService {

    private final UserRepository userRepository;
    private final UserMatchRepository userMatchRepository;
    private final ObjectMapper objectMapper;

    public MatchingService(UserRepository userRepository, UserMatchRepository userMatchRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userMatchRepository = userMatchRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<MatchSuggestionResponse> getSuggestions(Long userId, Integer limit) {
        User user = userRepository.findById(Objects.requireNonNull(userId, "userId is required"))
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        List<User> candidates = userRepository.findByRoleAndUserIdNot(UserRole.STUDENT, user.getUserId());
        List<MatchSuggestionResponse> responses = new ArrayList<>();

        for (User candidate : candidates) {
            double score = compatibilityScore(user, candidate);
            String sharedCourses = sharedCourses(user, candidate);
            UserMatch userMatch = userMatchRepository
                    .findByUserUserIdAndMatchedUserUserId(user.getUserId(), candidate.getUserId())
                    .orElseGet(UserMatch::new);
            userMatch.setUser(user);
            userMatch.setMatchedUser(candidate);
            userMatch.setCompatibilityScore(score);
            userMatch.setSharedCourses(sharedCourses);
            userMatchRepository.save(userMatch);
            responses.add(toResponse(candidate, sharedCourses, score));
        }

        int boundedLimit = limit == null ? 10 : Math.max(1, limit);
        return responses.stream()
                .sorted((a, b) -> Double.compare(b.getCompatibilityScore(), a.getCompatibilityScore()))
                .limit(boundedLimit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchSuggestionResponse> searchMatches(
            Long userId, String course, String interest, Double minScore, Integer page, Integer size) {
        Objects.requireNonNull(userId, "userId is required");
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }

        List<UserMatch> matches = userMatchRepository.searchMatches(userId, emptyToNull(course), minScore);
        List<MatchSuggestionResponse> filtered = matches.stream()
                .filter(match -> interest == null
                        || interest.isBlank()
                        || containsToken(match.getMatchedUser().getSocialPreferences(), interest)
                        || containsToken(match.getMatchedUser().getUserName(), interest)
                        || containsToken(match.getMatchedUser().getFirstName(), interest)
                        || containsToken(match.getMatchedUser().getLastName(), interest))
                .map(match -> toResponse(match.getMatchedUser(), match.getSharedCourses(), match.getCompatibilityScore()))
                .collect(Collectors.toList());

        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 20 : Math.max(1, size);
        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        return filtered.subList(fromIndex, toIndex);
    }

    private MatchSuggestionResponse toResponse(User matchedUser, String sharedCourses, double score) {
        MatchSuggestionResponse response = new MatchSuggestionResponse();
        response.setUserId(matchedUser.getUserId());
        response.setUserName(matchedUser.getUserName());
        response.setFirstName(matchedUser.getFirstName());
        response.setLastName(matchedUser.getLastName());
        response.setSharedCourses(sharedCourses);
        response.setCompatibilityScore(score);
        return response;
    }

    private double compatibilityScore(User source, User target) {
        Set<String> sourceCourses = tokenizedSet(source.getCourses());
        Set<String> targetCourses = tokenizedSet(target.getCourses());
        Set<String> sourcePrefs = tokenizedSet(source.getSocialPreferences());
        Set<String> targetPrefs = tokenizedSet(target.getSocialPreferences());

        long sharedCourseCount = sourceCourses.stream().filter(targetCourses::contains).count();
        long sharedPreferenceCount = sourcePrefs.stream().filter(targetPrefs::contains).count();

        return (sharedCourseCount * 0.7) + (sharedPreferenceCount * 0.3);
    }

    private String sharedCourses(User source, User target) {
        Set<String> sourceCourses = tokenizedSet(source.getCourses());
        Set<String> targetCourses = tokenizedSet(target.getCourses());
        Set<String> shared = new LinkedHashSet<>();
        for (String course : sourceCourses) {
            if (targetCourses.contains(course)) {
                shared.add(course);
            }
        }
        return String.join(", ", shared);
    }

    private Set<String> tokenizedSet(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                return parsed.stream()
                        .filter(v -> v != null && !v.isBlank())
                        .map(v -> v.toLowerCase(Locale.ROOT).trim())
                        .filter(v -> !v.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            } catch (JsonProcessingException ignored) {
                // fall through to comma-split
            }
        }
        return Arrays.stream(trimmed.split("[,;]"))
                .map(v -> v.toLowerCase(Locale.ROOT).trim())
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private boolean containsToken(String text, String token) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }
}
