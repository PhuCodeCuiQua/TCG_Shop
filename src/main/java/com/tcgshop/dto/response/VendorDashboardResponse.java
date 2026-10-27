package com.tcgshop.dto.response;

import java.math.BigDecimal;

public record VendorDashboardResponse(long activeProducts, long newOrders, BigDecimal monthlyRevenue) { }
