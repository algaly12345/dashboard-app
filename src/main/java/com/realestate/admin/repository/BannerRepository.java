package com.realestate.admin.repository;

import com.realestate.admin.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findAllByOrderByIdDesc();

    /** id may not be AUTO_INCREMENT in this DB - assign it manually on insert. */
    @Query("select coalesce(max(b.id), 0) from Banner b")
    Long findMaxId();
}
