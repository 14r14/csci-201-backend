package com.csci201.backend.controller;

import com.csci201.backend.dto.GroupBookRoomRequest;
import com.csci201.backend.dto.GroupReservationResponse;
import com.csci201.backend.service.GroupReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/group-reservations")
public class GroupReservationController {

    private final GroupReservationService groupReservationService;

    public GroupReservationController(GroupReservationService groupReservationService) {
        this.groupReservationService = groupReservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupReservationResponse bookRoomForGroup(@Valid @RequestBody GroupBookRoomRequest request) {
        return groupReservationService.bookRoomForGroup(request);
    }
}
