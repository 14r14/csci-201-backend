package com.csci201.backend.service;

import com.csci201.backend.dto.CreateReviewRequest;
import com.csci201.backend.dto.ReviewResponse;
import com.csci201.backend.dto.UpdateReviewRequest;
import com.csci201.backend.entity.Review;
import com.csci201.backend.entity.Room;
import com.csci201.backend.entity.User;
import com.csci201.backend.repository.ReviewRepository;
import com.csci201.backend.repository.RoomRepository;
import com.csci201.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the review / rating system.
 *
 * <h3>Guest enforcement</h3>
 * The project uses a stateless session model: when {@code /auth/guest} is called, the
 * server returns {@code AuthUserResponse.guest = true} and no database userId.  The
 * frontend stores this in sessionStorage and must forward {@code X-Is-Guest: true} on
 * every request.  ReviewController reads that header and passes {@code isGuest} here;
 * any write operation from a guest immediately throws an {@link IllegalArgumentException}
 * which GlobalExceptionHandler converts to HTTP 400.
 *
 * <h3>Rating update strategy</h3>
 * On every successful submit/update the overall {@code averageRating} and
 * {@code ratingsCount} on {@link Room} are updated <em>incrementally</em> so that
 * individual room fetches are always fast.  Subcategory averages use the same
 * incremental formula on submit; on update they are left for the nightly
 * {@link RatingRecalculationService} scheduled job to correct authoritatively,
 * since the incremental edit formula for optional subcategories is complex and
 * error-prone.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository   userRepository;
    private final RoomRepository   roomRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         RoomRepository roomRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository   = userRepository;
        this.roomRepository   = roomRepository;
    }

    // ── Read (guests allowed) ─────────────────────────────────────────────────

    /** Returns all reviews for a room, newest first. Open to guests and authenticated users. */
    public List<ReviewResponse> getReviewsForRoom(Long roomId) {
        return reviewRepository.findByRoom_RoomIdOrderByCreatedTimestampDesc(roomId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Write (authenticated users only) ──────────────────────────────────────

    /**
     * Submit a new review.
     *
     * Rules:
     *  - Guest users are rejected immediately.
     *  - One review per user per room; submitting again returns a clear error message.
     *  - Overall {@code averageRating} and {@code ratingsCount} on Room are updated
     *    incrementally.  Subcategory averages are also updated incrementally when provided.
     *
     * @param request validated request body
     * @param isGuest {@code true} when the caller is a guest session
     */
    @Transactional
    public ReviewResponse submitReview(CreateReviewRequest request, boolean isGuest) {
        rejectGuest(isGuest, "submit a review");

        User user = requireUser(request.getUserId());
        Room room = requireRoom(request.getRoomId());

        // Enforce one review per user per room
        if (reviewRepository.findByUser_UserIdAndRoom_RoomId(user.getUserId(), room.getRoomId()).isPresent()) {
            throw new IllegalArgumentException(
                    "You have already reviewed this room. Use PUT /api/reviews/{id} to update your existing review.");
        }

        Review review = new Review();
        review.setUser(user);
        review.setRoom(room);
        review.setRating(request.getRating());
        review.setNoiseRating(request.getNoiseRating());
        review.setCleanlinessRating(request.getCleanlinessRating());
        review.setAmenitiesRating(request.getAmenitiesRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);

        // Incremental overall average:  newAvg = (oldAvg * oldCount + newRating) / newCount
        int newCount = room.getRatingsCount() + 1;
        double newAvg = ((room.getAverageRating() * room.getRatingsCount()) + request.getRating()) / newCount;
        room.setRatingsCount(newCount);
        room.setAverageRating(round1(newAvg));

        // Incremental subcategory averages (only when provided)
        if (request.getNoiseRating() != null) {
            room.setAvgNoiseRating(round1(incrAvg(room.getAvgNoiseRating(), newCount - 1, request.getNoiseRating(), newCount)));
        }
        if (request.getCleanlinessRating() != null) {
            room.setAvgCleanlinessRating(round1(incrAvg(room.getAvgCleanlinessRating(), newCount - 1, request.getCleanlinessRating(), newCount)));
        }
        if (request.getAmenitiesRating() != null) {
            room.setAvgAmenitiesRating(round1(incrAvg(room.getAvgAmenitiesRating(), newCount - 1, request.getAmenitiesRating(), newCount)));
        }
        roomRepository.save(room);

        return toResponse(saved);
    }

    /**
     * Update an existing review.
     *
     * Rules:
     *  - Guest users are rejected.
     *  - Only the original author (matched by userId) may edit.
     *  - Overall rating average is adjusted incrementally.
     *  - Subcategory averages are NOT adjusted here; the nightly recalculation job
     *    will correct them.  This avoids complex logic around optional-field deltas.
     *
     * @param reviewId PK of the review to update
     * @param request  validated request body (must include userId for ownership check)
     * @param isGuest  {@code true} when the caller is a guest session
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, boolean isGuest) {
        rejectGuest(isGuest, "edit a review");

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));

        if (!review.getUser().getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("You can only edit your own reviews.");
        }

        Room room = review.getRoom();
        int oldRating = review.getRating();

        review.setRating(request.getRating());
        review.setNoiseRating(request.getNoiseRating());
        review.setCleanlinessRating(request.getCleanlinessRating());
        review.setAmenitiesRating(request.getAmenitiesRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);

        // Adjust overall average: newAvg = (oldAvg * count - oldRating + newRating) / count
        if (room.getRatingsCount() > 0) {
            double adjusted = (room.getAverageRating() * room.getRatingsCount() - oldRating + request.getRating())
                    / room.getRatingsCount();
            room.setAverageRating(round1(adjusted));
            roomRepository.save(room);
        }

        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Throws 400 if the caller is a guest. */
    private static void rejectGuest(boolean isGuest, String action) {
        if (isGuest) {
            throw new IllegalArgumentException(
                    "Guests cannot " + action + ". Please log in or create an account to continue.");
        }
    }

    /** Incremental running average formula. */
    private static double incrAvg(double currentAvg, int oldCount, int newValue, int newCount) {
        return (currentAvg * oldCount + newValue) / newCount;
    }

    /** Round to one decimal place. */
    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private Room requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
    }

    ReviewResponse toResponse(Review review) {
        ReviewResponse r = new ReviewResponse();
        r.setReviewId(review.getReviewId());
        r.setUserId(review.getUser().getUserId());
        r.setUserName(review.getUser().getUserName());
        r.setRoomId(review.getRoom().getRoomId());
        r.setRating(review.getRating());
        r.setNoiseRating(review.getNoiseRating());
        r.setCleanlinessRating(review.getCleanlinessRating());
        r.setAmenitiesRating(review.getAmenitiesRating());
        r.setComment(review.getComment());
        r.setCreatedTimestamp(review.getCreatedTimestamp());
        return r;
    }
}
