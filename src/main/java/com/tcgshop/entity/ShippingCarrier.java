package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shipping_carriers")
@Getter
@Setter
@NoArgsConstructor
public class ShippingCarrier extends BaseEntity {

	@Column(name = "name", unique = true)
	private String name;

	@Column(name = "code", unique = true)
	private String code;

	@Column(name = "base_fee", precision = 15, scale = 2)
	private BigDecimal baseFee;

	@Column(name = "active")
	private boolean active;

	@Column(name = "logo_url")
	private String logoUrl;

	@Column(name = "free_ship_threshold", precision = 15, scale = 2)
	private BigDecimal freeShipThreshold;
}
