package com.tcgshop.entity;

import com.tcgshop.entity.enums.OtpType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "otp_tokens")
@Getter
@Setter
@NoArgsConstructor
public class OtpToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private OtpType type;

	@Column(name = "code_hash", nullable = false)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "attempts", nullable = false)
	private int attempts = 0;

	@Column(name = "resend_count", nullable = false)
	private int resendCount = 0;

	@Column(name = "last_sent_at")
	private LocalDateTime lastSentAt;

	@Column(name = "used", nullable = false)
	private boolean used = false;
}
