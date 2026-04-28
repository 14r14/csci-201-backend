package com.csci201.backend.dto;

public class MatchSuggestionResponse {

    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String sharedCourses;
    private Double compatibilityScore;

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

    public String getSharedCourses() {
        return sharedCourses;
    }

    public void setSharedCourses(String sharedCourses) {
        this.sharedCourses = sharedCourses;
    }

    public Double getCompatibilityScore() {
        return compatibilityScore;
    }

    public void setCompatibilityScore(Double compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }
}
