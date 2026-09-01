package com.realestate.admin.repository;

import com.realestate.admin.entity.CommissionWithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommissionWithdrawalRequestRepository extends JpaRepository<CommissionWithdrawalRequest, Long> {

    @Query("select w from CommissionWithdrawalRequest w where :status is null or w.status = :status order by w.requestedAt desc")
    Page<CommissionWithdrawalRequest> search(@Param("status") CommissionWithdrawalRequest.Status status, Pageable pageable);

    long countByStatus(CommissionWithdrawalRequest.Status status);
}
