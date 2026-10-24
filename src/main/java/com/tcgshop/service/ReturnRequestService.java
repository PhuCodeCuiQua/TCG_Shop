package com.tcgshop.service;

import com.tcgshop.dto.request.CreateReturnRequest;
import com.tcgshop.dto.request.EscalateReturnRequest;
import com.tcgshop.dto.response.ReturnRequestInfoResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ReturnRequestService {
	ReturnRequestInfoResponse createReturnRequest(Long orderId, CreateReturnRequest request,
			MultipartFile image, MultipartFile video);

	void escalate(Long orderId, EscalateReturnRequest request);

	void rejectEscalation(Long orderId, String note);
}
