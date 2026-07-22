package com.realestate.admin.repository;

import com.realestate.admin.entity.Estate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EstateRepository extends JpaRepository<Estate, Long> {

    long countByStatus(Estate.Status status);

    long countByAdvertisementType(String advertisementType);

    @Query("select e from Estate e where (:zoneId is null or e.zoneId = :zoneId) and (:categoryId is null or e.categoryId = :categoryId) and (:userId is null or e.userId = :userId)")
    List<Estate> findForReport(@Param("zoneId") Long zoneId, @Param("categoryId") Integer categoryId, @Param("userId") Long userId);

    @Query("select distinct e.city from Estate e where e.city is not null and (:zoneId is null or e.zoneId = :zoneId) order by e.city")
    List<String> findDistinctCitiesByZone(@Param("zoneId") Long zoneId);

    @Query("select distinct e.districts from Estate e where e.districts is not null and (:city is null or e.city = :city) and (:zoneId is null or e.zoneId = :zoneId) order by e.districts")
    List<String> findDistinctDistricts(@Param("city") String city, @Param("zoneId") Long zoneId);

    @Query("select distinct e.advertiserName from Estate e where e.advertiserName is not null order by e.advertiserName")
    List<String> findDistinctAdvertiserNames();

    @Query("""
           select e from Estate e
           where (:zoneId is null or e.zoneId = :zoneId)
             and (:city is null or e.city = :city)
             and (:district is null or e.districts = :district)
             and (:categoryId is null or e.categoryId = :categoryId)
             and (:advertiserName is null or lower(e.advertiserName) like lower(concat('%', :advertiserName, '%')))
             and e.latitude is not null and e.latitude <> ''
             and e.longitude is not null and e.longitude <> ''
           order by e.createdAt desc
           """)
    Page<Estate> searchForMap(@Param("zoneId") Long zoneId,
                               @Param("city") String city,
                               @Param("district") String district,
                               @Param("categoryId") Integer categoryId,
                               @Param("advertiserName") String advertiserName,
                               Pageable pageable);

    @Query("select e from Estate e where e.latitude is not null and e.latitude <> '' and e.longitude is not null and e.longitude <> '' order by e.createdAt desc")
    List<Estate> findRecentWithCoordinates(org.springframework.data.domain.Pageable pageable);

    @Query("select distinct e.userId from Estate e where e.userId is not null")
    List<Long> findDistinctUserIds();

    @Query("select e.city, count(e), avg(cast(e.price as big_decimal)) from Estate e where e.city is not null group by e.city order by count(e) desc")
    List<Object[]> countAndAvgPriceGroupedByCity();

    @Query("select e.zoneId, count(e), avg(cast(e.price as big_decimal)) from Estate e where e.zoneId is not null group by e.zoneId order by count(e) desc")
    List<Object[]> countAndAvgPriceGroupedByZone();

    @Query("select sum(cast(e.price as big_decimal)) from Estate e where e.advertisementType = 'بيع'")
    java.math.BigDecimal sumSalePrice();

    @Query("select avg(cast(e.price as big_decimal)) from Estate e where e.advertisementType = 'بيع'")
    java.math.BigDecimal avgSalePrice();

    @Query(value = """
           select date_format(created_at, '%Y-%m') as ym, count(*) as cnt
           from estates
           where created_at is not null
           group by ym
           order by ym desc
           limit 12
           """, nativeQuery = true)
    List<Object[]> countByMonthLast12();

    @Query("select e.categoryName, e.advertisementType, count(e) from Estate e where e.categoryName is not null group by e.categoryName, e.advertisementType")
    List<Object[]> countGroupedByCategoryAndAdType();

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    @Query("select max(e.createdAt) from Estate e")
    LocalDateTime findMaxCreatedAt();

    long countByUserId(Long userId);

    @Query("select e.userId, count(e) from Estate e where e.userId is not null group by e.userId")
    List<Object[]> countGroupedByUser();

    @Query("select e.zoneId, count(e) from Estate e where e.zoneId is not null group by e.zoneId")
    List<Object[]> countGroupedByZone();

    @Query("""
           select e from Estate e
           where (:q is null or lower(e.shortDescription) like lower(concat('%', :q, '%'))
                              or lower(e.city) like lower(concat('%', :q, '%'))
                              or lower(e.districts) like lower(concat('%', :q, '%'))
                              or lower(e.advertiserName) like lower(concat('%', :q, '%')))
             and (:status is null or e.status = :status)
             and (:city is null or e.city = :city)
             and (:categoryName is null or e.categoryName = :categoryName)
             and (:adType is null or e.advertisementType = :adType)
             and (:estateType is null or e.estateType = :estateType)
             and (:virtualTour is null
                  or (:virtualTour = true  and e.arPath is not null and e.arPath <> '')
                  or (:virtualTour = false and (e.arPath is null or e.arPath = '')))
             and (:minPrice is null or cast(e.price as big_decimal) >= :minPrice)
             and (:maxPrice is null or cast(e.price as big_decimal) <= :maxPrice)
           order by e.createdAt desc
           """)
    Page<Estate> search(@Param("q") String q,
                         @Param("status") Estate.Status status,
                         @Param("city") String city,
                         @Param("categoryName") String categoryName,
                         @Param("adType") String adType,
                         @Param("estateType") String estateType,
                         @Param("virtualTour") Boolean virtualTour,
                         @Param("minPrice") Double minPrice,
                         @Param("maxPrice") Double maxPrice,
                         Pageable pageable);

    List<Estate> findTop6ByOrderByCreatedAtDesc();

    @Query("select distinct e.city from Estate e where e.city is not null order by e.city")
    List<String> findDistinctCities();

    @Query("select distinct e.categoryName from Estate e where e.categoryName is not null order by e.categoryName")
    List<String> findDistinctCategoryNames();

    @Query("select distinct e.estateType from Estate e where e.estateType is not null order by e.estateType")
    List<String> findDistinctEstateTypes();

    @Query("select e.categoryName, count(e) from Estate e where e.categoryName is not null group by e.categoryName order by count(e) desc")
    List<Object[]> countGroupedByCategory();

    @Query("select e.city, count(e) from Estate e where e.city is not null group by e.city order by count(e) desc")
    List<Object[]> countGroupedByCity();
}
