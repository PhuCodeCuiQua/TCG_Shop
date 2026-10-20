package com.tcgshop.repository;

import com.tcgshop.entity.OtpToken;
import com.tcgshop.entity.enums.OtpType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

	Optional<OtpToken> findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(Long userId, OtpType type);
}
