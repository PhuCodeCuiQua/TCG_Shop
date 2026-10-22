package com.tcgshop.service;

import com.tcgshop.dto.response.ShippingCarrierResponse;
import java.math.BigDecimal;
import java.util.List;

public interface ShippingService {
	List<ShippingCarrierResponse> getActiveCarriers();

	BigDecimal calculateFee(Long carrierId, BigDecimal orderSubtotal);
}
