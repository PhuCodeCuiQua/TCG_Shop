package com.tcgshop.entity;

import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cards",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_cards_series_number_language",
						columnNames = {"series_id", "card_number", "language"})
		},
		indexes = {
				@Index(name = "idx_cards_name", columnList = "name"),
				@Index(name = "idx_cards_card_number", columnList = "card_number")
		})
@Getter
@Setter
@NoArgsConstructor
public class Card extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "series_id")
	private Series series;

	@Column(name = "name")
	private String name;

	@Column(name = "card_number")
	private String cardNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "rarity")
	private Rarity rarity;

	@Enumerated(EnumType.STRING)
	@Column(name = "language")
	private CardLanguage language;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(name = "image_public_id")
	private String imagePublicId;

	@Column(name = "market_price", precision = 15, scale = 2)
	private BigDecimal marketPrice;

	@Column(name = "active")
	private boolean active = true;
}
