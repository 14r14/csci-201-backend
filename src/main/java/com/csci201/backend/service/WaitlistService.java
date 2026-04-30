package com.csci201.backend.service;

import com.csci201.backend.dto.JoinWaitlistRequest;
import com.csci201.backend.dto.WaitlistResponse;
import com.csci201.backend.entity.Reservation;
import com.csci201.backend.entity.Room;
import com.csci201.backend.entity.User;
import com.csci201.backend.entity.WaitlistEntry;
import com.csci201.backend.entity.enums.ReservationStatus;
import com.csci201.backend.entity.enums.RoomCurrentStatus;
import com.csci201.backend.repository.ReservationRepository;
import com.csci201.backend.repository.RoomRepository;
import com.csci201.backend.repository.UserRepository;
import com.csci201.backend.repository.WaitlistEntryRepository;
import jakarta.transaction.Transactional;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WaitlistService {

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public WaitlistService(
            WaitlistEntryRepository waitlistEntryRepository,
            UserRepository userRepository,
            RoomRepository roomRepository,
            ReservationRepository reservationRepository
    ) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public WaitlistResponse joinWaitlist(JoinWaitlistRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found."));

        boolean isMarkedBooked = room.getCurrentStatus() == RoomCurrentStatus.OCCUPIED
                || room.getCurrentStatus() == RoomCurrentStatus.RESERVED;
        boolean hasConfirmedReservation = reservationRepository
                .existsByRoom_RoomIdAndStatus(request.getRoomId(), ReservationStatus.CONFIRMED);
        if (!isMarkedBooked && !hasConfirmedReservation) {
            throw new IllegalArgumentException(
                    "Room is not currently booked. Book it directly instead."
            );
        }

        boolean alreadyOnWaitlist = waitlistEntryRepository.existsByUser_UserIdAndRoom_RoomIdAndRequestedTimeSlot(
                request.getUserId(),
                request.getRoomId(),
                request.getRequestedTimeSlot()
        );

        if (alreadyOnWaitlist) {
            throw new IllegalArgumentException(
                    "User is already on the waitlist for this room and time slot."
            );
        }

        long currentCount = waitlistEntryRepository.countByRoom_RoomIdAndRequestedTimeSlot(
                request.getRoomId(),
                request.getRequestedTimeSlot()
        );

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setRoom(room);
        entry.setRequestedTimeSlot(request.getRequestedTimeSlot());
        entry.setQueuePosition((int) currentCount + 1);

        WaitlistEntry savedEntry = waitlistEntryRepository.save(entry);

        return new WaitlistResponse(
                savedEntry.getWaitlistId(),
                user.getUserId(),
                room.getRoomId(),
                savedEntry.getRequestedTimeSlot(),
                savedEntry.getQueuePosition(),
                currentCount + 1,
                "User successfully added to waitlist."
        );
    }

    public List<WaitlistEntry> getWaitlistForRoomAndTime(Long roomId, String requestedTimeSlot) {
        return waitlistEntryRepository.findByRoom_RoomIdAndRequestedTimeSlotOrderByQueuePositionAsc(
                roomId,
                requestedTimeSlot
        );
    }

    public long getWaitlistCount(Long roomId, String requestedTimeSlot) {
        return waitlistEntryRepository.countByRoom_RoomIdAndRequestedTimeSlot(
                roomId,
                requestedTimeSlot
        );
    }

    @Transactional
    public void leaveWaitlist(Long waitlistId) {
        WaitlistEntry entry = waitlistEntryRepository.findById(waitlistId)
                .orElseThrow(() -> new IllegalArgumentException("Waitlist entry not found."));

        Long roomId = entry.getRoom().getRoomId();
        String timeSlot = entry.getRequestedTimeSlot();
        Integer position = entry.getQueuePosition();

        waitlistEntryRepository.delete(entry);
        waitlistEntryRepository.decrementPositionsAfter(roomId, timeSlot, position);
    }

    @Transactional
    public List<WaitlistResponse> getWaitlistByUser(Long userId) {
        return waitlistEntryRepository.findByUser_UserIdOrderByCreatedTimestampDesc(userId).stream()
                .map(e -> new WaitlistResponse(
                        e.getWaitlistId(),
                        e.getUser().getUserId(),
                        e.getRoom().getRoomId(),
                        e.getRequestedTimeSlot(),
                        e.getQueuePosition(),
                        waitlistEntryRepository.countByRoom_RoomIdAndRequestedTimeSlot(
                                e.getRoom().getRoomId(), e.getRequestedTimeSlot()),
                        null
                ))
                .toList();
    }

    @Transactional
    public Reservation promoteNextUserIfAvailable(Reservation cancelledReservation) {
        Long roomId = cancelledReservation.getRoom().getRoomId();
        String requestedTimeSlot = cancelledReservation.getStartTime().toString();

        WaitlistEntry nextEntry = waitlistEntryRepository
                .findFirstByRoom_RoomIdAndRequestedTimeSlotOrderByQueuePositionAsc(
                        roomId,
                        requestedTimeSlot
                )
                .orElse(null);

        if (nextEntry == null) {
            return null;
        }

        Reservation newReservation = new Reservation();
        newReservation.setUser(nextEntry.getUser());
        newReservation.setRoom(nextEntry.getRoom());
        newReservation.setStartTime(cancelledReservation.getStartTime());
        newReservation.setEndTime(cancelledReservation.getEndTime());
        newReservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation savedReservation = reservationRepository.save(newReservation);

        Integer promotedPosition = nextEntry.getQueuePosition();
        waitlistEntryRepository.delete(nextEntry);
        waitlistEntryRepository.decrementPositionsAfter(roomId, requestedTimeSlot, promotedPosition);

        return savedReservation;
    }
}