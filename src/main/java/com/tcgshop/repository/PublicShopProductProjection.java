package com.tcgshop.repository;

import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;

/** Read-only repository projection for the public product grid. */
public interface PublicShopProductProjection {
	Long getId();

	String getName();

	String getDescription();

	BigDecimal getPrice();

	Integer getStock();

	ProductType getType();

	String getImageUrl();
}
