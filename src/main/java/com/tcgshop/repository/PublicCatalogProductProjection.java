package com.tcgshop.repository;

import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;

public interface PublicCatalogProductProjection {
	Long getId();

	Long getShopId();

	String getShopName();

	String getName();

	String getCardName();

	String getCardNumber();

	ProductType getType();

	CardCondition getCardCondition();

	BigDecimal getPrice();

	Integer getStock();

	Integer getSoldCount();

	String getImageUrl();
}
