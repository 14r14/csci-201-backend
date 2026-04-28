package com.csci201.backend.dto;

import com.csci201.backend.entity.enums.GroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateStudyGroupRequest {

    @NotNull
    private Long ownerUserId;

    @NotBlank
    @Size(max = 255)
    private String groupName;

    @Size(max = 2000)
    private String description;

    @NotNull
    private GroupVisibility visibility;

    @Size(max = 255)
    private String primaryCourse;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GroupVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(GroupVisibility visibility) {
        this.visibility = visibility;
    }

    public String getPrimaryCourse() {
        return primaryCourse;
    }

    public void setPrimaryCourse(String primaryCourse) {
        this.primaryCourse = primaryCourse;
    }
}
