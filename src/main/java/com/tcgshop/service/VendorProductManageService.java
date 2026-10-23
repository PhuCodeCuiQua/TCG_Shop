package com.tcgshop.service;

import com.tcgshop.dto.request.VendorProductEditRequest;
import com.tcgshop.dto.response.VendorProductActionResponse;
import com.tcgshop.dto.response.VendorProductEditResponse;

public interface VendorProductManageService {
	VendorProductEditResponse getOwnedProduct(Long productId);
	void updateOwnedProduct(Long productId, VendorProductEditRequest request);
	VendorProductActionResponse toggleOwnedProduct(Long productId);
	VendorProductActionResponse softDeleteOwnedProduct(Long productId);
}
