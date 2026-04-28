package com.csci201.backend.repository;

import com.csci201.backend.entity.StudyGroupMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, Long> {

    boolean existsByGroupGroupIdAndUserUserId(Long groupId, Long userId);

    long countByGroupGroupId(Long groupId);

    List<StudyGroupMember> findByGroupGroupId(Long groupId);

    List<StudyGroupMember> findByUserUserId(Long userId);
}
