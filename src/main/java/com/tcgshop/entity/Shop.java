package com.tcgshop.entity;

import com.tcgshop.entity.enums.ShopStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shops")
@Getter
@Setter
@NoArgsConstructor
public class Shop extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", unique = true)
	private User owner;

	@Column(name = "name", unique = true)
	private String name;

	@Column(name = "slug", unique = true)
	private String slug;

	@Column(name = "logo_url")
	private String logoUrl;

	@Column(name = "logo_public_id")
	private String logoPublicId;

	@Column(name = "banner_url")
	private String bannerUrl;

	@Column(name = "banner_public_id")
	private String bannerPublicId;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "pickup_address")
	private String pickupAddress;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ShopStatus status = ShopStatus.PENDING;

	@Column(name = "reject_reason")
	private String rejectReason;

	@Column(name = "commission_rate", precision = 5, scale = 2)
	private BigDecimal commissionRate;

	@Column(name = "avg_rating", precision = 3, scale = 2)
	private BigDecimal avgRating;

	@Column(name = "rating_count")
	private Integer ratingCount;

	@Column(name = "deleted")
	private Boolean deleted;
}
