package com.realestate.admin.repository;

import com.realestate.admin.entity.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    long countByStatus(String status);

    long countByPhoneProvider(String phoneProvider);

    @Query("select o.phoneProvider, count(o) from Offer o where o.phoneProvider is not null group by o.phoneProvider")
    List<Object[]> countGroupedByPhoneProvider();

    @Query("select o.status, count(o) from Offer o group by o.status")
    List<Object[]> countGroupedByStatus();

    @Query("""
           select o from Offer o
           where (:zoneId is null or exists (
                    select 1 from OfferZone oz where oz.offerId = o.id and oz.zoneId = :zoneId))
             and (:serviceTypeId is null or o.serviceTypeId = :serviceTypeId)
             and (:phoneProvider is null or o.phoneProvider = :phoneProvider)
           """)
    List<Offer> findForReport(@Param("zoneId") Long zoneId,
                               @Param("serviceTypeId") Long serviceTypeId,
                               @Param("phoneProvider") String phoneProvider);

    @Query("select o.serviceTypeId, count(o) from Offer o group by o.serviceTypeId order by count(o) desc")
    List<Object[]> countGroupedByServiceType();

    @Query("select avg(o.discount) from Offer o where o.offerType = com.realestate.admin.entity.Offer.OfferType.discount and o.discount is not null")
    Double avgDiscount();

    @Query("select avg(o.servicePrice) from Offer o where o.offerType = com.realestate.admin.entity.Offer.OfferType.price and o.servicePrice is not null")
    Double avgServicePrice();

    @Query(value = """
           select date_format(created_at, '%Y-%m') as ym, count(*) as cnt
           from offers
           where created_at is not null
           group by ym
           order by ym desc
           limit 12
           """, nativeQuery = true)
    List<Object[]> countByMonthLast12();

    @Query("select distinct o.serviceTypeId from Offer o order by o.serviceTypeId")
    List<Long> findDistinctServiceTypeIds();

    @Query("""
           select o from Offer o
           where (:q is null or lower(o.title) like lower(concat('%', :q, '%'))
                              or lower(o.description) like lower(concat('%', :q, '%'))
                              or o.phoneProvider like concat('%', :q, '%'))
             and (:status is null or o.status = :status)
             and (:offerType is null or o.offerType = :offerType)
             and (:serviceTypeId is null or o.serviceTypeId = :serviceTypeId)
             and (:zoneId is null or exists (
                    select 1 from OfferZone oz where oz.offerId = o.id and oz.zoneId = :zoneId))
             and (:categoryId is null or exists (
                    select 1 from CategoryOffer co where co.offerId = o.id and co.categoryId = :categoryId))
           order by o.createdAt desc
           """)
    Page<Offer> search(@Param("q") String q,
                        @Param("status") String status,
                        @Param("offerType") Offer.OfferType offerType,
                        @Param("serviceTypeId") Long serviceTypeId,
                        @Param("zoneId") Long zoneId,
                        @Param("categoryId") Long categoryId,
                        Pageable pageable);
}
