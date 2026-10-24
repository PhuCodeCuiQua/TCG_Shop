package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.response.AssignableOrderResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShipperLoadResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.AssignableOrderMapper;
import com.tcgshop.repository.ManagerOrderRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ShipperAssignmentServiceImplTest {

	private static final Long ORDER_ID = 10L;
	private static final Long SHIPPER_ID = 7L;

	@Mock
	private ManagerOrderRepository managerOrderRepository;
	@Mock
	private OrderService orderService;
	@Mock
	private AssignableOrderMapper assignableOrderMapper;
	@Mock
	private CurrentUser currentUser;

	@InjectMocks
	private ShipperAssignmentServiceImpl service;

	private void asManager() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
	}

	// ------------------------------------------------------------ phân quyền

	@Test
	void assign_neitherManagerNorAdmin_throwsForbiddenAndDoesNotCallOrderService() {
		assertThatThrownBy(() -> service.assign(ORDER_ID, SHIPPER_ID)).isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(orderService);
	}

	@Test
	void searchOrders_neitherManagerNorAdmin_throwsForbidden() {
		assertThatThrownBy(() -> service.searchOrders(false, null, PageRequest.of(0, 10)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(managerOrderRepository);
	}

	@Test
	void getShipperLoads_neitherManagerNorAdmin_throwsForbidden() {
		assertThatThrownBy(() -> service.getShipperLoads()).isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(managerOrderRepository);
	}

	@Test
	void assign_asAdmin_isAllowed() {
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(false);
		when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);

		service.assign(ORDER_ID, SHIPPER_ID);

		verify(orderService).assignShipper(ORDER_ID, SHIPPER_ID);
	}

	// ------------------------------------------------------------ assign

	@Test
	void assign_nullShipperId_throwsBadRequestAndDoesNotCallOrderService() {
		asManager();

		assertThatThrownBy(() -> service.assign(ORDER_ID, null)).isInstanceOf(BadRequestException.class);

		verifyNoInteractions(orderService);
	}

	@Test
	void assign_validRequest_delegatesToOrderService() {
		asManager();

		service.assign(ORDER_ID, SHIPPER_ID);

		verify(orderService).assignShipper(ORDER_ID, SHIPPER_ID);
	}

	@Test
	void assign_orderServiceRejects_propagatesException() {
		asManager();
		doThrow(new BadRequestException("Shipper không hợp lệ"))
				.when(orderService).assignShipper(ORDER_ID, SHIPPER_ID);

		assertThatThrownBy(() -> service.assign(ORDER_ID, SHIPPER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Shipper không hợp lệ");
	}

	// ------------------------------------------------------------ danh sách

	@Test
	void searchOrders_unassignedTab_queriesConfirmedOrdersWithoutShipper() {
		asManager();
		Order order = new Order();
		order.setId(ORDER_ID);
		AssignableOrderResponse response = new AssignableOrderResponse(ORDER_ID, "DH001", "Card Paradise",
				"Người nhận", "0900000000", BigDecimal.TEN, LocalDateTime.now(), null, null);
		Pageable pageable = PageRequest.of(0, 10);
		when(managerOrderRepository.searchByStatusAndAssigned(OrderStatus.CONFIRMED, false, null, pageable))
				.thenReturn(new PageImpl<>(List.of(order), pageable, 1));
		when(assignableOrderMapper.toResponse(order)).thenReturn(response);

		PageResponse<AssignableOrderResponse> result = service.searchOrders(false, "  ", pageable);

		// Từ khóa trống được chuẩn hóa thành null
		assertThat(result.content()).containsExactly(response);
		verify(managerOrderRepository).searchByStatusAndAssigned(eq(OrderStatus.CONFIRMED), eq(false),
				isNull(), eq(pageable));
	}

	@Test
	void searchOrders_assignedTab_trimsKeyword() {
		asManager();
		Pageable pageable = PageRequest.of(0, 10);
		when(managerOrderRepository.searchByStatusAndAssigned(OrderStatus.CONFIRMED, true, "DH001", pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		service.searchOrders(true, "  DH001 ", pageable);

		verify(managerOrderRepository).searchByStatusAndAssigned(OrderStatus.CONFIRMED, true, "DH001",
				pageable);
	}

	@Test
	void getShipperLoads_returnsActiveShippersWithShippingCount() {
		asManager();
		List<ShipperLoadResponse> loads = List.of(
				new ShipperLoadResponse(SHIPPER_ID, "Shipper Một", "shipper1@tcgshop.local", 2L));
		when(managerOrderRepository.findShipperLoads(RoleName.ROLE_SHIPPER, UserStatus.ACTIVE,
				OrderStatus.SHIPPING)).thenReturn(loads);

		List<ShipperLoadResponse> result = service.getShipperLoads();

		assertThat(result).isEqualTo(loads);
	}
}