package com.realestate.admin.repository;

import com.realestate.admin.entity.ServiceProviderSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceProviderSubscriptionRepository extends JpaRepository<ServiceProviderSubscription, Integer> {
    Optional<ServiceProviderSubscription> findFirstByOfferIdOrderByIdDesc(Integer offerId);
}
