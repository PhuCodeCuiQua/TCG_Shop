package com.tcgshop.entity;

import com.tcgshop.entity.enums.ReturnStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "return_requests", uniqueConstraints = {
		@UniqueConstraint(name = "uk_return_requests_order_id", columnNames = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequest extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", unique = true)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "reason", columnDefinition = "TEXT")
	private String reason;

	@Column(name = "proof_image_url")
	private String proofImageUrl;

	@Column(name = "proof_image_public_id")
	private String proofImagePublicId;

	@Column(name = "proof_video_url")
	private String proofVideoUrl;

	@Column(name = "proof_video_public_id")
	private String proofVideoPublicId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ReturnStatus status = ReturnStatus.PENDING;

	@Column(name = "vendor_note", columnDefinition = "TEXT")
	private String vendorNote;

	@Column(name = "admin_note", columnDefinition = "TEXT")
	private String adminNote;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;
}
