package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "receiver_name", nullable = false)
	private String receiverName;

	@Column(name = "phone", nullable = false)
	private String phone;

	@Column(name = "province", nullable = false)
	private String province;

	@Column(name = "district", nullable = false)
	private String district;

	@Column(name = "ward", nullable = false)
	private String ward;

	@Column(name = "detail", nullable = false)
	private String detail;

	@Column(name = "is_default", nullable = false)
	private boolean isDefault = false;
}
