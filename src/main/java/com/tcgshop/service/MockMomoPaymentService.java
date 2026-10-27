package com.tcgshop.service;

import com.tcgshop.dto.response.MockMomoPaymentResponse;

public interface MockMomoPaymentService {
	MockMomoPaymentResponse confirm(String groupCode);
	MockMomoPaymentResponse cancel(String groupCode);
}
