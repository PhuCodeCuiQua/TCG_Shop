package com.tcgshop.service.impl;

import com.tcgshop.dto.response.VendorReturnResponse;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReturnStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.VendorReturnMapper;
import com.tcgshop.repository.ReturnRequestRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorReturnRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.OrderService;
import com.tcgshop.service.VendorReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorReturnServiceImpl implements VendorReturnService {

	private final VendorReturnRepository vendorReturnRepository;
	private final ReturnRequestRepository returnRequestRepository;
	private final ShopRepository shopRepository;
	private final CurrentUser currentUser;
	private final VendorReturnMapper mapper;
	private final OrderService orderService;

	@Override
	@Transactional(readOnly = true)
	public Page<VendorReturnResponse> getPendingReturns(Pageable pageable) {
		Shop shop = getCurrentShop();
		return vendorReturnRepository.findPendingByShopId(
				shop.getId(), OrderStatus.RETURN_REQUESTED, ReturnStatus.PENDING, pageable)
			.map(mapper::toResponse);
	}

	@Override
	@Transactional
	public void approve(Long orderId) {
		verifyOwnership(orderId, getCurrentShop());
		// OrderService là điểm duy nhất đổi trạng thái, ghi timeline và đồng bộ ReturnRequest.
		orderService.changeStatus(orderId, OrderStatus.RETURN_APPROVED, null);
	}

	@Override
	@Transactional
	public void reject(Long orderId, String reason) {
		verifyOwnership(orderId, getCurrentShop());
		if (reason == null || reason.isBlank()) {
			throw new BadRequestException("Vui lòng nhập lý do từ chối");
		}
		orderService.changeStatus(orderId, OrderStatus.RETURN_REJECTED, reason.trim());
	}

	private Shop getCurrentShop() {
		return shopRepository.findByOwnerId(currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh của bạn"));
	}

	private void verifyOwnership(Long orderId, Shop currentShop) {
		if (vendorReturnRepository.existsByOrderIdAndShopId(orderId, currentShop.getId())) {
			return;
		}
		if (returnRequestRepository.existsByOrderId(orderId)) {
			throw new ForbiddenException("Bạn không có quyền xử lý yêu cầu trả hàng này");
		}
		throw new ResourceNotFoundException("Yêu cầu trả hàng", orderId);
	}
}
