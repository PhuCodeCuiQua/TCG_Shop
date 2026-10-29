package com.tcgshop.service;

import com.tcgshop.dto.response.ShipperDeliveryChartResponse;
import com.tcgshop.dto.response.ShipperStatisticsResponse;
import java.time.LocalDate;

public interface ShipperStatisticsService {
	ShipperStatisticsResponse getStatistics(LocalDate fromDate, LocalDate toDate);

	ShipperDeliveryChartResponse getDailyDeliveries(LocalDate fromDate, LocalDate toDate);
}
