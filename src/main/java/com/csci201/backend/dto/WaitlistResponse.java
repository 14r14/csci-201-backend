package com.csci201.backend.dto;

public class WaitlistResponse {

    private Long waitlistId;
    private Long userId;
    private Long roomId;
    private String requestedTimeSlot;
    private Integer queuePosition;
    private long waitlistCount;
    private String message;

    public WaitlistResponse(
            Long waitlistId,
            Long userId,
            Long roomId,
            String requestedTimeSlot,
            Integer queuePosition,
            long waitlistCount,
            String message
    ) {
        this.waitlistId = waitlistId;
        this.userId = userId;
        this.roomId = roomId;
        this.requestedTimeSlot = requestedTimeSlot;
        this.queuePosition = queuePosition;
        this.waitlistCount = waitlistCount;
        this.message = message;
    }

    public Long getWaitlistId() {
        return waitlistId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getRequestedTimeSlot() {
        return requestedTimeSlot;
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }

    public long getWaitlistCount() {
        return waitlistCount;
    }

    public String getMessage() {
        return message;
    }
}