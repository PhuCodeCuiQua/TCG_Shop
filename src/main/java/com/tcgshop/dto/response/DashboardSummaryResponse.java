package com.tcgshop.dto.response;

import java.math.BigDecimal;

/** commission = null với Manager (bản rút gọn). */
public record DashboardSummaryResponse(
		long totalUsers,
		long totalShops,
		long totalOrders,
		BigDecimal revenue,
		BigDecimal commission) {
}