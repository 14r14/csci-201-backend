package com.csci201.backend.repository;

import com.csci201.backend.entity.UserMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMatchRepository extends JpaRepository<UserMatch, Long> {}
