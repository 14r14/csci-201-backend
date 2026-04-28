package com.csci201.backend.controller;

import com.csci201.backend.dto.CreateStudyGroupRequest;
import com.csci201.backend.dto.GroupInvitationActionRequest;
import com.csci201.backend.dto.InviteToGroupRequest;
import com.csci201.backend.dto.JoinStudyGroupRequest;
import com.csci201.backend.dto.StudyGroupInvitationResponse;
import com.csci201.backend.dto.StudyGroupResponse;
import com.csci201.backend.service.StudyGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/study-groups")
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    public StudyGroupController(StudyGroupService studyGroupService) {
        this.studyGroupService = studyGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupResponse createGroup(@Valid @RequestBody CreateStudyGroupRequest request) {
        return studyGroupService.createGroup(request);
    }

    @GetMapping
    public List<StudyGroupResponse> getGroups(@RequestParam Long userId) {
        return studyGroupService.getGroupsForUser(userId);
    }

    @GetMapping("/{groupId}")
    public StudyGroupResponse getGroup(@PathVariable Long groupId) {
        return studyGroupService.getGroup(groupId);
    }

    @PostMapping("/{groupId}/join")
    public StudyGroupResponse joinGroup(@PathVariable Long groupId, @Valid @RequestBody JoinStudyGroupRequest request) {
        return studyGroupService.joinPublicGroup(groupId, request.getUserId());
    }

    @PostMapping("/{groupId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupInvitationResponse inviteToGroup(
            @PathVariable Long groupId, @Valid @RequestBody InviteToGroupRequest request) {
        return studyGroupService.inviteToGroup(groupId, request);
    }

    @PostMapping("/invites/{inviteId}/accept")
    public StudyGroupInvitationResponse acceptInvite(
            @PathVariable Long inviteId, @Valid @RequestBody GroupInvitationActionRequest request) {
        return studyGroupService.acceptInvite(inviteId, request.getUserId());
    }

    @PostMapping("/invites/{inviteId}/decline")
    public StudyGroupInvitationResponse declineInvite(
            @PathVariable Long inviteId, @Valid @RequestBody GroupInvitationActionRequest request) {
        return studyGroupService.declineInvite(inviteId, request.getUserId());
    }
}
