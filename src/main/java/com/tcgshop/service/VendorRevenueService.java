package com.tcgshop.service;

import com.tcgshop.dto.response.VendorRevenueReportResponse;
import com.tcgshop.dto.response.VendorRevenueChartResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public interface VendorRevenueService {
	VendorRevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate, Pageable pageable);

	VendorRevenueChartResponse getRevenueCharts(LocalDate fromDate, LocalDate toDate, String groupBy);
}
