package com.csci201.backend.repository;

import com.csci201.backend.entity.WaitlistEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByRoom_RoomIdAndRequestedTimeSlotOrderByQueuePositionAsc(
            Long roomId,
            String requestedTimeSlot
    );

    Optional<WaitlistEntry> findFirstByRoom_RoomIdAndRequestedTimeSlotOrderByQueuePositionAsc(
            Long roomId,
            String requestedTimeSlot
    );

    boolean existsByUser_UserIdAndRoom_RoomIdAndRequestedTimeSlot(
            Long userId,
            Long roomId,
            String requestedTimeSlot
    );

    long countByRoom_RoomIdAndRequestedTimeSlot(
            Long roomId,
            String requestedTimeSlot
    );

    List<WaitlistEntry> findByUser_UserIdOrderByCreatedTimestampDesc(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE WaitlistEntry w SET w.queuePosition = w.queuePosition - 1 " +
           "WHERE w.room.roomId = :roomId AND w.requestedTimeSlot = :timeSlot " +
           "AND w.queuePosition > :position")
    void decrementPositionsAfter(
            @Param("roomId") Long roomId,
            @Param("timeSlot") String timeSlot,
            @Param("position") Integer position
    );
}