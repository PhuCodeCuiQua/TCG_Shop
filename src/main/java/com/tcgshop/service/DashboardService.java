package com.tcgshop.service;

import com.tcgshop.dto.response.DashboardSummaryResponse;
import com.tcgshop.dto.response.OrderStatusCountResponse;
import com.tcgshop.dto.response.RevenuePointResponse;
import com.tcgshop.dto.response.TopProductResponse;
import com.tcgshop.dto.response.TopShopResponse;
import java.time.LocalDate;
import java.util.List;

/** from/to null thì lấy 30 ngày gần nhất. Hoa hồng chỉ trả về cho Admin. */
public interface DashboardService {
	DashboardSummaryResponse getSummary(LocalDate from, LocalDate to);

	/** group = "day" (mặc định) hoặc "month". */
	List<RevenuePointResponse> getRevenueSeries(LocalDate from, LocalDate to, String group);

	List<OrderStatusCountResponse> getOrdersByStatus(LocalDate from, LocalDate to);

	List<TopShopResponse> getTopShops(LocalDate from, LocalDate to, int limit);

	List<TopProductResponse> getTopProducts(LocalDate from, LocalDate to, int limit);
}