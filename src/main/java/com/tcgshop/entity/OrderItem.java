package com.tcgshop.entity;

import com.tcgshop.entity.enums.CardCondition;
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
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "product_name")
	private String productName;

	@Column(name = "product_image")
	private String productImage;

	@Enumerated(EnumType.STRING)
	@Column(name = "card_condition")
	private CardCondition cardCondition;

	@Column(name = "price", precision = 15, scale = 2)
	private BigDecimal price;

	@Column(name = "quantity")
	private int quantity;

	@Column(name = "total_price", precision = 15, scale = 2)
	private BigDecimal totalPrice;
}
