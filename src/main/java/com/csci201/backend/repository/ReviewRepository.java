package com.csci201.backend.repository;

import com.csci201.backend.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRoom_RoomIdOrderByCreatedTimestampDesc(Long roomId);
}
