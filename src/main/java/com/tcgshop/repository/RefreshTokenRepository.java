package com.tcgshop.repository;

import com.tcgshop.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select token from RefreshToken token join fetch token.user where token.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Modifying
	@Query("update RefreshToken token set token.revoked = true where token.user.id = :userId")
	int revokeAllByUserId(@Param("userId") Long userId);

	@Modifying
	@Query("delete from RefreshToken token where token.expiresAt <= :now")
	int deleteExpiredBefore(@Param("now") LocalDateTime now);
}
