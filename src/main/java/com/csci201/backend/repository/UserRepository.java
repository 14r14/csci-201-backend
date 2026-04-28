package com.csci201.backend.repository;

import com.csci201.backend.entity.User;
import com.csci201.backend.entity.enums.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);

    List<User> findByRoleAndUserIdNot(UserRole role, Long excludedUserId);
}
