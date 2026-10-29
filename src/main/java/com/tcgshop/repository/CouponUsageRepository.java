package com.tcgshop.repository;

import com.tcgshop.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

	long countByCouponIdAndUserId(Long couponId, Long userId);
}
