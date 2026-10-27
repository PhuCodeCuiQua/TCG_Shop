package com.tcgshop.service.impl;

import com.tcgshop.dto.request.AddToCartRequest;
import com.tcgshop.dto.response.MockMomoPaymentResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderGroup;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderItemQuantity;
import com.tcgshop.repository.OrderGroupRepository;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CartService;
import com.tcgshop.service.MockMomoPaymentService;
import com.tcgshop.service.OrderService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockMomoPaymentServiceImpl implements MockMomoPaymentService {
	private final OrderGroupRepository orderGroupRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final CurrentUser currentUser;
	private final OrderService orderService;
	private final CartService cartService;
	private final Clock clock;

	public MockMomoPaymentServiceImpl(OrderGroupRepository orderGroupRepository, OrderRepository orderRepository,
			OrderItemRepository orderItemRepository, CurrentUser currentUser, OrderService orderService,
			CartService cartService, Clock clock) {
		this.orderGroupRepository = orderGroupRepository;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.currentUser = currentUser;
		this.orderService = orderService;
		this.cartService = cartService;
		this.clock = clock;
	}

	@Override
	@Transactional
	public MockMomoPaymentResponse confirm(String groupCode) {
		OrderGroup group = findOwnedMomoGroup(groupCode);
		if (group.getPaymentStatus() == PaymentStatus.PAID) {
			return response(group, PaymentStatus.PAID);
		}
		if (group.getPaymentStatus() != PaymentStatus.PENDING) {
			throw new BusinessException("Giao dịch MoMo này đã bị hủy hoặc không còn chờ thanh toán");
		}
		group.setPaymentStatus(PaymentStatus.PAID);
		group.setPaidAt(LocalDateTime.now(clock));
		orderGroupRepository.save(group);
		return response(group, PaymentStatus.PAID);
	}

	@Override
	@Transactional
	public MockMomoPaymentResponse cancel(String groupCode) {
		OrderGroup group = findOwnedMomoGroup(groupCode);
		if (group.getPaymentStatus() == PaymentStatus.FAILED) {
			return response(group, PaymentStatus.FAILED);
		}
		if (group.getPaymentStatus() != PaymentStatus.PENDING) {
			throw new BusinessException("Không thể hủy giao dịch MoMo đã thanh toán");
		}

		group.setPaymentStatus(PaymentStatus.FAILED);
		orderGroupRepository.save(group);
		List<Order> orders = orderRepository.findByOrderGroupIdOrderByIdAsc(group.getId());
		for (Order order : orders) {
			if (order.getStatus() == OrderStatus.NEW) {
				orderService.changeStatus(order.getId(), OrderStatus.CANCELLED, "Khách hủy thanh toán MoMo giả lập");
			}
			for (OrderItemQuantity quantity : orderItemRepository.findQuantitiesByOrderId(order.getId())) {
				cartService.addItem(new AddToCartRequest(quantity.getProductId(), Math.toIntExact(quantity.getQuantity())));
			}
		}
		return response(group, PaymentStatus.FAILED);
	}

	private OrderGroup findOwnedMomoGroup(String groupCode) {
		OrderGroup group = orderGroupRepository.findByGroupCodeAndUserIdForUpdate(groupCode, currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Nhóm đơn hàng", groupCode));
		if (group.getPaymentMethod() != PaymentMethod.MOMO) {
			throw new BusinessException("Nhóm đơn hàng này không sử dụng MoMo");
		}
		return group;
	}

	private MockMomoPaymentResponse response(OrderGroup group, PaymentStatus status) {
		String redirectUrl = status == PaymentStatus.PAID ? "/checkout/success/" + group.getGroupCode()
				: "/checkout?paymentCancelled=true";
		return new MockMomoPaymentResponse(status, redirectUrl);
	}
}
