package com.csci201.backend.dto;

import java.time.Instant;

public class StudyGroupInvitationResponse {

    private Long invitationId;
    private Long groupId;
    private Long invitedByUserId;
    private Long invitedUserId;
    private String status;
    private Instant createdTimestamp;
    private Instant respondedTimestamp;

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Instant createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Instant getRespondedTimestamp() {
        return respondedTimestamp;
    }

    public void setRespondedTimestamp(Instant respondedTimestamp) {
        this.respondedTimestamp = respondedTimestamp;
    }
}
