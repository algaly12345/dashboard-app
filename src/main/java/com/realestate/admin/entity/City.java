package com.realestate.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
public class City {

    @Id
    @Column(name = "city_id")
    private Integer cityId;

    @Column(name = "region_id", nullable = false)
    private Integer regionId;

    @Column(name = "name_ar", nullable = false)
    private String nameAr;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    // `center` is a spatial POINT column - not needed for the dashboard, so it
    // is intentionally left unmapped (insertable = false, updatable = false)
    // to avoid pulling in a spatial dialect just for a map pin.
}
