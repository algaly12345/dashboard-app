package com.realestate.admin.repository;

import com.realestate.admin.entity.Referral;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    @Query("select coalesce(max(r.id), 0) from Referral r")
    Long findMaxId();

    @Query("select r from Referral r where :status is null or r.status = :status order by r.createdAt desc")
    Page<Referral> search(@Param("status") Referral.Status status, Pageable pageable);

    long countByStatus(Referral.Status status);

    @Query("select r.status, count(r) from Referral r group by r.status")
    List<Object[]> countGroupedByStatus();
}
