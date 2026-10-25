package com.tcgshop.service;

import com.tcgshop.dto.response.VendorReturnResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendorReturnService {
	Page<VendorReturnResponse> getPendingReturns(Pageable pageable);

	void approve(Long orderId);

	void reject(Long orderId, String reason);
}
