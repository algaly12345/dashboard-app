package com.realestate.admin.repository;

import com.realestate.admin.entity.NotificationApp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationAppRepository extends JpaRepository<NotificationApp, Long> {
    Page<NotificationApp> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** id may not be AUTO_INCREMENT in this DB - assign it manually on insert. */
    @Query("select coalesce(max(n.id), 0) from NotificationApp n")
    Long findMaxId();
}
