package com.realestate.admin.repository;

import com.realestate.admin.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Integer> {

    Optional<Agent> findByUserId(Long userId);

    Optional<Agent> findByIdentity(String identity);

    Optional<Agent> findByUnifiedNumber(String unifiedNumber);

    /** agents.id isn't necessarily AUTO_INCREMENT - assign it manually on insert, same pattern as Banner/AppUser. */
    @Query("select coalesce(max(a.id), 0) from Agent a")
    Integer findMaxId();
}