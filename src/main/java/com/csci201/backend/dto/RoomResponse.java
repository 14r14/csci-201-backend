package com.csci201.backend.dto;

import java.util.List;

/** Response payload for a room, including all rating aggregates. */
public class RoomResponse {

    private Long roomId;
    private String buildingName;
    private String roomNumber;
    private Integer capacity;
    private List<String> featureList;
    private String mapLocation;
    private String currentStatus;

    // Overall rating
    private Double averageRating;
    private Integer ratingsCount;

    // Subcategory averages (0.0 when no reviews have supplied that subcategory)
    private Double avgNoiseRating;
    private Double avgCleanlinessRating;
    private Double avgAmenitiesRating;

    private long waitlistCount;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public List<String> getFeatureList() { return featureList; }
    public void setFeatureList(List<String> featureList) { this.featureList = featureList; }

    public String getMapLocation() { return mapLocation; }
    public void setMapLocation(String mapLocation) { this.mapLocation = mapLocation; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(Integer ratingsCount) { this.ratingsCount = ratingsCount; }

    public Double getAvgNoiseRating() { return avgNoiseRating; }
    public void setAvgNoiseRating(Double avgNoiseRating) { this.avgNoiseRating = avgNoiseRating; }

    public Double getAvgCleanlinessRating() { return avgCleanlinessRating; }
    public void setAvgCleanlinessRating(Double avgCleanlinessRating) { this.avgCleanlinessRating = avgCleanlinessRating; }

    public Double getAvgAmenitiesRating() { return avgAmenitiesRating; }
    public void setAvgAmenitiesRating(Double avgAmenitiesRating) { this.avgAmenitiesRating = avgAmenitiesRating; }

    public long getWaitlistCount() { return waitlistCount; }
    public void setWaitlistCount(long waitlistCount) { this.waitlistCount = waitlistCount; }
}
