package com.csci201.backend.service;

import com.csci201.backend.dto.CreateStudyGroupRequest;
import com.csci201.backend.dto.GroupMemberResponse;
import com.csci201.backend.dto.InviteToGroupRequest;
import com.csci201.backend.dto.StudyGroupInvitationResponse;
import com.csci201.backend.dto.StudyGroupResponse;
import com.csci201.backend.entity.StudyGroup;
import com.csci201.backend.entity.StudyGroupInvitation;
import com.csci201.backend.entity.StudyGroupMember;
import com.csci201.backend.entity.User;
import com.csci201.backend.entity.enums.GroupInvitationStatus;
import com.csci201.backend.entity.enums.GroupMemberRole;
import com.csci201.backend.entity.enums.GroupVisibility;
import com.csci201.backend.repository.StudyGroupInvitationRepository;
import com.csci201.backend.repository.StudyGroupMemberRepository;
import com.csci201.backend.repository.StudyGroupRepository;
import com.csci201.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final StudyGroupInvitationRepository studyGroupInvitationRepository;
    private final UserRepository userRepository;

    public StudyGroupService(
            StudyGroupRepository studyGroupRepository,
            StudyGroupMemberRepository studyGroupMemberRepository,
            StudyGroupInvitationRepository studyGroupInvitationRepository,
            UserRepository userRepository) {
        this.studyGroupRepository = studyGroupRepository;
        this.studyGroupMemberRepository = studyGroupMemberRepository;
        this.studyGroupInvitationRepository = studyGroupInvitationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StudyGroupResponse createGroup(CreateStudyGroupRequest request) {
        Long ownerUserId = Objects.requireNonNull(request.getOwnerUserId(), "ownerUserId is required");
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + ownerUserId));

        StudyGroup group = new StudyGroup();
        group.setGroupName(request.getGroupName().trim());
        group.setDescription(request.getDescription());
        group.setVisibility(request.getVisibility());
        group.setOwner(owner);
        group.setPrimaryCourse(request.getPrimaryCourse());
        StudyGroup savedGroup = studyGroupRepository.save(group);

        StudyGroupMember ownerMembership = new StudyGroupMember();
        ownerMembership.setGroup(savedGroup);
        ownerMembership.setUser(owner);
        ownerMembership.setMemberRole(GroupMemberRole.OWNER);
        studyGroupMemberRepository.save(ownerMembership);

        return getGroup(savedGroup.getGroupId());
    }

    @Transactional(readOnly = true)
    public List<StudyGroupResponse> getGroupsForUser(Long userId) {
        if (!userRepository.existsById(Objects.requireNonNull(userId, "userId is required"))) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        return studyGroupMemberRepository.findByUserUserId(userId).stream()
                .map(member -> toStudyGroupResponse(member.getGroup(), listMembers(member.getGroup().getGroupId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudyGroupResponse getGroup(Long groupId) {
        StudyGroup group = studyGroupRepository.findById(Objects.requireNonNull(groupId, "groupId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Study group not found: " + groupId));
        return toStudyGroupResponse(group, listMembers(groupId));
    }

    @Transactional
    public StudyGroupResponse joinPublicGroup(Long groupId, Long userId) {
        StudyGroup group = requireGroup(groupId);
        User user = requireUser(userId);

        if (group.getVisibility() != GroupVisibility.PUBLIC) {
            throw new IllegalArgumentException("This group is invite-only");
        }
        if (studyGroupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, userId)) {
            return getGroup(groupId);
        }

        StudyGroupMember membership = new StudyGroupMember();
        membership.setGroup(group);
        membership.setUser(user);
        membership.setMemberRole(GroupMemberRole.MEMBER);
        studyGroupMemberRepository.save(membership);
        return getGroup(groupId);
    }

    @Transactional
    public StudyGroupInvitationResponse inviteToGroup(Long groupId, InviteToGroupRequest request) {
        StudyGroup group = requireGroup(groupId);
        User invitedBy = requireUser(request.getInvitedByUserId());
        User invitedUser = requireUser(request.getInvitedUserId());

        if (!studyGroupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, invitedBy.getUserId())) {
            throw new IllegalArgumentException("Only group members can invite users");
        }
        if (studyGroupMemberRepository.existsByGroupGroupIdAndUserUserId(groupId, invitedUser.getUserId())) {
            throw new IllegalArgumentException("User is already a member of this group");
        }
        if (studyGroupInvitationRepository
                .findByGroupGroupIdAndInvitedUserUserIdAndStatus(groupId, invitedUser.getUserId(), GroupInvitationStatus.PENDING)
                .isPresent()) {
            throw new IllegalArgumentException("User already has a pending invitation");
        }

        StudyGroupInvitation invitation = new StudyGroupInvitation();
        invitation.setGroup(group);
        invitation.setInvitedBy(invitedBy);
        invitation.setInvitedUser(invitedUser);
        invitation.setStatus(GroupInvitationStatus.PENDING);

        return toInvitationResponse(studyGroupInvitationRepository.save(invitation));
    }

    @Transactional
    public StudyGroupInvitationResponse acceptInvite(Long invitationId, Long userId) {
        StudyGroupInvitation invitation = requirePendingInvitation(invitationId);
        if (!invitation.getInvitedUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Only invited user can accept this invitation");
        }

        invitation.setStatus(GroupInvitationStatus.ACCEPTED);
        invitation.setRespondedTimestamp(Instant.now());

        if (!studyGroupMemberRepository.existsByGroupGroupIdAndUserUserId(
                invitation.getGroup().getGroupId(), userId)) {
            StudyGroupMember membership = new StudyGroupMember();
            membership.setGroup(invitation.getGroup());
            membership.setUser(invitation.getInvitedUser());
            membership.setMemberRole(GroupMemberRole.MEMBER);
            studyGroupMemberRepository.save(membership);
        }
        return toInvitationResponse(studyGroupInvitationRepository.save(invitation));
    }

    @Transactional
    public StudyGroupInvitationResponse declineInvite(Long invitationId, Long userId) {
        StudyGroupInvitation invitation = requirePendingInvitation(invitationId);
        if (!invitation.getInvitedUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Only invited user can decline this invitation");
        }
        invitation.setStatus(GroupInvitationStatus.DECLINED);
        invitation.setRespondedTimestamp(Instant.now());
        return toInvitationResponse(studyGroupInvitationRepository.save(invitation));
    }

    private StudyGroup requireGroup(Long groupId) {
        return studyGroupRepository.findById(Objects.requireNonNull(groupId, "groupId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Study group not found: " + groupId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(Objects.requireNonNull(userId, "userId is required"))
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private StudyGroupInvitation requirePendingInvitation(Long invitationId) {
        StudyGroupInvitation invitation = studyGroupInvitationRepository
                .findById(Objects.requireNonNull(invitationId, "invitationId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found: " + invitationId));
        if (invitation.getStatus() != GroupInvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is no longer pending");
        }
        return invitation;
    }

    private List<GroupMemberResponse> listMembers(Long groupId) {
        return studyGroupMemberRepository.findByGroupGroupId(groupId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    private GroupMemberResponse toMemberResponse(StudyGroupMember member) {
        GroupMemberResponse response = new GroupMemberResponse();
        response.setUserId(member.getUser().getUserId());
        response.setUserName(member.getUser().getUserName());
        response.setFirstName(member.getUser().getFirstName());
        response.setLastName(member.getUser().getLastName());
        response.setMemberRole(member.getMemberRole().name());
        response.setJoinedTimestamp(member.getJoinedTimestamp());
        return response;
    }

    private StudyGroupResponse toStudyGroupResponse(StudyGroup group, List<GroupMemberResponse> members) {
        StudyGroupResponse response = new StudyGroupResponse();
        response.setGroupId(group.getGroupId());
        response.setGroupName(group.getGroupName());
        response.setDescription(group.getDescription());
        response.setVisibility(group.getVisibility().name());
        response.setOwnerUserId(group.getOwner().getUserId());
        response.setPrimaryCourse(group.getPrimaryCourse());
        response.setCreatedTimestamp(group.getCreatedTimestamp());
        response.setMembers(members);
        return response;
    }

    private StudyGroupInvitationResponse toInvitationResponse(StudyGroupInvitation invitation) {
        StudyGroupInvitationResponse response = new StudyGroupInvitationResponse();
        response.setInvitationId(invitation.getInvitationId());
        response.setGroupId(invitation.getGroup().getGroupId());
        response.setInvitedByUserId(invitation.getInvitedBy().getUserId());
        response.setInvitedUserId(invitation.getInvitedUser().getUserId());
        response.setStatus(invitation.getStatus().name());
        response.setCreatedTimestamp(invitation.getCreatedTimestamp());
        response.setRespondedTimestamp(invitation.getRespondedTimestamp());
        return response;
    }
}
