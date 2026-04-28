package com.csci201.backend.repository;

import com.csci201.backend.entity.StudyGroupInvitation;
import com.csci201.backend.entity.enums.GroupInvitationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupInvitationRepository extends JpaRepository<StudyGroupInvitation, Long> {

    Optional<StudyGroupInvitation> findByGroupGroupIdAndInvitedUserUserIdAndStatus(
            Long groupId, Long invitedUserId, GroupInvitationStatus status);
}
