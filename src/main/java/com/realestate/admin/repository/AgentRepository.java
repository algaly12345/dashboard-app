package com.realestate.admin.repository;

import com.realestate.admin.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Integer> {
    Optional<Agent> findByUserId(Long userId);
}
