package com.tcgshop.repository;

import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Các truy vấn chỉ phục vụ việc chọn tài khoản đăng ký dở an toàn để dọn. */
public interface AccountCleanupRepository extends Repository<User, Long> {

	List<User> findByStatusAndEmailVerifiedFalseAndCreatedAtBefore(UserStatus status, LocalDateTime cutoff);

	/**
	 * Chặn xóa nếu tài khoản còn được tham chiếu bởi dữ liệu nghiệp vụ.
	 * OTP còn hiệu lực cũng chặn xóa; user_roles do JPA xóa cùng quan hệ many-to-many khi xóa User.
	 */
	@Query(value = """
			SELECT (
				NOT EXISTS (SELECT 1 FROM otp_tokens WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM addresses WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM orders WHERE user_id = :userId OR shipper_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM order_groups WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM order_status_histories WHERE changed_by = :userId) AND
				NOT EXISTS (SELECT 1 FROM carts WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM wishlists WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM viewed_products WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM notifications WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM refresh_tokens WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM reviews WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM return_requests WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM coupon_usages WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM price_alerts WHERE user_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM shops WHERE owner_id = :userId) AND
				NOT EXISTS (SELECT 1 FROM commission_settings WHERE updated_by = :userId)
			) AS no_related_data
			""", nativeQuery = true)
	boolean hasNoRelatedData(@Param("userId") Long userId);
}
