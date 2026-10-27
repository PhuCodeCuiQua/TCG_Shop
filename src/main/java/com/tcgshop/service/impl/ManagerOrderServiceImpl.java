package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ManagerOrderDetailResponse;
import com.tcgshop.dto.response.ManagerOrderSummaryResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.ManagerOrderMapper;
import com.tcgshop.repository.ManagerOrderRepository;
import com.tcgshop.service.ManagerOrderService;
import com.tcgshop.util.OrderStatusUi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ManagerOrderServiceImpl implements ManagerOrderService {
	private static final int MAX_KEYWORD_LENGTH = 100;
	private final ManagerOrderRepository repository;
	private final ManagerOrderMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public Page<ManagerOrderSummaryResponse> searchOrders(String keyword, OrderStatus status, Pageable pageable) {
		String normalized = keyword == null ? null : keyword.trim();
		if (normalized != null && normalized.isEmpty()) normalized = null;
		if (normalized != null && normalized.length() > MAX_KEYWORD_LENGTH) {
			normalized = normalized.substring(0, MAX_KEYWORD_LENGTH);
		}
		return repository.searchAllOrders(status, normalized, pageable).map(this::toSummary);
	}

	@Override
	@Transactional(readOnly = true)
	public ManagerOrderDetailResponse getOrderDetail(Long orderId) {
		Order order = repository.findDetailById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
		List<ManagerOrderDetailResponse.Item> items = order.getItems().stream().map(mapper::toItem).toList();
		return new ManagerOrderDetailResponse(order.getId(), order.getOrderCode(), order.getCreatedAt(),
				order.getStatus(), order.getStatus() == null ? "Không xác định" : order.getStatus().getDisplayName(),
				order.getStatus() == null ? "secondary" : OrderStatusUi.color(order.getStatus()),
				order.getShop() == null ? null : order.getShop().getName(),
				order.getUser() == null ? null : order.getUser().getFullName(),
				order.getUser() == null ? null : order.getUser().getEmail(), order.getReceiverName(),
				order.getReceiverPhone(), order.getShippingAddress(), order.getNote(),
				order.getOrderGroup() == null || order.getOrderGroup().getPaymentMethod() == null ? null
						: order.getOrderGroup().getPaymentMethod().name(),
				order.getOrderGroup() == null || order.getOrderGroup().getPaymentStatus() == null ? null
						: order.getOrderGroup().getPaymentStatus().name(),
				order.getSubtotal(), order.getDiscountAmount(), order.getShippingFee(), order.getTotalAmount(),
				items, repository.findTimeline(orderId));
	}

	private ManagerOrderSummaryResponse toSummary(Order order) {
		return mapper.toSummary(order);
	}

}
