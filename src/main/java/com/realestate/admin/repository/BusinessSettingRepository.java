package com.realestate.admin.repository;

import com.realestate.admin.entity.BusinessSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BusinessSettingRepository extends JpaRepository<BusinessSetting, Long> {

    /** Most recent row first - callers take .get(0) as "the current value". */
    List<BusinessSetting> findByTypeOrderByIdDesc(String type);

    @Query("select max(b.id) from BusinessSetting b where b.type = :type")
    Long findMaxIdForType(@Param("type") String type);
}
