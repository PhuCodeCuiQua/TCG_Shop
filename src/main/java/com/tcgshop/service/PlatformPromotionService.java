package com.tcgshop.service;

import com.tcgshop.dto.request.PlatformPromotionRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.PlatformPromotionResponse;
import com.tcgshop.dto.response.PromotionTargetOptionResponse;
import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.PromotionType;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PlatformPromotionService {

	PageResponse<PlatformPromotionResponse> search(PromotionType type, ActiveStatus status, Pageable pageable);

	PlatformPromotionResponse getById(Long promotionId);

	PlatformPromotionResponse save(Long promotionId, PlatformPromotionRequest request);

	PlatformPromotionResponse setStatus(Long promotionId, ActiveStatus status);

	List<PromotionTargetOptionResponse> getGameOptions();

	List<PromotionTargetOptionResponse> getSeriesOptions();

	List<PromotionTargetOptionResponse> getSeriesOptions(Long gameId);

	List<PromotionTargetOptionResponse> getProductOptions();

	List<PromotionTargetOptionResponse> getProductOptions(Long seriesId);
}
