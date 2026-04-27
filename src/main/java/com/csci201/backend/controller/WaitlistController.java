package com.csci201.backend.controller;

import com.csci201.backend.dto.JoinWaitlistRequest;
import com.csci201.backend.dto.WaitlistResponse;
import com.csci201.backend.entity.WaitlistEntry;
import com.csci201.backend.service.WaitlistService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    public ResponseEntity<WaitlistResponse> joinWaitlist(
            @Valid @RequestBody JoinWaitlistRequest request
    ) {
        return ResponseEntity.ok(waitlistService.joinWaitlist(request));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<List<WaitlistEntry>> getWaitlistForRoomAndTime(
            @PathVariable Long roomId,
            @RequestParam String requestedTimeSlot
    ) {
        return ResponseEntity.ok(
                waitlistService.getWaitlistForRoomAndTime(roomId, requestedTimeSlot)
        );
    }

    @GetMapping("/rooms/{roomId}/count")
    public ResponseEntity<Long> getWaitlistCount(
            @PathVariable Long roomId,
            @RequestParam String requestedTimeSlot
    ) {
        return ResponseEntity.ok(
                waitlistService.getWaitlistCount(roomId, requestedTimeSlot)
        );
    }

    @DeleteMapping("/{waitlistId}")
    public ResponseEntity<String> leaveWaitlist(@PathVariable Long waitlistId) {
        waitlistService.leaveWaitlist(waitlistId);
        return ResponseEntity.ok("Waitlist entry removed.");
    }

    @GetMapping
    public ResponseEntity<List<WaitlistResponse>> getWaitlistByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(waitlistService.getWaitlistByUser(userId));
    }
}