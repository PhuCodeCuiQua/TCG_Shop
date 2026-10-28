package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ShipperDashboardResponse;
import com.tcgshop.dto.response.ShipperRecentOrderResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.mapper.ShipperOrderMapper;
import com.tcgshop.repository.ShipperOrderRepository;
import com.tcgshop.repository.ShipperStatisticsRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ShipperDashboardService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperDashboardServiceImpl implements ShipperDashboardService {
	private final ShipperStatisticsRepository shipperStatisticsRepository;
	private final ShipperOrderRepository shipperOrderRepository;
	private final ShipperOrderMapper shipperOrderMapper;
	private final CurrentUser currentUser;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public ShipperDashboardResponse getDashboard() {
		LocalDate today = LocalDate.now(clock);
		long toDeliver = shipperStatisticsRepository.countOrdersToDeliver(currentUser.getId());
		long deliveredToday = shipperStatisticsRepository.countDeliveredBetween(
				currentUser.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
		BigDecimal weeklyCod = shipperStatisticsRepository.sumCollectedCodBetween(
				currentUser.getId(), today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
		List<ShipperRecentOrderResponse> recentOrders = shipperOrderRepository
				.findAssignedOrders(currentUser.getId(), List.of(OrderStatus.PICKED_UP, OrderStatus.SHIPPING),
						PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")
								.and(Sort.by(Sort.Direction.DESC, "id"))))
				.map(shipperOrderMapper::toRecentOrder)
				.getContent();
		return new ShipperDashboardResponse(Math.max(0, toDeliver), Math.max(0, deliveredToday),
				weeklyCod == null ? BigDecimal.ZERO : weeklyCod, recentOrders);
	}
}
