package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_ar", nullable = false)
    private String nameAr;

    @Column(nullable = false)
    private String slug;

    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_home")
    private Status statusHome;

    private String image;

    @Column(name = "parent_id")
    private Integer parentId;

    private String type;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum Status { active, disactive }
}
