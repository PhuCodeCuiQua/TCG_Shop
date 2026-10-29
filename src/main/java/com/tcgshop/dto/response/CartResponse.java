package com.tcgshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
		List<CartShopGroupResponse> shops,
		long totalItemCount,
		long selectedItemCount,
		BigDecimal selectedSubtotal,
		boolean hasUnavailableItems) { }
