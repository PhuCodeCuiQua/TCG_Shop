package com.tcgshop.dto.response;

import java.math.BigDecimal;

/** label: yyyy-MM-dd (theo ngày) hoặc yyyy-MM (theo tháng). commission = null với Manager. */
public record RevenuePointResponse(String label, BigDecimal revenue, BigDecimal commission, long orderCount) {
}