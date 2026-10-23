package com.tcgshop.service;

import com.tcgshop.dto.request.PriceHistoryEditRequest;
import com.tcgshop.dto.request.PriceHistoryRequest;
import com.tcgshop.dto.response.CardPriceOptionResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PriceHistoryResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PriceHistoryService {

	List<CardPriceOptionResponse> getActiveCards();

	PageResponse<PriceHistoryResponse> search(
			Long cardId, LocalDate startDate, LocalDate endDate, Pageable pageable);

	PriceHistoryResponse getById(Long historyId);

	PriceHistoryResponse save(PriceHistoryRequest request);

	PriceHistoryResponse update(Long historyId, PriceHistoryEditRequest request);

	void delete(Long historyId);
}
