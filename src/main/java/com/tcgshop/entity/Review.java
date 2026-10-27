package com.tcgshop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviews", uniqueConstraints = {
		@UniqueConstraint(name = "uk_reviews_order_product", columnNames = {"order_id", "product_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Review extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private Order order;

	@Column(name = "rating")
	private int rating;

	@Column(name = "comment", columnDefinition = "TEXT")
	private String comment;

	@Column(name = "deleted")
	private boolean deleted = false;

	@Column(name = "vendor_reply", columnDefinition = "TEXT")
	private String vendorReply;

	@Column(name = "replied_at")
	private LocalDateTime repliedAt;

	@OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<ReviewMedia> media = new ArrayList<>();
}
