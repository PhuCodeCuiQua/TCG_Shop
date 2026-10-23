package com.tcgshop.service;

import com.tcgshop.dto.request.ShopRegistrationRequest;
import com.tcgshop.dto.response.ShopRegistrationResponse;
import java.util.Optional;

public interface ShopRegistrationService {

	Optional<ShopRegistrationResponse> getCurrentUserShop();

	ShopRegistrationResponse registerOrResubmit(ShopRegistrationRequest request);
}
