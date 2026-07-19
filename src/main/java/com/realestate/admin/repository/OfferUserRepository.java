package com.realestate.admin.repository;

import com.realestate.admin.entity.OfferUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OfferUserRepository extends JpaRepository<OfferUser, Integer> {

    /** How many service offers this user has submitted (any status). */
    long countByUserId(Long userId);

    @Query("select count(distinct ou.offerId) from OfferUser ou")
    long countDistinctOffers();

    @Query("select ou.userId, count(distinct ou.offerId) from OfferUser ou group by ou.userId")
    List<Object[]> countGroupedByUser();
}
