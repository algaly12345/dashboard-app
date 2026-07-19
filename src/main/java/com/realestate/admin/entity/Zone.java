package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "zones")
@Getter
@Setter
@NoArgsConstructor
public class Zone {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_ar", nullable = false)
    private String nameAr;

    private String image;

    @Column(name = "territory_id", nullable = false)
    private Long territoryId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String latitude;
    private String longitude;

    public enum Status { active, disactive }
}
