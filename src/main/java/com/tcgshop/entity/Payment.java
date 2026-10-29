package com.tcgshop.entity;

import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.TransactionStatus;
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
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_group_id")
	private OrderGroup orderGroup;

	@Column(name = "transaction_id")
	private String transactionId;

	@Column(name = "amount", precision = 15, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method")
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private TransactionStatus status;

	@Column(name = "pay_url", columnDefinition = "TEXT")
	private String payUrl;

	@Column(name = "response_data", columnDefinition = "TEXT")
	private String responseData;
}
