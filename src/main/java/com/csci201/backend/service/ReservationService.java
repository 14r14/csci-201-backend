package com.csci201.backend.service;

import com.csci201.backend.dto.BookRoomRequest;
import com.csci201.backend.dto.ReservationResponse;
import com.csci201.backend.entity.Reservation;
import com.csci201.backend.entity.Room;
import com.csci201.backend.entity.User;
import com.csci201.backend.entity.enums.ReservationStatus;
import com.csci201.backend.exception.RoomNotAvailableException;
import com.csci201.backend.repository.ReservationRepository;
import com.csci201.backend.repository.RoomRepository;
import com.csci201.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final WaitlistService waitlistService;

    public ReservationService(ReservationRepository reservationRepository,
                              RoomRepository roomRepository,
                              UserRepository userRepository,
                              @Lazy WaitlistService waitlistService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.waitlistService = waitlistService;
    }

    @Transactional
    public ReservationResponse bookRoom(BookRoomRequest request) {
        Long userId = Objects.requireNonNull(request.getUserId(), "userId is required");
        Long roomId = Objects.requireNonNull(request.getRoomId(), "roomId is required");

        User user = requireUser(userId);
        Room room = requireRoom(roomId);
        Reservation reservation = createConfirmedReservation(user, room, request.getStartTime(), request.getEndTime());
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("Reservation is already cancelled.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation = reservationRepository.save(reservation);

        waitlistService.promoteNextUserIfAvailable(reservation);

        return toResponse(reservation);
    }

    public List<ReservationResponse> getReservationsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        return reservationRepository.findByUser_UserIdOrderByStartTimeDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public User requireUser(Long userId) {
        return userRepository.findById(Objects.requireNonNull(userId, "userId is required"))
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    public Room requireRoom(Long roomId) {
        return roomRepository.findById(Objects.requireNonNull(roomId, "roomId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
    }

    public Reservation createConfirmedReservation(User user, Room room, java.time.Instant startTime, java.time.Instant endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        if (reservationRepository.existsOverlappingReservation(room.getRoomId(), ReservationStatus.CONFIRMED, startTime, endTime)) {
            throw new RoomNotAvailableException("Room is already booked for the requested time slot");
        }
        if (reservationRepository.existsOverlappingReservationForUser(user.getUserId(), ReservationStatus.CONFIRMED, startTime, endTime)) {
            throw new IllegalArgumentException("You already have a reservation during this time slot.");
        }
        long newDurationSeconds = endTime.getEpochSecond() - startTime.getEpochSecond();
        long existingSeconds = reservationRepository
                .findByUser_UserIdAndRoom_BuildingNameAndStatus(user.getUserId(), room.getBuildingName(), ReservationStatus.CONFIRMED)
                .stream()
                .mapToLong(r -> r.getEndTime().getEpochSecond() - r.getStartTime().getEpochSecond())
                .sum();
        if (existingSeconds + newDurationSeconds > 7200) {
            throw new IllegalArgumentException(
                    "You cannot reserve more than 2 hours total in " + room.getBuildingName() + ".");
        }
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    public ReservationResponse toResponse(Reservation r) {
        ReservationResponse response = new ReservationResponse();
        response.setReservationId(r.getReservationId());
        response.setUserId(r.getUser().getUserId());
        response.setRoomId(r.getRoom().getRoomId());
        response.setBuildingName(r.getRoom().getBuildingName());
        response.setRoomNumber(r.getRoom().getRoomNumber());
        response.setStartTime(r.getStartTime());
        response.setEndTime(r.getEndTime());
        response.setStatus(r.getStatus().name());
        response.setCreatedTimestamp(r.getCreatedTimestamp());
        return response;
    }
}
