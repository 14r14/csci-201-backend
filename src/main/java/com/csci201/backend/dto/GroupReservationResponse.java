package com.csci201.backend.dto;

public class GroupReservationResponse {

    private Long groupId;
    private Long bookedByUserId;
    private ReservationResponse reservation;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getBookedByUserId() {
        return bookedByUserId;
    }

    public void setBookedByUserId(Long bookedByUserId) {
        this.bookedByUserId = bookedByUserId;
    }

    public ReservationResponse getReservation() {
        return reservation;
    }

    public void setReservation(ReservationResponse reservation) {
        this.reservation = reservation;
    }
}
