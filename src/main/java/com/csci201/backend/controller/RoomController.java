package com.csci201.backend.controller;

import com.csci201.backend.dto.RoomResponse;
import com.csci201.backend.entity.Room;
import com.csci201.backend.repository.RoomRepository;
import com.csci201.backend.repository.WaitlistEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;

    public RoomController(RoomRepository roomRepository, WaitlistEntryRepository waitlistEntryRepository) {
        this.roomRepository = roomRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
    }

    @GetMapping
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public RoomResponse getRoomById(@PathVariable Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + id));
        return toResponse(room);
    }

    private RoomResponse toResponse(Room room) {
        RoomResponse r = new RoomResponse();
        r.setRoomId(room.getRoomId());
        r.setBuildingName(room.getBuildingName());
        r.setRoomNumber(room.getRoomNumber());
        r.setCapacity(room.getCapacity());
        String fl = room.getFeatureList();
        r.setFeatureList(fl == null || fl.isBlank()
                ? List.of()
                : Arrays.stream(fl.split(",")).map(String::trim).collect(Collectors.toList()));
        r.setMapLocation(room.getMapLocation());
        r.setCurrentStatus(room.getCurrentStatus().name());
        r.setAverageRating(room.getAverageRating());
        r.setRatingsCount(room.getRatingsCount());
        r.setWaitlistCount(waitlistEntryRepository.countByRoom_RoomId(room.getRoomId()));
        return r;
    }
}
