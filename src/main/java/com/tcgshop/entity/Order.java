package com.tcgshop.entity;

import com.tcgshop.entity.enums.OrderStatus;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders", indexes = {
		@Index(name = "idx_orders_user_status", columnList = "user_id, status"),
		@Index(name = "idx_orders_shop_status", columnList = "shop_id, status"),
		@Index(name = "idx_orders_shipper_status", columnList = "shipper_id, status"),
		@Index(name = "idx_orders_order_code", columnList = "order_code")
})
@Getter
@Setter
@NoArgsConstructor
public class Order extends BaseEntity {

	@Column(name = "order_code", unique = true)
	private String orderCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_group_id", nullable = false)
	private OrderGroup orderGroup;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shop_id", nullable = false)
	private Shop shop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipper_id")
	private User shipper;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipping_carrier_id")
	private ShippingCarrier shippingCarrier;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private OrderStatus status = OrderStatus.NEW;

	@Column(name = "subtotal", precision = 15, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "shipping_fee", precision = 15, scale = 2)
	private BigDecimal shippingFee;

	@Column(name = "discount_amount", precision = 15, scale = 2)
	private BigDecimal discountAmount;

	@Column(name = "platform_discount_amount", precision = 15, scale = 2)
	private BigDecimal platformDiscountAmount;

	@Column(name = "total_amount", precision = 15, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "refund_amount", precision = 15, scale = 2)
	private BigDecimal refundAmount;

	@Column(name = "refunded_at")
	private LocalDateTime refundedAt;

	@Column(name = "commission_rate", precision = 5, scale = 2)
	private BigDecimal commissionRate;

	@Column(name = "commission_amount", precision = 15, scale = 2)
	private BigDecimal commissionAmount;

	@Column(name = "shop_revenue", precision = 15, scale = 2)
	private BigDecimal shopRevenue;

	@Column(name = "receiver_name")
	private String receiverName;

	@Column(name = "receiver_phone")
	private String receiverPhone;

	@Column(name = "shipping_address", columnDefinition = "TEXT")
	private String shippingAddress;

	@Column(name = "note")
	private String note;

	@Column(name = "cancel_reason", length = 255)
	private String cancelReason;

	@Column(name = "cod_collected")
	private boolean codCollected = false;

	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;

	@Version
	@Column(name = "version")
	private Long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
	private List<OrderStatusHistory> statusHistories = new ArrayList<>();
}
