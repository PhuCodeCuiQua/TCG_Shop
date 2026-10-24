package com.tcgshop.service.impl;

import com.tcgshop.dto.request.VendorOrderStatusChangeRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.VendorOrderDetailResponse;
import com.tcgshop.dto.response.VendorOrderItemResponse;
import com.tcgshop.dto.response.VendorOrderListResponse;
import com.tcgshop.dto.response.VendorOrderStatusResponse;
import com.tcgshop.dto.response.VendorOrderSummaryResponse;
import com.tcgshop.dto.response.VendorOrderTabResponse;
import com.tcgshop.dto.response.VendorOrderTimelineResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.PaymentMethod;
import com.tcgshop.entity.enums.PaymentStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.VendorOrderMapper;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorOrderRepository;
import com.tcgshop.repository.VendorOrderStatusCount;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.OrderService;
import com.tcgshop.service.VendorOrderService;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorOrderServiceImpl implements VendorOrderService {
	private static final int MAX_KEYWORD_LENGTH = 50;
	private static final List<OrderTabDefinition> TABS = List.of(
			new OrderTabDefinition("new", "Mới", List.of(OrderStatus.NEW)),
			new OrderTabDefinition("confirmed", "Đã xác nhận", List.of(OrderStatus.CONFIRMED)),
			new OrderTabDefinition("picked-up", "Đã lấy hàng", List.of(OrderStatus.PICKED_UP)),
			new OrderTabDefinition("shipping", "Đang giao", List.of(OrderStatus.SHIPPING, OrderStatus.DELIVERY_FAILED)),
			new OrderTabDefinition("delivered", "Đã giao", List.of(OrderStatus.DELIVERED)),
			new OrderTabDefinition("cancelled", "Đã hủy", List.of(OrderStatus.CANCELLED)),
			new OrderTabDefinition("returns", "Trả hàng / Hoàn tiền", List.of(OrderStatus.RETURN_REQUESTED,
					OrderStatus.RETURN_APPROVED, OrderStatus.RETURN_REJECTED, OrderStatus.RETURNED, OrderStatus.REFUNDED)));

	private final VendorOrderRepository vendorOrderRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final ShopRepository shopRepository;
	private final VendorOrderMapper mapper;
	private final CurrentUser currentUser;
	private final OrderService orderService;

	@Override
	@Transactional(readOnly = true)
	public VendorOrderListResponse getOrders(String tabSlug, String keyword, Pageable pageable) {
		Shop shop = getCurrentShop();
		OrderTabDefinition selectedTab = findTab(tabSlug);
		String normalizedKeyword = normalizeKeyword(keyword);
		Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
		for (VendorOrderStatusCount row : vendorOrderRepository.countStatusesByShopId(shop.getId())) {
			counts.put(row.getStatus(), row.getTotal());
		}
		List<VendorOrderTabResponse> tabs = TABS.stream().map(tab -> new VendorOrderTabResponse(
				tab.slug(), tab.label(), tab.statuses().stream().mapToLong(status -> counts.getOrDefault(status, 0L)).sum(),
				tab.slug().equals(selectedTab.slug()))).toList();
		Page<Order> orderPage = vendorOrderRepository.searchShopOrders(
				shop.getId(), selectedTab.statuses(), normalizedKeyword, pageable);
		Page<VendorOrderSummaryResponse> summaries = orderPage.map(mapper::toSummary);
		return new VendorOrderListResponse(tabs, PageResponse.from(summaries), selectedTab.slug(), normalizedKeyword);
	}

	@Override
	@Transactional(readOnly = true)
	public VendorOrderDetailResponse getOrderDetail(Long orderId) {
		return toDetail(findOwnedOrder(orderId));
	}

	@Override
	@Transactional
	public VendorOrderStatusResponse changeOrderStatus(Long orderId, VendorOrderStatusChangeRequest request) {
		if (request == null || request.getNewStatus() == null) {
			throw new BadRequestException("Vui lòng chọn thao tác hợp lệ");
		}
		Order order = findOwnedOrder(orderId);
		OrderStatus targetStatus = request.getNewStatus();
		if (order.getStatus() != OrderStatus.NEW
				|| targetStatus != OrderStatus.CONFIRMED && targetStatus != OrderStatus.CANCELLED) {
			throw new BadRequestException("Chỉ có thể xác nhận hoặc từ chối đơn hàng mới");
		}
		String note = request.getNote() == null ? null : request.getNote().trim();
		if (targetStatus == OrderStatus.CANCELLED && (note == null || note.isBlank())) {
			throw new BadRequestException("Vui lòng nhập lý do từ chối đơn hàng");
		}

		// Mọi thay đổi trạng thái phải đi qua OrderService để áp dụng state machine, ghi lịch sử và phát sự kiện.
		orderService.changeStatus(orderId, targetStatus, note);
		Order updated = findOwnedOrder(orderId);
		return new VendorOrderStatusResponse(updated.getId(), updated.getStatus(),
				updated.getStatus().getDisplayName(), com.tcgshop.util.OrderStatusUi.color(updated.getStatus()));
	}

	private VendorOrderDetailResponse toDetail(Order order) {
		List<VendorOrderItemResponse> items = order.getItems().stream().map(mapper::toItem).toList();
		List<VendorOrderTimelineResponse> timeline = historyRepository.findTimelineByOrderId(order.getId()).stream()
				.map(mapper::toTimeline).toList();
		PaymentMethod method = order.getOrderGroup() == null ? null : order.getOrderGroup().getPaymentMethod();
		PaymentStatus paymentStatus = order.getOrderGroup() == null ? null : order.getOrderGroup().getPaymentStatus();
		return new VendorOrderDetailResponse(
				order.getId(), order.getOrderCode(), order.getCreatedAt(), order.getStatus(),
				order.getStatus().getDisplayName(), com.tcgshop.util.OrderStatusUi.color(order.getStatus()),
				order.getUser() == null || order.getUser().getFullName() == null
						? "Khách hàng" : order.getUser().getFullName(),
				order.getReceiverName(), order.getReceiverPhone(), order.getShippingAddress(),
				paymentMethodLabel(method), paymentStatusLabel(method, paymentStatus, order), items,
				valueOrZero(order.getSubtotal()), valueOrZero(order.getDiscountAmount()),
				valueOrZero(order.getShippingFee()), valueOrZero(order.getTotalAmount()), timeline,
				order.getStatus() == OrderStatus.NEW);
	}

	private String paymentMethodLabel(PaymentMethod method) {
		if (method == null) return "Chưa xác định";
		return method == PaymentMethod.COD ? "Thanh toán khi nhận hàng (COD)" : "Ví MoMo";
	}

	private String paymentStatusLabel(PaymentMethod method, PaymentStatus status, Order order) {
		if (method == PaymentMethod.COD) return order.isCodCollected() ? "Đã thanh toán" : "Thanh toán khi nhận hàng";
		if (status == null) return "Chờ thanh toán";
		return switch (status) {
			case PENDING -> "Chờ thanh toán";
			case PAID -> "Đã thanh toán";
			case FAILED -> "Thanh toán thất bại";
			case REFUND_PENDING -> "Đang chờ hoàn tiền";
			case REFUNDED -> "Đã hoàn tiền";
		};
	}

	private BigDecimal valueOrZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private Order findOwnedOrder(Long orderId) {
		Shop currentShop = getCurrentShop();
		Order order = vendorOrderRepository.findDetailById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", orderId));
		if (order.getShop() == null || !currentShop.getId().equals(order.getShop().getId())
				|| order.getShop().getOwner() == null
				|| !currentUser.getId().equals(order.getShop().getOwner().getId())) {
			throw new ForbiddenException("Bạn không có quyền xem hoặc xử lý đơn hàng của shop khác");
		}
		return order;
	}

	private Shop getCurrentShop() {
		if (!currentUser.hasRole(RoleName.ROLE_VENDOR)) {
			throw new ForbiddenException("Chỉ Vendor mới được xem đơn hàng của shop");
		}
		return shopRepository.findByOwnerId(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy shop của tài khoản hiện tại"));
	}

	private OrderTabDefinition findTab(String slug) {
		return TABS.stream().filter(tab -> tab.slug().equals(slug)).findFirst()
				.orElseGet(() -> TABS.getFirst());
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null) return null;
		String normalized = keyword.trim().replaceAll("\\s+", " ");
		if (normalized.length() > MAX_KEYWORD_LENGTH) normalized = normalized.substring(0, MAX_KEYWORD_LENGTH);
		return normalized.isEmpty() ? null : normalized;
	}

	private record OrderTabDefinition(String slug, String label, List<OrderStatus> statuses) { }
}
