package com.tcgshop.repository;

import com.tcgshop.entity.OtpToken;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Truy vấn xóa OTP đã dùng hoặc hết hạn quá ngưỡng lưu giữ. */
public interface OtpTokenCleanupRepository extends Repository<OtpToken, Long> {

	@Modifying
	@Query("delete from OtpToken token where token.used = true or token.expiresAt <= :cutoff")
	int deleteUsedOrExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
