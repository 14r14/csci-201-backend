package com.csci201.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Request body for PUT /api/reviews/{id}. Only the review's original author may call this. */
public class UpdateReviewRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Min(value = 1, message = "noiseRating must be between 1 and 5")
    @Max(value = 5, message = "noiseRating must be between 1 and 5")
    private Integer noiseRating;

    @Min(value = 1, message = "cleanlinessRating must be between 1 and 5")
    @Max(value = 5, message = "cleanlinessRating must be between 1 and 5")
    private Integer cleanlinessRating;

    @Min(value = 1, message = "amenitiesRating must be between 1 and 5")
    @Max(value = 5, message = "amenitiesRating must be between 1 and 5")
    private Integer amenitiesRating;

    private String comment;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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
