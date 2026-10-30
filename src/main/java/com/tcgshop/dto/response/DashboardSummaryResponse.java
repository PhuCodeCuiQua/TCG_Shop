package com.tcgshop.dto.response;

import java.math.BigDecimal;

/** commission giữ tương thích Admin; totalCommission là tổng snapshot cho báo cáo Manager/Admin. */
public record DashboardSummaryResponse(
		long totalUsers,
		long totalShops,
		long totalOrders,
		BigDecimal revenue,
		BigDecimal commission,
		BigDecimal totalCommission) {
}