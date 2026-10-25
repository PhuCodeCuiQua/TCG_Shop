package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShipperOrderDetailResponse;
import com.tcgshop.dto.response.ShipperOrderSummaryResponse;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ShipperOrderServiceImplTest {
	private static final Long SHIPPER_ID = 42L;

	@Mock
	private ShipperOrderRepository shipperOrderRepository;
	@Mock
	private ShipperOrderMapper shipperOrderMapper;
	@Mock
	private CurrentUser currentUser;

	@InjectMocks
	private ShipperOrderServiceImpl shipperOrderService;

	@BeforeEach
	void setUp() {
		lenient().when(currentUser.hasRole(RoleName.ROLE_SHIPPER)).thenReturn(true);
		lenient().when(currentUser.getId()).thenReturn(SHIPPER_ID);
	}

	@Test
	void listIsScopedToCurrentShipperAndCombinesStatusesForTabs() {
		ShipperOrderStatusCount count = mock(ShipperOrderStatusCount.class);
		when(count.getStatus()).thenReturn(OrderStatus.SHIPPING);
		when(count.getTotal()).thenReturn(3L);
		when(shipperOrderRepository.countStatusesByShipperId(SHIPPER_ID)).thenReturn(List.of(count));
		Order order = new Order();
		ShipperOrderSummaryResponse summary = summary();
		when(shipperOrderRepository.findAssignedOrders(eq(SHIPPER_ID),
				eq(List.of(OrderStatus.PICKED_UP, OrderStatus.SHIPPING)), any()))
				.thenReturn(new PageImpl<>(List.of(order)));
		when(shipperOrderMapper.toSummary(order)).thenReturn(summary);

		var result = shipperOrderService.getOrders("shipping",
				PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt"))));

		assertEquals(5, result.tabs().size());
		assertEquals(3L, result.tabs().get(1).count());
		assertEquals(true, result.tabs().get(1).selected());
		assertEquals("shipping", result.selectedTab());
		assertEquals(List.of(summary), result.orders().content());
		verify(shipperOrderRepository).countStatusesByShipperId(SHIPPER_ID);
		verify(shipperOrderRepository).findAssignedOrders(eq(SHIPPER_ID), eq(List.of(OrderStatus.PICKED_UP,
				OrderStatus.SHIPPING)), any());
	}

	@Test
	void invalidTabIsRejected() {
		assertThrows(BadRequestException.class, () -> shipperOrderService.getOrders("unknown",
				PageRequest.of(0, 10)));
	}

	@Test
	void detailLoadsOnlyTheCurrentShippersAssignment() {
		Order order = new Order();
		ShipperOrderDetailResponse detail = detail();
		when(shipperOrderRepository.findByIdAndShipperId(15L, SHIPPER_ID)).thenReturn(Optional.of(order));
		when(shipperOrderMapper.toDetail(order)).thenReturn(detail);

		assertEquals(detail, shipperOrderService.getOrderDetail(15L));
		verify(shipperOrderRepository).findByIdAndShipperId(15L, SHIPPER_ID);
	}

	@Test
	void detailForAnotherShipperReturnsForbidden() {
		when(shipperOrderRepository.findByIdAndShipperId(15L, SHIPPER_ID)).thenReturn(Optional.empty());
		when(shipperOrderRepository.existsById(15L)).thenReturn(true);

		assertThrows(ForbiddenException.class, () -> shipperOrderService.getOrderDetail(15L));
	}

	@Test
	void missingDetailReturnsNotFound() {
		when(shipperOrderRepository.findByIdAndShipperId(15L, SHIPPER_ID)).thenReturn(Optional.empty());
		when(shipperOrderRepository.existsById(15L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> shipperOrderService.getOrderDetail(15L));
	}

	@Test
	void nonShipperCannotUseTheService() {
		when(currentUser.hasRole(RoleName.ROLE_SHIPPER)).thenReturn(false);

		assertThrows(ForbiddenException.class,
				() -> shipperOrderService.getOrders("pickup", PageRequest.of(0, 10)));
	}

	private ShipperOrderSummaryResponse summary() {
		return new ShipperOrderSummaryResponse(15L, "ORD-15", null, "Hà Nội", null,
				OrderStatus.SHIPPING, "Đang giao", "primary");
	}

	private ShipperOrderDetailResponse detail() {
		return new ShipperOrderDetailResponse(15L, "ORD-15", null, OrderStatus.SHIPPING,
				"Đang giao", "primary", "Moonlight Cards", "Hà Nội", "0900000000",
				null, "Người nhận", "0912345678", "Hà Nội", null, null, List.of());
	}
}
