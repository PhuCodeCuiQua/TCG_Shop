package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ShipperDashboardResponse;
import com.tcgshop.repository.ShipperStatisticsRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ShipperDashboardService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('SHIPPER')")
@RequiredArgsConstructor
public class ShipperDashboardServiceImpl implements ShipperDashboardService {
	private final ShipperStatisticsRepository shipperStatisticsRepository;
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
		return new ShipperDashboardResponse(Math.max(0, toDeliver), Math.max(0, deliveredToday),
				weeklyCod == null ? BigDecimal.ZERO : weeklyCod);
	}
}
