package com.realestate.admin.repository;

import com.realestate.admin.entity.Commission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommissionRepository extends JpaRepository<Commission, Long> {

    @Query("select coalesce(max(c.id), 0) from Commission c")
    Long findMaxId();

    @Query("select c from Commission c where :status is null or c.status = :status order by c.createdAt desc")
    Page<Commission> search(@Param("status") Commission.Status status, Pageable pageable);

    List<Commission> findByReferralId(Long referralId);

    @Query("select c.status, count(c), coalesce(sum(c.amount), 0) from Commission c group by c.status")
    List<Object[]> summaryGroupedByStatus();
}
