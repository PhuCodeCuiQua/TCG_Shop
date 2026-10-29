package com.tcgshop.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface PromotionService {
	BigDecimal getEffectivePrice(Long productId);

	Map<Long, BigDecimal> getEffectivePrices(Collection<Long> productIds);
}
