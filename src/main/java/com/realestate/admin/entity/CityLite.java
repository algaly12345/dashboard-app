package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cities_lite")
@Getter
@Setter
@NoArgsConstructor
public class CityLite {

    @Id
    @Column(name = "city_id")
    private Integer cityId;

    @Column(name = "region_id")
    private Integer regionId;

    @Column(name = "name_ar")
    private String nameAr;

    @Column(name = "name_en")
    private String nameEn;
}
