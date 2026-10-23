package com.tcgshop.service;

import com.tcgshop.dto.request.CommissionRateRequest;
import com.tcgshop.dto.response.CommissionSettingResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShopCommissionResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;

public interface CommissionService {
	BigDecimal getRate(Long shopId);

	BigDecimal getDefaultRate();

	PageResponse<CommissionSettingResponse> getSettingsHistory(Pageable pageable);

	PageResponse<ShopCommissionResponse> getShopCommissionRates(Pageable pageable);

	ShopCommissionResponse getShopCommissionRate(Long shopId);

	void addDefaultRate(CommissionRateRequest request);

	void setShopRate(Long shopId, CommissionRateRequest request);

	void resetShopRate(Long shopId);
}
