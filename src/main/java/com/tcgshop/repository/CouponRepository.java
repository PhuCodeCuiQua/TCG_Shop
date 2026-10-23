package com.tcgshop.repository;

import com.tcgshop.entity.Coupon;
import com.tcgshop.entity.enums.ActiveStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

	Optional<Coupon> findByCode(String code);

	boolean existsByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

	@Query(value = """
			select c from Coupon c
			where c.shop is null and (:status is null or c.status = :status)
			""",
			countQuery = """
					select count(c) from Coupon c
					where c.shop is null and (:status is null or c.status = :status)
					""")
	Page<Coupon> searchPlatformCoupons(@Param("status") ActiveStatus status, Pageable pageable);

	@Query("select c from Coupon c where c.id = :id and c.shop is null")
	Optional<Coupon> findPlatformCouponById(@Param("id") Long id);
}
