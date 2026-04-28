package com.csci201.backend.dto;

import java.time.Instant;

public class GroupMemberResponse {

    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String memberRole;
    private Instant joinedTimestamp;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public Instant getJoinedTimestamp() {
        return joinedTimestamp;
    }

    public void setJoinedTimestamp(Instant joinedTimestamp) {
        this.joinedTimestamp = joinedTimestamp;
    }
}
