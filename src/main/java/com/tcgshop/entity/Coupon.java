package com.tcgshop.entity;

import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shop_id")
	private Shop shop;

	@Column(name = "code", unique = true)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type")
	private DiscountType discountType;

	@Column(name = "discount_value", precision = 15, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "min_order_value", precision = 15, scale = 2)
	private BigDecimal minOrderValue = BigDecimal.ZERO;

	@Column(name = "max_discount_amount", precision = 15, scale = 2)
	private BigDecimal maxDiscountAmount;

	@Column(name = "total_quantity")
	private Integer totalQuantity;

	@Column(name = "used_quantity")
	private int usedQuantity = 0;

	@Column(name = "limit_per_user")
	private int limitPerUser = 1;

	@Column(name = "start_date")
	private LocalDateTime startDate;

	@Column(name = "end_date")
	private LocalDateTime endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ActiveStatus status = ActiveStatus.ACTIVE;
}
