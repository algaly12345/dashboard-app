package com.realestate.admin.repository;

import com.realestate.admin.entity.OfferZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfferZoneRepository extends JpaRepository<OfferZone, OfferZone.OfferZoneId> {

    /** [zoneId, distinct offer count] for every zone that has at least one offer. */
    @Query("select oz.zoneId, count(distinct oz.offerId) from OfferZone oz group by oz.zoneId")
    List<Object[]> countDistinctOffersByZone();

    @Query("select count(distinct oz.offerId) from OfferZone oz")
    long countDistinctOffers();

    @Query("select oz.zoneId from OfferZone oz where oz.offerId = :offerId")
    List<Long> findZoneIdsByOfferId(@Param("offerId") Long offerId);

    List<OfferZone> findByOfferIdIn(List<Long> offerIds);
}
