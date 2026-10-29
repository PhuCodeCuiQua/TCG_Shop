package com.tcgshop.service;

import com.tcgshop.dto.request.VendorSingleProductRequest;
import com.tcgshop.dto.request.VendorCustomCardRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.dto.response.VendorSingleProductResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorSingleProductService {

	Page<CardListingResponse> searchCards(String keyword, Long seriesId, Pageable pageable);

	CardListingResponse getListingCard(Long cardId);

	CardListingResponse getPendingListingCard(Long cardId);

	CardListingResponse createPendingCard(VendorCustomCardRequest request);

	List<SeriesResponse> getActiveSeriesOptions();

	VendorSingleProductResponse createListing(VendorSingleProductRequest request);

	VendorSingleProductResponse createListingFromPendingCard(VendorSingleProductRequest request);
}
