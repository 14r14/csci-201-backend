package com.csci201.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Represents a room review left by an authenticated user.
 *
 * Relationships:
 *   User  →(has many)→ Review
 *   Room  →(has many)→ Review
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /** Overall 1-5 star rating (required). */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    /** Subcategory: how quiet/noisy the room is (1 = very noisy, 5 = very quiet). Optional. */
    @Column(name = "noise_rating")
    private Integer noiseRating;

    /** Subcategory: cleanliness of the room (1 = dirty, 5 = spotless). Optional. */
    @Column(name = "cleanliness_rating")
    private Integer cleanlinessRating;

    /** Subcategory: quality of equipment/amenities (1 = poor, 5 = excellent). Optional. */
    @Column(name = "amenities_rating")
    private Integer amenitiesRating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private Instant createdTimestamp;

    // ── getters / setters ─────────────────────────────────────────────────────

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

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
}
