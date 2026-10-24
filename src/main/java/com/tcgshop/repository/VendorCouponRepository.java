package com.tcgshop.repository;

import com.tcgshop.entity.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Shop-scoped coupon queries; platform coupon administration keeps using CouponRepository. */
public interface VendorCouponRepository extends JpaRepository<Coupon, Long> {
	Page<Coupon> findAllByShopId(Long shopId, Pageable pageable);

	@Query("select count(u) from CouponUsage u where u.coupon.id = :couponId and u.order.id = :orderId")
	long countUsageForCouponAndOrder(@Param("couponId") Long couponId, @Param("orderId") Long orderId);

	@Query("select c from Coupon c join fetch c.shop where c.id = :couponId")
	Optional<Coupon> findByIdWithShop(@Param("couponId") Long couponId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Coupon c left join fetch c.shop where c.id = :couponId")
	Optional<Coupon> findByIdForUpdate(@Param("couponId") Long couponId);
}
