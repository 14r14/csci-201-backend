package com.csci201.backend.repository;

import com.csci201.backend.entity.WaitlistEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}