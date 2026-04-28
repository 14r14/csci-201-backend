package com.csci201.backend.dto;

import jakarta.validation.constraints.NotNull;

public class InviteToGroupRequest {

    @NotNull
    private Long invitedByUserId;

    @NotNull
    private Long invitedUserId;

    public Long getInvitedByUserId() {
        return invitedByUserId;
    }

    public void setInvitedByUserId(Long invitedByUserId) {
        this.invitedByUserId = invitedByUserId;
    }

    public Long getInvitedUserId() {
        return invitedUserId;
    }

    public void setInvitedUserId(Long invitedUserId) {
        this.invitedUserId = invitedUserId;
    }
}
