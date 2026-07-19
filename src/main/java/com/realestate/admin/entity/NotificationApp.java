package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to `notification_apps`. One row per marketing push notification
 * composed/sent from the admin. `target` is stored under the column name
 * `tergat` in the DB (a typo in the original schema - kept as-is on
 * purpose, changing it would mean altering a live table).
 */
@Entity
@Table(name = "notification_apps")
@Getter
@Setter
@NoArgsConstructor
public class NotificationApp {

    @Id
    private Long id;

    private String title;

    private String description;

    /** DB column is literally `tergat` (typo for "target"). */
    @Column(name = "tergat")
    private String target;

    private String type;

    @Column(nullable = false)
    private Boolean status = true;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
