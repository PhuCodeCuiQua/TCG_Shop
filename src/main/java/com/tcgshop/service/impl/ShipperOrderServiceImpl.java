package com.tcgshop.service.impl;

import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShipperOrderDetailResponse;
import com.tcgshop.dto.response.ShipperOrderListResponse;
import com.tcgshop.dto.response.ShipperOrderSummaryResponse;
import com.tcgshop.dto.response.ShipperOrderTabResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.ShipperOrderMapper;
import com.tcgshop.repository.ShipperOrderRepository;
import com.tcgshop.repository.ShipperOrderStatusCount;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ShipperOrderService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperOrderServiceImpl implements ShipperOrderService {
	private static final List<OrderTabDefinition> TABS = List.of(
			new OrderTabDefinition("pickup", "Cần lấy hàng", List.of(OrderStatus.CONFIRMED)),
			new OrderTabDefinition("shipping", "Đang giao", List.of(OrderStatus.PICKED_UP, OrderStatus.SHIPPING)),
			new OrderTabDefinition("completed", "Hoàn tất", List.of(OrderStatus.DELIVERED)),
			new OrderTabDefinition("failed", "Thất bại", List.of(OrderStatus.DELIVERY_FAILED)),
			new OrderTabDefinition("returns", "Trả hàng cần lấy về", List.of(OrderStatus.RETURN_APPROVED)));

	private final ShipperOrderRepository shipperOrderRepository;
	private final ShipperOrderMapper shipperOrderMapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public ShipperOrderListResponse getOrders(String tabSlug, Pageable pageable) {
		requireShipper();
		OrderTabDefinition selectedTab = findTab(tabSlug);
		Long shipperId = currentUser.getId();

		Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
		for (ShipperOrderStatusCount row : shipperOrderRepository.countStatusesByShipperId(shipperId)) {
			counts.put(row.getStatus(), row.getTotal());
		}
		List<ShipperOrderTabResponse> tabs = TABS.stream()
				.map(tab -> new ShipperOrderTabResponse(tab.slug(), tab.label(),
						tab.statuses().stream().mapToLong(status -> counts.getOrDefault(status, 0L)).sum(),
						tab.slug().equals(selectedTab.slug())))
				.toList();

		var orderPage = shipperOrderRepository.findAssignedOrders(shipperId, selectedTab.statuses(), pageable);
		return new ShipperOrderListResponse(tabs, PageResponse.from(orderPage.map(shipperOrderMapper::toSummary)),
				selectedTab.slug());
	}

	@Override
	@Transactional(readOnly = true)
	public ShipperOrderDetailResponse getOrderDetail(Long orderId) {
		requireShipper();
		Long shipperId = currentUser.getId();
		Order order = shipperOrderRepository.findByIdAndShipperId(orderId, shipperId)
				.orElseGet(() -> {
					if (shipperOrderRepository.existsById(orderId)) {
						throw new ForbiddenException("Bạn không có quyền xem đơn hàng chưa được phân công cho mình");
					}
					throw new ResourceNotFoundException("Đơn hàng", orderId);
				});
		return shipperOrderMapper.toDetail(order);
	}

	private OrderTabDefinition findTab(String tabSlug) {
		return TABS.stream()
				.filter(tab -> tab.slug().equals(tabSlug))
				.findFirst()
				.orElseThrow(() -> new BadRequestException("Mục đơn hàng không hợp lệ"));
	}

	private void requireShipper() {
		if (!currentUser.hasRole(RoleName.ROLE_SHIPPER)) {
			throw new ForbiddenException("Chỉ Shipper được xem danh sách đơn được phân công");
		}
	}

	private record OrderTabDefinition(String slug, String label, List<OrderStatus> statuses) { }
}
