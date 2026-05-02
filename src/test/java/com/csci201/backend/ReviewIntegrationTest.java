package com.csci201.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.csci201.backend.dto.CreateReviewRequest;
import com.csci201.backend.dto.ReviewResponse;
import com.csci201.backend.dto.UpdateReviewRequest;
import com.csci201.backend.entity.Room;
import com.csci201.backend.repository.RoomRepository;
import com.csci201.backend.service.RatingRecalculationService;
import com.csci201.backend.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the review / rating system.
 *
 * Runs against the in-memory H2 database (profile "h2") so no MySQL is needed.
 * All test data comes from V2__seed_data.sql (users alice=1, bob=2; room SAL_101=1).
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class ReviewIntegrationTest {

    @Autowired ReviewService               reviewService;
    @Autowired RatingRecalculationService  recalcService;
    @Autowired RoomRepository             roomRepository;

    // Seed IDs from V2__seed_data.sql
    private static final Long ALICE   = 1L;
    private static final Long BOB     = 2L;
    private static final Long SAL_101 = 1L;

    // ── submitReview ──────────────────────────────────────────────────────────

    @Test
    void submitReview_authenticated_success() {
        ReviewResponse response = reviewService.submitReview(buildCreate(ALICE, SAL_101, 4, 3, 5, 4, "Great room"), false);

        assertThat(response.getReviewId()).isNotNull();
        assertThat(response.getUserId()).isEqualTo(ALICE);
        assertThat(response.getRoomId()).isEqualTo(SAL_101);
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getNoiseRating()).isEqualTo(3);
        assertThat(response.getCleanlinessRating()).isEqualTo(5);
        assertThat(response.getAmenitiesRating()).isEqualTo(4);
        assertThat(response.getComment()).isEqualTo("Great room");
        assertThat(response.getCreatedTimestamp()).isNotNull();
    }

    @Test
    void submitReview_updatesRoomOverallAverage() {
        reviewService.submitReview(buildCreate(ALICE, SAL_101, 4, null, null, null, null), false);
        reviewService.submitReview(buildCreate(BOB,   SAL_101, 2, null, null, null, null), false);

        Room room = roomRepository.findById(SAL_101).orElseThrow();
        assertThat(room.getRatingsCount()).isEqualTo(2);
        assertThat(room.getAverageRating()).isEqualTo(3.0); // (4+2)/2
    }

    @Test
    void submitReview_updatesSubcategoryAverages() {
        reviewService.submitReview(buildCreate(ALICE, SAL_101, 5, 4, 3, 5, null), false);

        Room room = roomRepository.findById(SAL_101).orElseThrow();
        assertThat(room.getAvgNoiseRating()).isEqualTo(4.0);
        assertThat(room.getAvgCleanlinessRating()).isEqualTo(3.0);
        assertThat(room.getAvgAmenitiesRating()).isEqualTo(5.0);
    }

    @Test
    void submitReview_guestUser_throws() {
        assertThatThrownBy(() -> reviewService.submitReview(buildCreate(ALICE, SAL_101, 5, null, null, null, null), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Guests cannot submit a review");
    }

    @Test
    void submitReview_duplicateReview_throws() {
        reviewService.submitReview(buildCreate(ALICE, SAL_101, 4, null, null, null, null), false);

        assertThatThrownBy(() -> reviewService.submitReview(buildCreate(ALICE, SAL_101, 5, null, null, null, null), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reviewed");
    }

    // ── updateReview ──────────────────────────────────────────────────────────

    @Test
    void updateReview_authenticated_success() {
        ReviewResponse created = reviewService.submitReview(buildCreate(ALICE, SAL_101, 3, null, null, null, "OK"), false);

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setUserId(ALICE);
        req.setRating(5);
        req.setNoiseRating(4);
        req.setCleanlinessRating(5);
        req.setComment("Actually great!");

        ReviewResponse updated = reviewService.updateReview(created.getReviewId(), req, false);

        assertThat(updated.getRating()).isEqualTo(5);
        assertThat(updated.getNoiseRating()).isEqualTo(4);
        assertThat(updated.getCleanlinessRating()).isEqualTo(5);
        assertThat(updated.getComment()).isEqualTo("Actually great!");
    }

    @Test
    void updateReview_adjustsOverallAverage() {
        // Alice submits rating=2, Bob submits rating=4 → avg=3.0
        ReviewResponse aliceReview = reviewService.submitReview(buildCreate(ALICE, SAL_101, 2, null, null, null, null), false);
        reviewService.submitReview(buildCreate(BOB, SAL_101, 4, null, null, null, null), false);

        // Alice edits to rating=4 → avg should be (4+4)/2 = 4.0
        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setUserId(ALICE);
        req.setRating(4);
        reviewService.updateReview(aliceReview.getReviewId(), req, false);

        Room room = roomRepository.findById(SAL_101).orElseThrow();
        assertThat(room.getAverageRating()).isEqualTo(4.0);
    }

    @Test
    void updateReview_wrongUser_throws() {
        ReviewResponse created = reviewService.submitReview(buildCreate(ALICE, SAL_101, 3, null, null, null, null), false);

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setUserId(BOB);  // Bob trying to edit Alice's review
        req.setRating(1);

        assertThatThrownBy(() -> reviewService.updateReview(created.getReviewId(), req, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only edit your own");
    }

    @Test
    void updateReview_guest_throws() {
        ReviewResponse created = reviewService.submitReview(buildCreate(ALICE, SAL_101, 3, null, null, null, null), false);

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setUserId(ALICE);
        req.setRating(5);

        assertThatThrownBy(() -> reviewService.updateReview(created.getReviewId(), req, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Guests cannot edit a review");
    }

    // ── getReviewsForRoom ─────────────────────────────────────────────────────

    @Test
    void getReviewsForRoom_emptyWhenNone() {
        assertThat(reviewService.getReviewsForRoom(SAL_101)).isEmpty();
    }

    @Test
    void getReviewsForRoom_returnsNewestFirst() {
        reviewService.submitReview(buildCreate(ALICE, SAL_101, 3, null, null, null, "first"), false);
        reviewService.submitReview(buildCreate(BOB,   SAL_101, 5, null, null, null, "second"), false);

        var reviews = reviewService.getReviewsForRoom(SAL_101);
        assertThat(reviews).hasSize(2);
        assertThat(reviews.get(0).getComment()).isEqualTo("second"); // newest first
    }

    @Test
    void getReviewsForRoom_allowedForGuests() {
        // GET does not take an isGuest flag — guests can always read
        reviewService.submitReview(buildCreate(ALICE, SAL_101, 4, null, null, null, null), false);
        assertThat(reviewService.getReviewsForRoom(SAL_101)).hasSize(1);
    }

    // ── Scheduled recalculation ───────────────────────────────────────────────

    @Test
    void recalculateAllRatings_correctsAverages() {
        // Submit two reviews with full subcategories
        reviewService.submitReview(buildCreate(ALICE, SAL_101, 4, 3, 5, 4, null), false);
        reviewService.submitReview(buildCreate(BOB,   SAL_101, 2, 5, 3, 2, null), false);

        // Tamper with the stored values to simulate drift
        Room room = roomRepository.findById(SAL_101).orElseThrow();
        room.setAverageRating(99.0);
        room.setAvgNoiseRating(99.0);
        roomRepository.save(room);

        // Run the recalculation job
        recalcService.recalculateAllRatings();

        Room corrected = roomRepository.findById(SAL_101).orElseThrow();
        assertThat(corrected.getAverageRating()).isEqualTo(3.0);       // (4+2)/2
        assertThat(corrected.getAvgNoiseRating()).isEqualTo(4.0);      // (3+5)/2
        assertThat(corrected.getAvgCleanlinessRating()).isEqualTo(4.0); // (5+3)/2
        assertThat(corrected.getAvgAmenitiesRating()).isEqualTo(3.0);  // (4+2)/2
        assertThat(corrected.getRatingsCount()).isEqualTo(2);
    }

    @Test
    void recalculateAllRatings_roomWithNoReviews_resetsToZero() {
        // Manually corrupt a room that has no reviews
        Room room = roomRepository.findById(SAL_101).orElseThrow();
        room.setAverageRating(3.5);
        room.setRatingsCount(5);
        roomRepository.save(room);

        recalcService.recalculateAllRatings();

        Room reset = roomRepository.findById(SAL_101).orElseThrow();
        assertThat(reset.getAverageRating()).isEqualTo(0.0);
        assertThat(reset.getRatingsCount()).isEqualTo(0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreateReviewRequest buildCreate(Long userId, Long roomId, int rating,
                                            Integer noise, Integer clean, Integer amenities,
                                            String comment) {
        CreateReviewRequest r = new CreateReviewRequest();
        r.setUserId(userId);
        r.setRoomId(roomId);
        r.setRating(rating);
        r.setNoiseRating(noise);
        r.setCleanlinessRating(clean);
        r.setAmenitiesRating(amenities);
        r.setComment(comment);
        return r;
    }
}
