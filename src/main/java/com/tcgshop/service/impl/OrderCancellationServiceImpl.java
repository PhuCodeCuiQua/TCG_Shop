package com.tcgshop.service.impl;

import com.tcgshop.dto.request.CancelOrderRequest;
import com.tcgshop.dto.response.CancelOrderResultResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.OrderCancellationService;
import com.tcgshop.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giao dịch hủy được đặt ở service riêng vì UserOrderService là read-only ở cấp lớp;
 * gọi đổi trạng thái bên trong giao dịch chỉ đọc có thể khiến Hibernate không ghi dữ liệu.
 */
@Slf4j
@Service
@Transactional
public class OrderCancellationServiceImpl implements OrderCancellationService {
	private final OrderRepository orderRepository;
	private final CurrentUser currentUser;
	private final OrderTransitionPolicy transitionPolicy;
	private final OrderService orderService;

	public OrderCancellationServiceImpl(OrderRepository orderRepository, CurrentUser currentUser,
			OrderTransitionPolicy transitionPolicy, OrderService orderService) {
		this.orderRepository = orderRepository;
		this.currentUser = currentUser;
		this.transitionPolicy = transitionPolicy;
		this.orderService = orderService;
	}

	@Override
	public CancelOrderResultResponse cancelMyOrder(Long orderId, CancelOrderRequest request) {
		Order order = orderRepository.findByIdAndUserId(orderId, currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
		if (order.getStatus() == OrderStatus.CANCELLED) {
			return new CancelOrderResultResponse(order.getId(), order.getOrderCode(), true);
		}
		if (!transitionPolicy.canBuyerCancel(order.getStatus())) {
			throw new BusinessException("Đơn hàng đang ở trạng thái \"" + order.getStatus().getDisplayName()
					+ "\" nên không thể hủy. Vui lòng liên hệ cửa hàng nếu cần hỗ trợ");
		}

		String detail = normalizeDetail(request.getDetail());
		String note = detail == null ? request.getReason().getLabel()
				: request.getReason().getLabel() + ": " + detail;
		// Chỉ ghi orderId vào log vì lý do hủy có thể chứa dữ liệu cá nhân.
		orderService.changeStatus(orderId, OrderStatus.CANCELLED, note);
		log.info("Buyer cancelled orderId={}", orderId);
		return new CancelOrderResultResponse(order.getId(), order.getOrderCode(), false);
	}

	private String normalizeDetail(String detail) {
		if (detail == null) return null;
		String normalized = detail.trim().replaceAll("\\s+", " ");
		return normalized.isEmpty() ? null : normalized;
	}
}
