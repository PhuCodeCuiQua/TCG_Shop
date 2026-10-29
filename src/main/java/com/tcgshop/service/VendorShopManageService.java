package com.tcgshop.service;

import com.tcgshop.dto.request.VendorShopManageRequest;
import com.tcgshop.dto.response.VendorShopManageResponse;
import java.util.Optional;

public interface VendorShopManageService {

	Optional<VendorShopManageResponse> getCurrentVendorShop();

	VendorShopManageResponse updateCurrentVendorShop(VendorShopManageRequest request);
}
