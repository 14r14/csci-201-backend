package com.csci201.backend.repository;

import com.csci201.backend.entity.Reservation;
import com.csci201.backend.entity.enums.ReservationStatus;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByRoom_RoomIdAndStartTimeAndStatus(
            Long roomId,
            Instant startTime,
            ReservationStatus status
    );
}