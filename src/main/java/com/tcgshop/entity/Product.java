package com.tcgshop.entity;

import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products", indexes = {
		@Index(name = "idx_products_shop_id", columnList = "shop_id"),
		@Index(name = "idx_products_card_id", columnList = "card_id"),
		@Index(name = "idx_products_series_id", columnList = "series_id"),
		@Index(name = "idx_products_type", columnList = "type"),
		@Index(name = "idx_products_sold_count", columnList = "sold_count")
})
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shop_id")
	private Shop shop;

	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private ProductType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "card_id")
	private Card card;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "series_id")
	private Series series;

	@Column(name = "name")
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "card_condition")
	private CardCondition cardCondition;

	@Column(name = "edition")
	private String edition;

	@Enumerated(EnumType.STRING)
	@Column(name = "language")
	private CardLanguage language;

	@Column(name = "price", precision = 15, scale = 2)
	private BigDecimal price;

	@Column(name = "stock")
	private Integer stock;

	@Column(name = "sold_count")
	private Integer soldCount;

	@Column(name = "avg_rating", precision = 3, scale = 2)
	private BigDecimal avgRating;

	@Column(name = "rating_count")
	private Integer ratingCount;

	@Column(name = "favorite_count")
	private Integer favoriteCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ProductStatus status = ProductStatus.ACTIVE;

	@Column(name = "ban_reason")
	private String banReason;

	@Column(name = "deleted")
	private Boolean deleted;

	@Version
	@Column(name = "version")
	private Long version;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<ProductImage> images = new ArrayList<>();
}
