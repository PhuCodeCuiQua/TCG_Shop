package com.tcgshop.service;

import com.tcgshop.dto.response.ChatbotPriceOfferResponse;
import java.math.BigDecimal;
import java.util.List;

public interface ChatbotPriceSearchService {
	List<ChatbotPriceOfferResponse> search(BigDecimal minPrice, BigDecimal maxPrice,
			BigDecimal targetPrice, int limit);
}
