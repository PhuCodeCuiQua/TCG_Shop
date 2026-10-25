package com.tcgshop.service.impl;

import com.tcgshop.dto.request.ShipperOrderStatusChangeRequest;
import com.tcgshop.dto.response.ShipperOrderDetailResponse;
import com.tcgshop.dto.response.ShipperOrderStatusChangeResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.service.OrderService;
import com.tcgshop.service.ShipperOrderService;
import com.tcgshop.service.ShipperOrderStatusService;
import com.tcgshop.util.OrderStatusUi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperOrderStatusServiceImpl implements ShipperOrderStatusService {
	private final ShipperOrderService shipperOrderService;
	private final OrderService orderService;

	@Override
	@Transactional
	public ShipperOrderStatusChangeResponse changeStatus(Long orderId,
			ShipperOrderStatusChangeRequest request) {
		// Dùng cùng kiểm tra ownership của PG-23 trước khi gọi state machine dùng chung.
		ShipperOrderDetailResponse order = shipperOrderService.getOrderDetail(orderId);
		OrderStatus newStatus = request.newStatus();

		if (newStatus == OrderStatus.DELIVERY_FAILED
				&& (request.note() == null || request.note().isBlank())) {
			throw new BadRequestException("Vui lòng nhập lý do giao hàng thất bại");
		}
		if (order.status() == OrderStatus.SHIPPING && newStatus == OrderStatus.DELIVERED
				&& order.codAmount() != null && !request.codCollectedConfirmed()) {
			throw new BadRequestException("Vui lòng xác nhận đã thu đủ tiền COD trước khi giao thành công");
		}

		// OrderService tự ghi timeline, áp dụng tác dụng phụ và đánh dấu COD đã thu khi DELIVERED.
		orderService.changeStatus(orderId, newStatus, request.note());
		return new ShipperOrderStatusChangeResponse(true, orderId, newStatus,
				newStatus.getDisplayName(), OrderStatusUi.color(newStatus), "Cập nhật trạng thái đơn hàng thành công");
	}
}
