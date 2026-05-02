package com.csci201.backend.service;

import com.csci201.backend.entity.Room;
import com.csci201.backend.repository.ReviewRepository;
import com.csci201.backend.repository.RoomRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service that authoritatively recalculates and stores all room rating
 * aggregates from the reviews table.
 *
 * <h3>Why a scheduler?</h3>
 * Incremental updates in {@link ReviewService} are fast but accumulate floating-point
 * rounding error over time.  This job runs nightly and recomputes everything from raw
 * data, so the stored averages are always accurate.  It also handles the subcategory
 * averages after review edits (where the service skips the incremental subcategory
 * adjustment to avoid complex optional-field delta logic).
 *
 * <h3>Schedule</h3>
 * Default: every night at 02:00 server time (cron {@code "0 0 2 * * *"}).
 * Override via the property {@code app.rating-recalc.cron} in application.yml if needed.
 *
 * <h3>Algorithm</h3>
 * {@code averageRating = SUM(rating) / COUNT(*)}  — matches the spec.
 * Subcategory averages only count rows where that subcategory was provided (NULL rows
 * are excluded by SQL AVG automatically).  If no reviews supplied a subcategory, the
 * average stays at 0.0.
 * Rooms with zero reviews are reset to all-zero averages.
 */
@Service
public class RatingRecalculationService {

    private static final Logger log = LoggerFactory.getLogger(RatingRecalculationService.class);

    private final ReviewRepository reviewRepository;
    private final RoomRepository   roomRepository;

    public RatingRecalculationService(ReviewRepository reviewRepository,
                                      RoomRepository roomRepository) {
        this.reviewRepository = reviewRepository;
        this.roomRepository   = roomRepository;
    }

    /**
     * Nightly recalculation job.
     *
     * Cron expression: second minute hour day month weekday
     * "0 0 2 * * *" = every day at 02:00:00 server time.
     */
    @Scheduled(cron = "${app.rating-recalc.cron:0 0 2 * * *}")
    @Transactional
    public void recalculateAllRatings() {
        log.info("Rating recalculation job started");
        long start = System.currentTimeMillis();

        // Fetch aggregate stats grouped by room
        List<Object[]> rows = reviewRepository.aggregateRatingsByRoom();

        // Build a lookup map: roomId → aggregate row
        Map<Long, Object[]> statsByRoom = new HashMap<>();
        for (Object[] row : rows) {
            statsByRoom.put((Long) row[0], row);
        }

        List<Room> allRooms = roomRepository.findAll();
        int updated = 0;

        for (Room room : allRooms) {
            Object[] stats = statsByRoom.get(room.getRoomId());

            if (stats == null) {
                // No reviews at all — reset to zero
                if (room.getRatingsCount() != 0 || room.getAverageRating() != 0.0) {
                    room.setRatingsCount(0);
                    room.setAverageRating(0.0);
                    room.setAvgNoiseRating(0.0);
                    room.setAvgCleanlinessRating(0.0);
                    room.setAvgAmenitiesRating(0.0);
                    roomRepository.save(room);
                    updated++;
                }
            } else {
                long   count         = ((Number) stats[1]).longValue();
                double avgOverall    = nullToZero((Double) stats[2]);
                double avgNoise      = nullToZero((Double) stats[3]);
                double avgClean      = nullToZero((Double) stats[4]);
                double avgAmenities  = nullToZero((Double) stats[5]);

                room.setRatingsCount((int) count);
                room.setAverageRating(round1(avgOverall));
                room.setAvgNoiseRating(round1(avgNoise));
                room.setAvgCleanlinessRating(round1(avgClean));
                room.setAvgAmenitiesRating(round1(avgAmenities));
                roomRepository.save(room);
                updated++;
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Rating recalculation job finished: {} rooms updated in {}ms", updated, elapsed);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
