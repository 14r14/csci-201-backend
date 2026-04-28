package com.csci201.backend.repository;

import com.csci201.backend.entity.GroupReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupReservationRepository extends JpaRepository<GroupReservation, Long> {}
