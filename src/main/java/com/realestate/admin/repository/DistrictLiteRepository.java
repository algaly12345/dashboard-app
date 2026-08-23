package com.realestate.admin.repository;

import com.realestate.admin.entity.DistrictLite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DistrictLiteRepository extends JpaRepository<DistrictLite, String> {
    List<DistrictLite> findByCityIdOrderByNameAr(Integer cityId);
}
