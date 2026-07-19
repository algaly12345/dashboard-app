package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to `business_settings` - a legacy key/value settings store. `type`
 * is NOT unique in the existing data (many duplicate rows accumulated over
 * time from the original app re-inserting instead of updating) - always
 * treat the row with the highest `id` for a given `type` as the current
 * value, and UPDATE that row on save rather than inserting another dupe.
 */
@Entity
@Table(name = "business_settings")
@Getter
@Setter
@NoArgsConstructor
public class BusinessSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, columnDefinition = "longtext")
    private String value;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
