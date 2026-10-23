package com.tcgshop.service;

import com.tcgshop.dto.request.CheckoutPreviewRequest;
import com.tcgshop.dto.response.CheckoutPreviewResponse;

public interface CheckoutService {
	CheckoutPreviewResponse preview(CheckoutPreviewRequest request);
}
