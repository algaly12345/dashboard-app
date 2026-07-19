package com.realestate.admin.repository;

import com.realestate.admin.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByEstateId(Long estateId);

    @Query("select distinct r.title from Report r order by r.title")
    List<String> findDistinctTitles();

    @Query("select r.estateId, count(r) from Report r group by r.estateId order by count(r) desc")
    List<Object[]> countGroupedByEstate();

    @Query("""
           select r from Report r
           where (:q is null or lower(r.description) like lower(concat('%', :q, '%'))
                              or lower(r.title) like lower(concat('%', :q, '%')))
             and (:title is null or r.title = :title)
             and (:estateId is null or r.estateId = :estateId)
           order by r.createdAt desc
           """)
    Page<Report> search(@Param("q") String q,
                         @Param("title") String title,
                         @Param("estateId") Long estateId,
                         Pageable pageable);
}
