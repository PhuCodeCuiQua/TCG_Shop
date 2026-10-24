package com.tcgshop.service;

import com.tcgshop.dto.request.VendorPromotionRequest;
import com.tcgshop.dto.response.VendorPromotionProductOptionResponse;
import com.tcgshop.dto.response.VendorPromotionResponse;
import com.tcgshop.entity.enums.ActiveStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorPromotionService {
	Page<VendorPromotionResponse> getCurrentShopPromotions(Pageable pageable);
	VendorPromotionResponse getCurrentShopPromotion(Long promotionId);
	VendorPromotionResponse saveCurrentShopPromotion(Long promotionId, VendorPromotionRequest request);
	VendorPromotionResponse toggleCurrentShopPromotion(Long promotionId);
	VendorPromotionResponse deactivateCurrentShopPromotion(Long promotionId);
	List<VendorPromotionProductOptionResponse> getCurrentShopProductOptions();
}
