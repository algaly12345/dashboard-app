package com.realestate.admin.repository;

import com.realestate.admin.entity.CategoryOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryOfferRepository extends JpaRepository<CategoryOffer, Integer> {

    @Query("select co.categoryId from CategoryOffer co where co.offerId = :offerId")
    List<Long> findCategoryIdsByOfferId(@Param("offerId") Long offerId);
}
