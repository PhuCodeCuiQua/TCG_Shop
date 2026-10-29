package com.tcgshop.repository;

import com.tcgshop.entity.enums.CardCondition;
import java.math.BigDecimal;

public interface OrderItemRow {
	Long getOrderId();
	Long getItemId();
	Long getProductId();
	String getProductName();
	String getProductImage();
	CardCondition getCardCondition();
	Integer getQuantity();
	BigDecimal getPrice();
	BigDecimal getTotalPrice();
}
