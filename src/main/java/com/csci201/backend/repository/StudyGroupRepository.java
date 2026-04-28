package com.csci201.backend.repository;

import com.csci201.backend.entity.StudyGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    List<StudyGroup> findByOwnerUserId(Long ownerUserId);
}
