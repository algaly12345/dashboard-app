package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to `banners` - promotional banners shown in the app, optionally
 * scoped to a zone and/or tied to a service provider. `status` is
 * tinyint(1) - MySQL's JDBC driver reports that as BIT, which Hibernate
 * maps to Boolean (same gotcha we hit with notification_apps.status).
 */
@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
public class Banner {

    @Id
    private Long id;

    private String title;

    private String image;

    private String description;

    @Column(nullable = false)
    private Boolean status = true;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
