package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_histories", uniqueConstraints = {
		@UniqueConstraint(name = "uk_price_histories_card_recorded_date",
				columnNames = {"card_id", "recorded_date"})
})
@Getter
@Setter
@NoArgsConstructor
public class PriceHistory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "card_id")
	private Card card;

	@Column(name = "recorded_date")
	private LocalDate recordedDate;

	@Column(name = "market_price", precision = 15, scale = 2)
	private BigDecimal marketPrice;

	@Column(name = "avg_price", precision = 15, scale = 2)
	private BigDecimal avgPrice;

	@Column(name = "source", length = 10)
	private String source = "ADMIN";
}
