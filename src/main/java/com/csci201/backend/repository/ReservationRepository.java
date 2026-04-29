package com.csci201.backend.repository;

import com.csci201.backend.entity.Reservation;
import com.csci201.backend.entity.enums.ReservationStatus;
import java.time.Instant;
import java.util.List;
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

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.user.userId = :userId AND r.status = :status AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsOverlappingReservationForUser(
            @Param("userId") Long userId,
            @Param("status") ReservationStatus status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    List<Reservation> findByUser_UserIdAndRoom_BuildingNameAndStatus(
            Long userId,
            String buildingName,
            ReservationStatus status);

    boolean existsByRoom_RoomIdAndStartTimeAndStatus(
            Long roomId,
            Instant startTime,
            ReservationStatus status
    );

    List<Reservation> findByUser_UserIdOrderByStartTimeDesc(Long userId);
}
