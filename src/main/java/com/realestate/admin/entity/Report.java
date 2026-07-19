package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps to `reports` — a complaint a user filed against a listing (wrong
 * location, spam, misleading info, violates REGA terms, etc). `user_id`
 * defaults to 0 in the existing data (effectively "unknown/anonymous") -
 * treat 0 the same as null when resolving a reporter's account.
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report {

    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private String description;

    @Column(name = "estate_id", nullable = false)
    private Long estateId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** users.id is Long; reports.user_id is a plain int. 0/null both mean
     *  "no reporter on record" - return null for either so map lookups by
     *  this value cleanly miss instead of matching the wrong account. */
    @Transient
    public Long getReporterId() {
        return (userId != null && userId > 0) ? userId.longValue() : null;
    }
}
