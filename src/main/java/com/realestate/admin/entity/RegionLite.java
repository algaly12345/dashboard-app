package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "regions_lite")
@Getter
@Setter
@NoArgsConstructor
public class RegionLite {

    @Id
    @Column(name = "region_id")
    private Integer regionId;

    @Column(name = "capital_city_id")
    private Integer capitalCityId;

    private String code;

    @Column(name = "name_ar")
    private String nameAr;

    @Column(name = "name_en")
    private String nameEn;

    private Integer population;

    private String latitude;
    private String longitude;
}
