package com.tcgshop.service.impl;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.dto.response.OrderDetailItemResponse;
import com.tcgshop.dto.response.OrderDetailResponse;
import com.tcgshop.dto.response.OrderItemPreviewResponse;
import com.tcgshop.dto.response.OrderListItemResponse;
import com.tcgshop.dto.response.OrderTabResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.SiblingOrderResponse;
import com.tcgshop.dto.response.StepResponse;
import com.tcgshop.dto.response.StepState;
import com.tcgshop.dto.response.TimelineEntryResponse;
import com.tcgshop.dto.response.UserOrderListResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.UserOrderTab;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.OrderItemRow;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.StatusCount;
import com.tcgshop.repository.TimelineRow;
import com.tcgshop.repository.spec.OrderSpecifications;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.UserOrderService;
import com.tcgshop.util.OrderStatusUi;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserOrderServiceImpl implements UserOrderService {
	private static final int PAGE_SIZE = 10;
	private static final int MAX_KEYWORD_LENGTH = 50;
	private static final int PREVIEW_ITEM_LIMIT = 3;
	private static final List<String> MAIN_FLOW_LABELS = List.of(
			"Đặt hàng", "Đã xác nhận", "Đã lấy hàng", "Đang giao", "Đã giao");
	private static final List<OrderStatus> MAIN_FLOW = List.of(
			OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.PICKED_UP,
			OrderStatus.SHIPPING, OrderStatus.DELIVERED);

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final CurrentUser currentUser;
	private final OrderProperties orderProperties;
	private final Clock clock;
	private final OrderTransitionPolicy transitionPolicy;

	public UserOrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
			OrderStatusHistoryRepository historyRepository, CurrentUser currentUser,
			OrderProperties orderProperties, Clock clock, OrderTransitionPolicy transitionPolicy) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.historyRepository = historyRepository;
		this.currentUser = currentUser;
		this.orderProperties = orderProperties;
		this.clock = clock;
		this.transitionPolicy = transitionPolicy;
	}

	@Override
	public UserOrderListResponse listMyOrders(String tabSlug, String keyword, int page) {
		Long userId = currentUser.getId();
		UserOrderTab selected = UserOrderTab.fromSlug(tabSlug);
		String normalizedKeyword = normalizeKeyword(keyword);

		// Một GROUP BY cho toàn bộ trạng thái giúp dựng mọi số đếm tab mà không truy vấn lặp theo từng tab.
		Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
		for (StatusCount row : orderRepository.countGroupedByStatus(userId)) {
			statusCounts.put(row.getStatus(), row.getTotal());
		}
		long allCount = statusCounts.values().stream().mapToLong(Long::longValue).sum();
		List<OrderTabResponse> tabs = new ArrayList<>(UserOrderTab.values().length);
		for (UserOrderTab tab : UserOrderTab.values()) {
			long count = tab == UserOrderTab.ALL ? allCount
					: tab.getStatuses().stream().mapToLong(status -> statusCounts.getOrDefault(status, 0L)).sum();
			tabs.add(new OrderTabResponse(tab.getSlug(), tab.getLabel(), count, tab == selected));
		}

		Specification<Order> specification = Specification.where(OrderSpecifications.belongsToUser(userId))
				.and(OrderSpecifications.statusIn(selected.getStatuses()))
				.and(OrderSpecifications.codeContains(normalizedKeyword));
		PageRequest pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		Page<Order> orderPage = orderRepository.findAll(specification, pageable);
		List<Order> orders = orderPage.getContent();
		Map<Long, List<OrderItemRow>> rowsByOrder = rowsByOrder(orders.stream().map(Order::getId).toList());
		Page<OrderListItemResponse> responsePage = orderPage.map(order -> toListItem(order,
				rowsByOrder.getOrDefault(order.getId(), List.of())));
		return new UserOrderListResponse(tabs, PageResponse.from(responsePage), selected.getSlug(), normalizedKeyword);
	}

	@Override
	public OrderDetailResponse getMyOrder(Long id) {
		Long userId = currentUser.getId();
		// 404 thống nhất cho đơn không tồn tại và đơn của người khác để không làm lộ dữ liệu riêng.
		Order order = orderRepository.findDetailByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", id));

		List<OrderItemRow> itemRows = orderItemRepository.findRowsByOrderIdIn(List.of(id));
		List<TimelineRow> timelineRows = historyRepository.findTimelineByOrderId(id);
		List<Order> siblingOrders = orderRepository.findByOrderGroupIdAndIdNotAndUserIdOrderByIdAsc(
				order.getOrderGroup().getId(), order.getId(), userId);

		return new OrderDetailResponse(order.getId(), order.getOrderCode(), order.getOrderGroup().getGroupCode(),
				order.getStatus(), order.getStatus().getDisplayName(), OrderStatusUi.color(order.getStatus()),
					order.getCreatedAt(), order.getDeliveredAt(), order.getShop().getId(), order.getShop().getName(),
					itemRows.stream().map(this::toDetailItem).toList(), order.getSubtotal(), order.getDiscountAmount(),
					order.getShippingFee(), order.getTotalAmount(),
					order.getShippingCarrier() == null ? null : order.getShippingCarrier().getName(),
					order.getReceiverName(), order.getReceiverPhone(), order.getShippingAddress(), order.getNote(),
					order.getCancelReason(), paymentMethodLabel(order.getOrderGroup().getPaymentMethod()),
					paymentLabel(order), order.getRefundAmount(), order.getRefundedAt(),
					order.getShipper() == null ? null : order.getShipper().getFullName(),
					steps(order.getStatus()), timeline(order, timelineRows),
					siblingOrders.stream().map(this::toSibling).toList(),
					canCancel(order.getStatus()), canRequestReturn(order));
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null) return null;
		String normalized = keyword.trim().replaceAll("\\s+", " ");
		if (normalized.length() > MAX_KEYWORD_LENGTH) normalized = normalized.substring(0, MAX_KEYWORD_LENGTH);
		return normalized.isEmpty() ? null : normalized;
	}

	private Map<Long, List<OrderItemRow>> rowsByOrder(List<Long> orderIds) {
		if (orderIds.isEmpty()) return Map.of();
		Map<Long, List<OrderItemRow>> rows = new LinkedHashMap<>();
		for (OrderItemRow row : orderItemRepository.findRowsByOrderIdIn(orderIds)) {
			rows.computeIfAbsent(row.getOrderId(), ignored -> new ArrayList<>()).add(row);
		}
		return rows;
	}

	private OrderListItemResponse toListItem(Order order, List<OrderItemRow> rows) {
		List<OrderItemPreviewResponse> preview = rows.stream().limit(PREVIEW_ITEM_LIMIT)
				.map(row -> new OrderItemPreviewResponse(row.getProductId(), row.getProductName(),
						row.getProductImage(), row.getQuantity(), row.getPrice())).toList();
		return new OrderListItemResponse(order.getId(), order.getOrderCode(), order.getOrderGroup().getGroupCode(),
				order.getShop().getId(), order.getShop().getName(), order.getStatus(), order.getStatus().getDisplayName(),
				OrderStatusUi.color(order.getStatus()), order.getCreatedAt(), order.getTotalAmount(), rows.size(),
				preview, Math.max(0, rows.size() - PREVIEW_ITEM_LIMIT));
	}

	private OrderDetailItemResponse toDetailItem(OrderItemRow row) {
		return new OrderDetailItemResponse(row.getProductId(), row.getProductName(), row.getProductImage(),
				row.getCardCondition() == null ? null : row.getCardCondition().getDisplayName(),
				row.getPrice(), row.getQuantity(), row.getTotalPrice());
	}

	private String paymentMethodLabel(PaymentMethod method) {
		return method == PaymentMethod.COD ? "Thanh toán khi nhận hàng (COD)" : "Ví MoMo";
	}

	private String paymentLabel(Order order) {
		if (order.getRefundAmount() != null) {
			return order.getRefundedAt() == null ? "Chờ hoàn tiền" : "Đã hoàn tiền";
		}
		if (order.getOrderGroup().getPaymentMethod() == PaymentMethod.COD) {
			return order.isCodCollected() ? "Đã thanh toán" : "Chưa thanh toán (thanh toán khi nhận hàng)";
		}
		PaymentStatus status = order.getOrderGroup().getPaymentStatus();
		if (status == null) return "Chờ thanh toán";
		return switch (status) {
			case PENDING -> "Chờ thanh toán";
			case PAID -> "Đã thanh toán";
			case FAILED -> "Thanh toán thất bại";
			case REFUND_PENDING -> "Chờ hoàn tiền";
			case REFUNDED -> "Đã hoàn tiền";
		};
	}

	private List<StepResponse> steps(OrderStatus status) {
		int currentIndex = MAIN_FLOW.indexOf(status);
		if (currentIndex < 0) return List.of();
		List<StepResponse> result = new ArrayList<>(MAIN_FLOW_LABELS.size());
		for (int index = 0; index < MAIN_FLOW_LABELS.size(); index++) {
			StepState state = status == OrderStatus.DELIVERED || index < currentIndex ? StepState.DONE
					: index == currentIndex ? StepState.CURRENT : StepState.PENDING;
			result.add(new StepResponse(MAIN_FLOW_LABELS.get(index), state));
		}
		return result;
	}

	private List<TimelineEntryResponse> timeline(Order order, List<TimelineRow> rows) {
		Long buyerId = order.getUser() == null ? null : order.getUser().getId();
		Long ownerId = order.getShop().getOwner() == null ? null : order.getShop().getOwner().getId();
		Long shipperId = order.getShipper() == null ? null : order.getShipper().getId();
		Long currentUserId = currentUser.getId();
		return rows.stream().map(row -> {
			boolean assignment = row.getPreviousStatus() != null
					&& row.getPreviousStatus() == row.getNewStatus()
					&& row.getNewStatus() == OrderStatus.CONFIRMED;
			String label = assignment ? "Đã phân công người giao hàng" : row.getNewStatus().getDisplayName();
			String note = assignment ? null : normalizeNote(row.getNote());
			return new TimelineEntryResponse(label, OrderStatusUi.color(row.getNewStatus()), row.getCreatedAt(),
					actorLabel(row.getActorId(), currentUserId, buyerId, ownerId, shipperId), note);
		}).toList();
	}

	private String actorLabel(Long actorId, Long currentId, Long buyerId, Long ownerId, Long shipperId) {
		if (actorId == null) return "Hệ thống";
		if (Objects.equals(actorId, buyerId)) return "Bạn";
		if (Objects.equals(actorId, ownerId)) return "Cửa hàng";
		if (Objects.equals(actorId, shipperId)) return "Người giao hàng";
		return "Quản trị viên";
	}

	private String normalizeNote(String note) {
		if (note == null) return null;
		String trimmed = note.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private SiblingOrderResponse toSibling(Order sibling) {
		return new SiblingOrderResponse(sibling.getId(), sibling.getOrderCode(), sibling.getShop().getName(),
				sibling.getStatus().getDisplayName(), OrderStatusUi.color(sibling.getStatus()), sibling.getTotalAmount());
	}

	private boolean canCancel(OrderStatus status) {
		return transitionPolicy.canBuyerCancel(status);
	}

	private boolean canRequestReturn(Order order) {
		if (order.getStatus() != OrderStatus.DELIVERED) return false;
		LocalDateTime deliveredAt = order.getDeliveredAt() == null ? order.getUpdatedAt() : order.getDeliveredAt();
		return deliveredAt != null && !LocalDateTime.now(clock).isAfter(deliveredAt.plusDays(orderProperties.returnWindowDays()));
	}
}
