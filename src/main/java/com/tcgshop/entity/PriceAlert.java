package com.tcgshop.entity;

import com.tcgshop.entity.enums.AlertCondition;
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
@Table(name = "price_alerts")
@Getter
@Setter
@NoArgsConstructor
public class PriceAlert extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "card_id")
	private Card card;

	@Column(name = "target_price", precision = 15, scale = 2)
	private BigDecimal targetPrice;

	@Enumerated(EnumType.STRING)
	@Column(name = "alert_condition")
	private AlertCondition alertCondition;

	@Column(name = "is_triggered")
	private boolean isTriggered = false;
}
