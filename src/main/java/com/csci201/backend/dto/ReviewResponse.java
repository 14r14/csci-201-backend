package com.csci201.backend.dto;

import java.time.Instant;

/** Response payload for a single review. Returned by both POST and GET endpoints. */
public class ReviewResponse {

    private Long reviewId;
    private Long userId;
    private String userName;
    private Long roomId;
    private Integer rating;
    private Integer noiseRating;
    private Integer cleanlinessRating;
    private Integer amenitiesRating;
    private String comment;
    private Instant createdTimestamp;

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

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

    public Instant getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(Instant createdTimestamp) { this.createdTimestamp = createdTimestamp; }
}
