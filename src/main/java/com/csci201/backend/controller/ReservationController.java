package com.csci201.backend.controller;

import com.csci201.backend.dto.BookRoomRequest;
import com.csci201.backend.dto.ReservationResponse;
import com.csci201.backend.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse bookRoom(@Valid @RequestBody BookRoomRequest request) {
        return reservationService.bookRoom(request);
    }

    @DeleteMapping("/{id}")
    public ReservationResponse cancelReservation(@PathVariable Long id) {
        return reservationService.cancelReservation(id);
    }

    @GetMapping
    public List<ReservationResponse> getReservationsByUser(@RequestParam Long userId) {
        return reservationService.getReservationsByUser(userId);
    }
}
