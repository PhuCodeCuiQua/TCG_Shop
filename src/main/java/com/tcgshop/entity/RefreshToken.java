package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens", indexes = {
		@Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash")
})
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "token_hash", unique = true, length = 128)
	private String tokenHash;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "revoked")
	private boolean revoked = false;
}
