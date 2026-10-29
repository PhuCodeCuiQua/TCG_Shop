package com.tcgshop.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VendorProductProjection {

	Long getId();

	String getName();

	String getType();

	BigDecimal getPrice();

	Integer getStock();

	String getStatus();

	String getImageUrl();

	LocalDateTime getCreatedAt();
}
