package com.tcgshop.entity;

import com.tcgshop.entity.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications", indexes = {
		@Index(name = "idx_notifications_user_is_read", columnList = "user_id, is_read")
})
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "title")
	private String title;

	@Column(name = "content", columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", length = 50, columnDefinition = "VARCHAR(50)")
	private NotificationType type;

	@Column(name = "is_read")
	private boolean isRead = false;

	@Column(name = "reference_id", length = 100)
	private String referenceId;

	@Column(name = "link", length = 255)
	private String link;
}
