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
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + request.getRoomId()));

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        if (reservationRepository.existsOverlappingReservation(
                room.getRoomId(), ReservationStatus.CONFIRMED,
                request.getStartTime(), request.getEndTime())) {
            throw new RoomNotAvailableException("Room is already booked for the requested time slot");
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setStatus(ReservationStatus.CONFIRMED);

        reservation = reservationRepository.save(reservation);
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

    private ReservationResponse toResponse(Reservation r) {
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
