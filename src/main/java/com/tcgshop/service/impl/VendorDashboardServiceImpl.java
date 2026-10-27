package com.tcgshop.service.impl;

import com.tcgshop.dto.response.VendorDashboardResponse;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorDashboardRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.VendorDashboardService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorDashboardServiceImpl implements VendorDashboardService {
	private final VendorDashboardRepository vendorDashboardRepository;
	private final ShopRepository shopRepository;
	private final CurrentUser currentUser;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public VendorDashboardResponse getDashboard() {
		Shop shop = shopRepository.findByOwnerId(currentUser.getId())
				.filter(candidate -> candidate.getStatus() == ShopStatus.APPROVED
						&& !Boolean.TRUE.equals(candidate.getDeleted()))
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy shop đang hoạt động"));

		YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
		BigDecimal monthlyRevenue = vendorDashboardRepository.sumDeliveredRevenueByShopAndPeriod(
				shop.getId(), OrderStatus.DELIVERED,
				currentMonth.atDay(1).atStartOfDay(),
				currentMonth.plusMonths(1).atDay(1).atStartOfDay());

		return new VendorDashboardResponse(
				vendorDashboardRepository.countProductsByShopAndStatus(shop.getId(), ProductStatus.ACTIVE),
				vendorDashboardRepository.countOrdersByShopAndStatus(shop.getId(), OrderStatus.NEW),
				monthlyRevenue == null ? BigDecimal.ZERO : monthlyRevenue);
	}
}
