package com.csci201.backend.service;

import com.csci201.backend.dto.GroupBookRoomRequest;
import com.csci201.backend.dto.GroupReservationResponse;
import com.csci201.backend.entity.GroupReservation;
import com.csci201.backend.entity.Reservation;
import com.csci201.backend.entity.Room;
import com.csci201.backend.entity.StudyGroup;
import com.csci201.backend.entity.User;
import com.csci201.backend.repository.GroupReservationRepository;
import com.csci201.backend.repository.StudyGroupMemberRepository;
import com.csci201.backend.repository.StudyGroupRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupReservationService {

    private final ReservationService reservationService;
    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final GroupReservationRepository groupReservationRepository;

    public GroupReservationService(
            ReservationService reservationService,
            StudyGroupRepository studyGroupRepository,
            StudyGroupMemberRepository studyGroupMemberRepository,
            GroupReservationRepository groupReservationRepository) {
        this.reservationService = reservationService;
        this.studyGroupRepository = studyGroupRepository;
        this.studyGroupMemberRepository = studyGroupMemberRepository;
        this.groupReservationRepository = groupReservationRepository;
    }

    @Transactional
    public GroupReservationResponse bookRoomForGroup(GroupBookRoomRequest request) {
        Long groupId = Objects.requireNonNull(request.getGroupId(), "groupId is required");
        Long bookedByUserId = Objects.requireNonNull(request.getBookedByUserId(), "bookedByUserId is required");
        Long roomId = Objects.requireNonNull(request.getRoomId(), "roomId is required");

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Study group not found: " + groupId));
        User bookedBy = reservationService.requireUser(bookedByUserId);
        Room room = reservationService.requireRoom(roomId);

        if (!studyGroupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, bookedByUserId)) {
            throw new IllegalArgumentException("Only group members can book rooms for this group");
        }

        long groupSize = studyGroupMemberRepository.countByGroupGroupId(groupId);
        if (groupSize > room.getCapacity()) {
            throw new IllegalArgumentException("Room capacity is not enough for the group size");
        }

        Reservation reservation = reservationService.createConfirmedReservation(
                bookedBy, room, request.getStartTime(), request.getEndTime());

        GroupReservation groupReservation = new GroupReservation();
        groupReservation.setGroup(group);
        groupReservation.setBookedBy(bookedBy);
        groupReservation.setReservation(reservation);
        groupReservationRepository.save(groupReservation);

        GroupReservationResponse response = new GroupReservationResponse();
        response.setGroupId(groupId);
        response.setBookedByUserId(bookedByUserId);
        response.setReservation(reservationService.toResponse(reservation));
        return response;
    }
}
