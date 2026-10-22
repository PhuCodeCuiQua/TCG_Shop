package com.tcgshop.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.tcgshop.entity.OtpToken;
import com.tcgshop.entity.enums.OtpPurpose;

/** Truy vấn OTP chưa dùng; tách riêng để không sửa repository dùng chung đã có. */
public interface OtpTokenQueryRepository extends Repository<OtpToken, Long> {
	List<OtpToken> findByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);
}
