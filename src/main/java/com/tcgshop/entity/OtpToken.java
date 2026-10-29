package com.tcgshop.entity;

import com.tcgshop.entity.enums.OtpPurpose;
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
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose")
	private OtpPurpose purpose;

	@Column(name = "otp_hash")
	private String otpHash;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "attempt_count")
	private int attemptCount = 0;

	@Column(name = "used")
	private boolean used = false;
}
