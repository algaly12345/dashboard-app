package com.realestate.admin.repository;

import com.realestate.admin.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
    List<ServiceType> findAllByOrderByNameAsc();
}
