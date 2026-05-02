package com.csci201.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/reviews.
 *
 * Only authenticated users may submit — the controller reads X-Is-Guest
 * and rejects the request before this DTO is even processed.
 */
public class CreateReviewRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "roomId is required")
    private Long roomId;

    /** Overall 1-5 rating (required). */
    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    /** Subcategory: noise level (optional, 1 = very noisy, 5 = very quiet). */
    @Min(value = 1, message = "noiseRating must be between 1 and 5")
    @Max(value = 5, message = "noiseRating must be between 1 and 5")
    private Integer noiseRating;

    /** Subcategory: cleanliness (optional, 1 = dirty, 5 = spotless). */
    @Min(value = 1, message = "cleanlinessRating must be between 1 and 5")
    @Max(value = 5, message = "cleanlinessRating must be between 1 and 5")
    private Integer cleanlinessRating;

    /** Subcategory: amenities / equipment quality (optional, 1 = poor, 5 = excellent). */
    @Min(value = 1, message = "amenitiesRating must be between 1 and 5")
    @Max(value = 5, message = "amenitiesRating must be between 1 and 5")
    private Integer amenitiesRating;

    private String comment;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Integer getNoiseRating() { return noiseRating; }
    public void setNoiseRating(Integer noiseRating) { this.noiseRating = noiseRating; }

    public Integer getCleanlinessRating() { return cleanlinessRating; }
    public void setCleanlinessRating(Integer cleanlinessRating) { this.cleanlinessRating = cleanlinessRating; }

    public Integer getAmenitiesRating() { return amenitiesRating; }
    public void setAmenitiesRating(Integer amenitiesRating) { this.amenitiesRating = amenitiesRating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
