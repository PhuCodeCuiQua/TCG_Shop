package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartShopGroupResponse(
		Long shopId,
		String shopName,
		List<CartItemResponse> items,
		BigDecimal selectedSubtotal,
		boolean allSelected) { }
