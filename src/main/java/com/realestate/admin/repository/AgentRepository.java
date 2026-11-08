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

    @org.springframework.data.jpa.repository.Query(
            value = "select `identity`, unified_number from agents where user_id = :uid limit 1",
            nativeQuery = true)
    java.util.List<Object[]> findIdentityRowByUserId(
            @org.springframework.data.repository.query.Param("uid") Long uid);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(
            value = "update agents set `identity` = :identity, unified_number = :unified, updated_at = now() where user_id = :uid",
            nativeQuery = true)
    int updateIdentity(@org.springframework.data.repository.query.Param("uid") Long uid,
                       @org.springframework.data.repository.query.Param("identity") String identity,
                       @org.springframework.data.repository.query.Param("unified") String unified);


    @org.springframework.data.jpa.repository.Query(
            value = "select user_id, `identity`, unified_number from agents",
            nativeQuery = true)
    java.util.List<Object[]> findAllIdentityRows();

}