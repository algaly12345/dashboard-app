package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "districts_lite")
@Getter
@Setter
@NoArgsConstructor
public class DistrictLite {

    @Id
    @Column(name = "district_id")
    private String districtId;

    @Column(name = "city_id")
    private Integer cityId;

    @Column(name = "region_id")
    private Integer regionId;

    @Column(name = "name_ar")
    private String nameAr;

    @Column(name = "name_en")
    private String nameEn;
}
