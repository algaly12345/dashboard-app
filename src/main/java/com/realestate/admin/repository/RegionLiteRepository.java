package com.realestate.admin.repository;

import com.realestate.admin.entity.RegionLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegionLiteRepository extends JpaRepository<RegionLite, Integer> {
    @Query("select r from RegionLite r order by r.nameAr")
    List<RegionLite> findAllOrderByName();
}
