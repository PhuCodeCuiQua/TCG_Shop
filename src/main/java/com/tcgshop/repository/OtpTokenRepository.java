package com.tcgshop.repository;

import com.tcgshop.entity.OtpToken;
import com.tcgshop.entity.enums.OtpPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

	Optional<OtpToken> findTopByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);
}
