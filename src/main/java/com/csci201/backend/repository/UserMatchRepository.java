package com.csci201.backend.repository;

import com.csci201.backend.entity.UserMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMatchRepository extends JpaRepository<UserMatch, Long> {

    Optional<UserMatch> findByUserUserIdAndMatchedUserUserId(Long userId, Long matchedUserId);

    List<UserMatch> findTop50ByUserUserIdOrderByCompatibilityScoreDesc(Long userId);

    @Query(
            "SELECT um FROM UserMatch um "
                    + "WHERE um.user.userId = :userId "
                    + "AND (:course IS NULL OR LOWER(COALESCE(um.sharedCourses, '')) LIKE LOWER(CONCAT('%', :course, '%'))) "
                    + "AND (:minScore IS NULL OR um.compatibilityScore >= :minScore) "
                    + "ORDER BY um.compatibilityScore DESC")
    List<UserMatch> searchMatches(
            @Param("userId") Long userId,
            @Param("course") String course,
            @Param("minScore") Double minScore);
}
