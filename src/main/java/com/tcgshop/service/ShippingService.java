package com.tcgshop.service;

import com.tcgshop.dto.request.ShippingCarrierRequest;
import com.tcgshop.dto.response.AdminShippingCarrierResponse;
import com.tcgshop.dto.response.ShippingCarrierResponse;
import java.math.BigDecimal;
import java.util.List;

public interface ShippingService {
	List<ShippingCarrierResponse> getActiveCarriers();

	BigDecimal calculateFee(Long carrierId, BigDecimal orderSubtotal);

	List<AdminShippingCarrierResponse> getAllForAdmin();

	AdminShippingCarrierResponse getForAdmin(Long carrierId);

	AdminShippingCarrierResponse create(ShippingCarrierRequest request);

	AdminShippingCarrierResponse update(Long carrierId, ShippingCarrierRequest request);

	boolean removeOrHide(Long carrierId);
}
