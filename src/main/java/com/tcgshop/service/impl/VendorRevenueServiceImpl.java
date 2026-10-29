package com.tcgshop.service.impl;

import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.VendorRevenueEntryResponse;
import com.tcgshop.dto.response.VendorRevenueReportResponse;
import com.tcgshop.dto.response.VendorRevenueSummaryResponse;
import com.tcgshop.dto.response.VendorRevenueChartResponse;
import com.tcgshop.dto.response.VendorRevenueChartResponse.OrderStatusCount;
import com.tcgshop.dto.response.VendorRevenueChartResponse.RevenuePoint;
import com.tcgshop.dto.response.VendorRevenueChartResponse.TopProduct;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.VendorRevenueMapper;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorRevenueRepository;
import com.tcgshop.repository.VendorRevenueRepository.VendorRevenueTotals;
import com.tcgshop.repository.VendorRevenueRepository.VendorOrderStatusRow;
import com.tcgshop.repository.VendorRevenueRepository.VendorRevenueTrendRow;
import com.tcgshop.repository.VendorRevenueRepository.VendorTopProductRow;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.VendorRevenueService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorRevenueServiceImpl implements VendorRevenueService {
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final int MONEY_SCALE = 2;

	private final VendorRevenueRepository vendorRevenueRepository;
	private final ShopRepository shopRepository;
	private final CurrentUser currentUser;
	private final VendorRevenueMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public VendorRevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
		}
		if (toDate != null && toDate.equals(LocalDate.MAX)) {
			throw new BadRequestException("Ngày kết thúc không hợp lệ");
		}

		Shop shop = shopRepository.findByOwnerId(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh của bạn"));
		LocalDateTime startInclusive = fromDate == null ? null : fromDate.atStartOfDay();
		LocalDateTime endExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

		VendorRevenueTotals totals = vendorRevenueRepository.summarizeDeliveredOrders(
				shop.getId(), OrderStatus.DELIVERED, startInclusive, endExclusive, ZERO);
		BigDecimal revenueBase = valueOrZero(totals == null ? null : totals.getRevenueBase());
		BigDecimal commissionTotal = valueOrZero(totals == null ? null : totals.getCommissionTotal());
		BigDecimal receivedTotal = valueOrZero(totals == null ? null : totals.getReceivedTotal());
		long completedOrders = totals == null || totals.getCompletedOrders() == null
				? 0 : totals.getCompletedOrders();
		BigDecimal averageOrderValue = completedOrders == 0
				? ZERO.setScale(MONEY_SCALE)
				: revenueBase.divide(BigDecimal.valueOf(completedOrders), MONEY_SCALE, RoundingMode.HALF_UP);

		Page<VendorRevenueEntryResponse> orderPage = vendorRevenueRepository.findDeliveredOrders(
				shop.getId(), OrderStatus.DELIVERED, startInclusive, endExclusive, ZERO, pageable)
				.map(mapper::toResponse);
		VendorRevenueSummaryResponse summary = new VendorRevenueSummaryResponse(
				revenueBase, commissionTotal, receivedTotal, completedOrders, averageOrderValue);
		return new VendorRevenueReportResponse(summary, PageResponse.from(orderPage), fromDate, toDate);
	}

	@Override
	@Transactional(readOnly = true)
	public VendorRevenueChartResponse getRevenueCharts(LocalDate fromDate, LocalDate toDate, String groupBy) {
		validateDateRange(fromDate, toDate);
		String normalizedGroup = groupBy == null ? "day" : groupBy.toLowerCase(Locale.ROOT);
		if (!normalizedGroup.equals("day") && !normalizedGroup.equals("month")) {
			throw new BadRequestException("Tham số groupBy chỉ nhận day hoặc month");
		}
		Shop shop = shopRepository.findByOwnerId(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh của bạn"));
		LocalDateTime startInclusive = fromDate == null ? null : fromDate.atStartOfDay();
		LocalDateTime endExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
		String periodFormat = normalizedGroup.equals("month") ? "%Y-%m" : "%Y-%m-%d";

		List<RevenuePoint> revenue = safeList(vendorRevenueRepository.findDeliveredRevenueTrend(
				shop.getId(), startInclusive, endExclusive, periodFormat)).stream()
				.filter(Objects::nonNull).map(this::toRevenuePoint).toList();
		List<TopProduct> topProducts = safeList(vendorRevenueRepository.findTopDeliveredProducts(
				shop.getId(), startInclusive, endExclusive, org.springframework.data.domain.PageRequest.of(0, 10)))
				.stream().filter(Objects::nonNull).map(this::toTopProduct).toList();
		List<OrderStatusCount> orderStatuses = safeList(vendorRevenueRepository.countOrdersByStatus(
				shop.getId(), startInclusive, endExclusive)).stream().filter(Objects::nonNull)
				.map(this::toOrderStatusCount).filter(Objects::nonNull).toList();
		return new VendorRevenueChartResponse(normalizedGroup, revenue, topProducts, orderStatuses);
	}

	private RevenuePoint toRevenuePoint(VendorRevenueTrendRow row) {
		return new RevenuePoint(Objects.requireNonNullElse(row.getPeriod(), ""), valueOrZero(row.getRevenue()));
	}

	private TopProduct toTopProduct(VendorTopProductRow row) {
		return new TopProduct(Objects.requireNonNullElse(row.getProductName(), "Sản phẩm"),
				row.getQuantitySold() == null ? 0 : row.getQuantitySold());
	}

	private OrderStatusCount toOrderStatusCount(VendorOrderStatusRow row) {
		if (row.getStatus() == null || row.getStatus().isBlank()) {
			return null;
		}
		OrderStatus status = OrderStatus.valueOf(row.getStatus());
		return new OrderStatusCount(status.name(), status.getDisplayName(),
				row.getOrderCount() == null ? 0 : row.getOrderCount());
	}

	private <T> List<T> safeList(List<T> rows) {
		return rows == null ? List.of() : rows;
	}

	private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
		}
		if (toDate != null && toDate.equals(LocalDate.MAX)) {
			throw new BadRequestException("Ngày kết thúc không hợp lệ");
		}
	}

	private BigDecimal valueOrZero(BigDecimal value) {
		return Objects.requireNonNullElse(value, ZERO);
	}
}
