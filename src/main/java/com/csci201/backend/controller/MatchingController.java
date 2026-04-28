package com.csci201.backend.controller;

import com.csci201.backend.dto.MatchSuggestionResponse;
import com.csci201.backend.service.MatchingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matches")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/suggestions")
    public List<MatchSuggestionResponse> getSuggestions(
            @RequestParam Long userId, @RequestParam(required = false) Integer limit) {
        return matchingService.getSuggestions(userId, limit);
    }

    @GetMapping("/search")
    public List<MatchSuggestionResponse> searchMatches(
            @RequestParam Long userId,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String interest,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return matchingService.searchMatches(userId, course, interest, minScore, page, size);
    }
}
