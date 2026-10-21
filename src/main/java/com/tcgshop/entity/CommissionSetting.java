package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "commission_settings")
@Getter
@Setter
@NoArgsConstructor
public class CommissionSetting extends BaseEntity {

	@Column(name = "default_rate", precision = 5, scale = 2)
	private BigDecimal defaultRate = new BigDecimal("5.00");

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_by")
	private User updatedBy;
}
