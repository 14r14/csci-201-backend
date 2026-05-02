package com.csci201.backend.repository;

import com.csci201.backend.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRoom_RoomIdOrderByCreatedTimestampDesc(Long roomId);

    Optional<Review> findByUser_UserIdAndRoom_RoomId(Long userId, Long roomId);

    /**
     * Aggregate query used by {@link com.csci201.backend.service.RatingRecalculationService}.
     *
     * Returns one row per room that has at least one review:
     *   [0] roomId          (Long)
     *   [1] reviewCount     (Long)
     *   [2] avgOverall      (Double)
     *   [3] avgNoise        (Double, null when no row supplied noiseRating)
     *   [4] avgCleanliness  (Double, null when no row supplied cleanlinessRating)
     *   [5] avgAmenities    (Double, null when no row supplied amenitiesRating)
     */
    @Query("""
            SELECT r.room.roomId,
                   COUNT(r),
                   AVG(r.rating),
                   AVG(r.noiseRating),
                   AVG(r.cleanlinessRating),
                   AVG(r.amenitiesRating)
            FROM Review r
            GROUP BY r.room.roomId
            """)
    List<Object[]> aggregateRatingsByRoom();
}
