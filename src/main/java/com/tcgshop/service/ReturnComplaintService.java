package com.tcgshop.service;

import com.tcgshop.dto.response.ReturnComplaintDetailResponse;
import com.tcgshop.dto.response.ReturnComplaintResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReturnComplaintService {
	/** Đơn RETURN_REJECTED mà người mua đã khiếu nại (ReturnRequest = ESCALATED). */
	Page<ReturnComplaintResponse> listComplaints(Pageable pageable);

	/** Đơn RETURNED đang chờ hoàn tiền. */
	Page<ReturnComplaintResponse> listRefundable(Pageable pageable);

	ReturnComplaintDetailResponse getDetail(Long orderId);

	void acceptComplaint(Long orderId, String note);

	/** Không đổi trạng thái đơn, chỉ cập nhật bản ghi khiếu nại. */
	void rejectComplaint(Long orderId, String note);

	void refund(Long orderId, String note);
}