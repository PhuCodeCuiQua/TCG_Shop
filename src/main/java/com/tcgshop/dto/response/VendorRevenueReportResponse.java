package com.tcgshop.dto.response;

import java.time.LocalDate;

public record VendorRevenueReportResponse(
		VendorRevenueSummaryResponse summary,
		PageResponse<VendorRevenueEntryResponse> orders,
		LocalDate fromDate,
		LocalDate toDate) {
}
