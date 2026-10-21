package com.tcgshop.entity;

import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.PromotionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
public class Promotion extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shop_id")
	private Shop shop;

	@Column(name = "title")
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private PromotionType type;

	@Column(name = "discount_percent", precision = 5, scale = 2)
	private BigDecimal discountPercent;

	@Column(name = "discount_amount", precision = 15, scale = 2)
	private BigDecimal discountAmount;

	@Column(name = "min_order_value", precision = 15, scale = 2)
	private BigDecimal minOrderValue;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "game_id")
	private Game game;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "series_id")
	private Series series;

	@Column(name = "start_date")
	private LocalDateTime startDate;

	@Column(name = "end_date")
	private LocalDateTime endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ActiveStatus status;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "promotion_products",
			joinColumns = @JoinColumn(name = "promotion_id"),
			inverseJoinColumns = @JoinColumn(name = "product_id"))
	private Set<Product> products = new HashSet<>();
}
