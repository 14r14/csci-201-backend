package com.csci201.backend.repository;

import com.csci201.backend.entity.Reservation;
import com.csci201.backend.entity.enums.ReservationStatus;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.room.roomId = :roomId AND r.status = :status AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsOverlappingReservation(
            @Param("roomId") Long roomId,
            @Param("status") ReservationStatus status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
