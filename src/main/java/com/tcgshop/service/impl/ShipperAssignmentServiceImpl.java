package com.tcgshop.service.impl;

import com.tcgshop.dto.response.AssignableOrderResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShipperLoadResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.AssignableOrderMapper;
import com.tcgshop.repository.ManagerOrderRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.OrderService;
import com.tcgshop.service.ShipperAssignmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipperAssignmentServiceImpl implements ShipperAssignmentService {

	private final ManagerOrderRepository managerOrderRepository;
	private final OrderService orderService;
	private final AssignableOrderMapper assignableOrderMapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<AssignableOrderResponse> searchOrders(boolean assigned, String keyword,
			Pageable pageable) {
		requireOperator();
		String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
		return PageResponse.from(managerOrderRepository
				.searchByStatusAndAssigned(OrderStatus.CONFIRMED, assigned, normalized, pageable)
				.map(assignableOrderMapper::toResponse));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShipperLoadResponse> getShipperLoads() {
		requireOperator();
		// "Đang giao" tính theo trạng thái SHIPPING, giống phần đếm của PA-13
		return managerOrderRepository.findShipperLoads(
				RoleName.ROLE_SHIPPER, UserStatus.ACTIVE, OrderStatus.SHIPPING);
	}

	@Override
	@Transactional
	public void assign(Long orderId, Long shipperId) {
		requireOperator();
		if (shipperId == null) {
			throw new BadRequestException("Vui lòng chọn shipper");
		}
		// OrderService kiểm tra: đơn CONFIRMED, shipper có ROLE_SHIPPER và ACTIVE, ghi lịch sử,
		// phát OrderAssignedEvent. Thông báo cho shipper và người mua do listener PA-40 xử lý.
		orderService.assignShipper(orderId, shipperId);
	}

	// Lớp phân quyền thứ hai (lớp thứ nhất là @PreAuthorize ở Controller)
	private void requireOperator() {
		if (!currentUser.hasRole(RoleName.ROLE_MANAGER) && !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
			throw new ForbiddenException("Bạn không có quyền phân công shipper");
		}
	}
}