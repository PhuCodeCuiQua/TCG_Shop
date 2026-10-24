package com.tcgshop.service;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PublicProductSearchItemResponse;
import com.tcgshop.dto.response.PublicProductSearchOptionsResponse;

public interface PublicProductSearchService {

	PageResponse<PublicProductSearchItemResponse> search(PublicProductSearchRequest filters);

	PublicProductSearchOptionsResponse getFilterOptions(Long gameId);
}
