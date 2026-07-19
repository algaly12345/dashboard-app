package com.realestate.admin.repository;

import com.realestate.admin.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    long countByUserType(String userType);

    /** Phone numbers aren't guaranteed unique in this table (dupes exist in
     *  the data) - take the first match rather than requiring one result. */
    Optional<AppUser> findFirstByPhoneOrderByIdAsc(String phone);

    @Query("""
           select u from AppUser u
           where (:q is null or lower(u.name) like lower(concat('%', :q, '%'))
                              or u.phone like concat('%', :q, '%'))
             and (:userType is null or u.userType = :userType)
           order by u.createdAt desc
           """)
    Page<AppUser> search(@Param("q") String q, @Param("userType") String userType, Pageable pageable);
}
