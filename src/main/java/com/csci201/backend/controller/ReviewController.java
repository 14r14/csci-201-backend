package com.csci201.backend.controller;

import com.csci201.backend.dto.CreateReviewRequest;
import com.csci201.backend.dto.ReviewResponse;
import com.csci201.backend.dto.UpdateReviewRequest;
import com.csci201.backend.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the review / rating system.
 *
 * <h3>Guest enforcement</h3>
 * The app uses stateless session-based auth: no JWT, no Spring Security principal.
 * When {@code POST /auth/guest} is called, the server responds with
 * {@code AuthUserResponse.guest = true} and no database userId.  The frontend
 * stores this in sessionStorage and must send the header {@code X-Is-Guest: true}
 * on every subsequent request.
 *
 * This controller reads that header on write endpoints and passes the boolean to
 * {@link ReviewService}, which throws an {@link IllegalArgumentException} for guests.
 * GlobalExceptionHandler converts that to HTTP 400.
 *
 * Reads ({@code GET /api/reviews}) are open to guests and authenticated users alike.
 *
 * <h3>Endpoints</h3>
 * <pre>
 *   POST   /api/reviews                 — submit a new review (auth only)
 *   PUT    /api/reviews/{reviewId}      — edit your own review (auth only)
 *   GET    /api/reviews?roomId={id}     — list reviews for a room (everyone)
 * </pre>
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Submit a new review for a room.
     *
     * @param isGuest   Value of the {@code X-Is-Guest} header. Defaults to {@code false}
     *                  so authenticated callers that omit the header still work.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader(value = "X-Is-Guest", defaultValue = "false") boolean isGuest) {

        return reviewService.submitReview(request, isGuest);
    }

    /**
     * Update an existing review. Only the original author may call this.
     *
     * @param reviewId  PK of the review to update
     * @param isGuest   Value of the {@code X-Is-Guest} header
     */
    @PutMapping("/{reviewId}")
    public ReviewResponse updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            @RequestHeader(value = "X-Is-Guest", defaultValue = "false") boolean isGuest) {

        return reviewService.updateReview(reviewId, request, isGuest);
    }

    /**
     * List all reviews for a given room, newest first. Open to guests and authenticated users.
     *
     * @param roomId  ID of the room whose reviews to retrieve
     */
    @GetMapping
    public List<ReviewResponse> getReviewsForRoom(@RequestParam Long roomId) {
        return reviewService.getReviewsForRoom(roomId);
    }
}
