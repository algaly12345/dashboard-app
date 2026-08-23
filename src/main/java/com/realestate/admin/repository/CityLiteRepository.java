package com.realestate.admin.repository;

import com.realestate.admin.entity.CityLite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityLiteRepository extends JpaRepository<CityLite, Integer> {
    List<CityLite> findByRegionIdOrderByNameAr(Integer regionId);
}
