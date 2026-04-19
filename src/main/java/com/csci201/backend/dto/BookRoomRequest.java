package com.csci201.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class BookRoomRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long roomId;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}
