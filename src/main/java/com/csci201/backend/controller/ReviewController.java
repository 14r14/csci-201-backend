package com.csci201.backend.controller;

import com.csci201.backend.dto.CreateReviewRequest;
import com.csci201.backend.dto.ReviewResponse;
import com.csci201.backend.entity.Review;
import com.csci201.backend.entity.Room;
import com.csci201.backend.entity.User;
import com.csci201.backend.repository.ReviewRepository;
import com.csci201.backend.repository.RoomRepository;
import com.csci201.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public ReviewController(ReviewRepository reviewRepository, UserRepository userRepository, RoomRepository roomRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ReviewResponse createReview(@Valid @RequestBody CreateReviewRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + request.getRoomId()));

        Review review = new Review();
        review.setUser(user);
        review.setRoom(room);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);

        // Recompute average rating incrementally
        int newCount = room.getRatingsCount() + 1;
        double newAvg = ((room.getAverageRating() * room.getRatingsCount()) + request.getRating()) / newCount;
        room.setRatingsCount(newCount);
        room.setAverageRating(Math.round(newAvg * 10.0) / 10.0);
        roomRepository.save(room);

        return toResponse(saved);
    }

    @GetMapping
    public List<ReviewResponse> getReviewsForRoom(@RequestParam Long roomId) {
        return reviewRepository.findByRoom_RoomIdOrderByCreatedTimestampDesc(roomId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse toResponse(Review review) {
        ReviewResponse r = new ReviewResponse();
        r.setReviewId(review.getReviewId());
        r.setUserId(review.getUser().getUserId());
        r.setUserName(review.getUser().getUserName());
        r.setRoomId(review.getRoom().getRoomId());
        r.setRating(review.getRating());
        r.setComment(review.getComment());
        r.setCreatedTimestamp(review.getCreatedTimestamp());
        return r;
    }
}
