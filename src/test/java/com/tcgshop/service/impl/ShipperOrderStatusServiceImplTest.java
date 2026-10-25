package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.ShipperOrderStatusChangeRequest;
import com.tcgshop.dto.response.ShipperOrderDetailResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.service.OrderService;
import com.tcgshop.service.ShipperOrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipperOrderStatusServiceImplTest {
	@Mock
	private ShipperOrderService shipperOrderService;
	@Mock
	private OrderService orderService;
	@InjectMocks
	private ShipperOrderStatusServiceImpl service;

	@Test
	void ownershipIsCheckedBeforeChangingStatus() {
		when(shipperOrderService.getOrderDetail(12L)).thenThrow(new ForbiddenException("Không được phép"));

		assertThrows(ForbiddenException.class, () -> service.changeStatus(12L,
				new ShipperOrderStatusChangeRequest(OrderStatus.PICKED_UP, null, false)));

		verify(orderService, never()).changeStatus(12L, OrderStatus.PICKED_UP, null);
	}

	@Test
	void codDeliveryRequiresServerSideConfirmation() {
		when(shipperOrderService.getOrderDetail(12L)).thenReturn(detail(OrderStatus.SHIPPING,
				new BigDecimal("250000")));

		assertThrows(BadRequestException.class, () -> service.changeStatus(12L,
				new ShipperOrderStatusChangeRequest(OrderStatus.DELIVERED, null, false)));

		verify(orderService, never()).changeStatus(12L, OrderStatus.DELIVERED, null);
	}

	@Test
	void deliveryFailureRequiresAReason() {
		when(shipperOrderService.getOrderDetail(12L)).thenReturn(detail(OrderStatus.SHIPPING, null));

		assertThrows(BadRequestException.class, () -> service.changeStatus(12L,
				new ShipperOrderStatusChangeRequest(OrderStatus.DELIVERY_FAILED, "  ", false)));

		verify(orderService, never()).changeStatus(12L, OrderStatus.DELIVERY_FAILED, "  ");
	}

	@Test
	void confirmedCodDeliveryDelegatesToOrderStateMachine() {
		when(shipperOrderService.getOrderDetail(12L)).thenReturn(detail(OrderStatus.SHIPPING,
				new BigDecimal("250000")));

		service.changeStatus(12L, new ShipperOrderStatusChangeRequest(OrderStatus.DELIVERED, null, true));

		verify(orderService).changeStatus(12L, OrderStatus.DELIVERED, null);
	}

	private ShipperOrderDetailResponse detail(OrderStatus status, BigDecimal codAmount) {
		return new ShipperOrderDetailResponse(12L, "ORD-12", null, status, status.getDisplayName(), "primary",
				"Shop", "Pickup", "0900000000", null, "Buyer", "0911111111", "Address", codAmount,
				new BigDecimal("250000"), List.of());
	}
}
