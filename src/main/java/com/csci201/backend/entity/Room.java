package com.csci201.backend.entity;

import com.csci201.backend.entity.enums.RoomCurrentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Represents a bookable study room.
 *
 * Relationships:
 *   Room →(has many)→ Reservation
 *   Room →(has many)→ Review
 *
 * Rating columns (overall + subcategory) are kept denormalized here for fast
 * reads.  They are updated incrementally on every review write, and
 * authoritatively recalculated by the nightly scheduled job in
 * {@link com.csci201.backend.service.RatingRecalculationService}.
 */
@Entity
@Table(
        name = "rooms",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_rooms_building_room", columnNames = {"building_name", "room_number"}))
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "building_name", nullable = false, length = 255)
    private String buildingName;

    @Column(name = "room_number", nullable = false, length = 64)
    private String roomNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "feature_list", columnDefinition = "TEXT")
    private String featureList;

    @Column(name = "map_location", length = 512)
    private String mapLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 32)
    private RoomCurrentStatus currentStatus;

    // ── Overall rating aggregates ─────────────────────────────────────────────

    /** Average of the overall 1-5 ratings across all reviews. */
    @Column(name = "average_rating", nullable = false)
    private Double averageRating = 0.0;

    /** Total number of reviews left for this room. */
    @Column(name = "ratings_count", nullable = false)
    private Integer ratingsCount = 0;

    // ── Subcategory averages (recalculated by RatingRecalculationService) ─────

    /** Average noise rating (1-5) across reviews that supplied one. 0 if none. */
    @Column(name = "avg_noise_rating", nullable = false)
    private Double avgNoiseRating = 0.0;

    /** Average cleanliness rating (1-5). 0 if no reviews supplied one. */
    @Column(name = "avg_cleanliness_rating", nullable = false)
    private Double avgCleanlinessRating = 0.0;

    /** Average amenities/equipment rating (1-5). 0 if no reviews supplied one. */
    @Column(name = "avg_amenities_rating", nullable = false)
    private Double avgAmenitiesRating = 0.0;

    // ── getters / setters ─────────────────────────────────────────────────────

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getFeatureList() { return featureList; }
    public void setFeatureList(String featureList) { this.featureList = featureList; }

    public String getMapLocation() { return mapLocation; }
    public void setMapLocation(String mapLocation) { this.mapLocation = mapLocation; }

    public RoomCurrentStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(RoomCurrentStatus currentStatus) { this.currentStatus = currentStatus; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }

    public Double getAvgNoiseRating() { return avgNoiseRating; }
    public void setAvgNoiseRating(Double avgNoiseRating) { this.avgNoiseRating = avgNoiseRating; }

    public Double getAvgCleanlinessRating() { return avgCleanlinessRating; }
    public void setAvgCleanlinessRating(Double avgCleanlinessRating) { this.avgCleanlinessRating = avgCleanlinessRating; }

    public Double getAvgAmenitiesRating() { return avgAmenitiesRating; }
    public void setAvgAmenitiesRating(Double avgAmenitiesRating) { this.avgAmenitiesRating = avgAmenitiesRating; }
}
