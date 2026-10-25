package com.tcgshop.service;

import com.tcgshop.dto.response.PriceChartResponse;

public interface PriceChartService {
	/** range chỉ nhận 7, 30 hoặc 90; sai thì BadRequestException, thẻ không tồn tại thì ResourceNotFoundException. */
	PriceChartResponse getChart(Long cardId, int range);
}