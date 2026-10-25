package com.tcgshop.repository;

import java.math.BigDecimal;

public interface DeliveredCardSales {
	Long getCardId();

	BigDecimal getSalesAmount();

	Long getQuantity();
}
