package com.tcgshop.service.impl;

import com.tcgshop.dto.response.VendorProductResponse;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.VendorProductMapper;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorProductRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.VendorProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
public class VendorProductServiceImpl implements VendorProductService {

	private final VendorProductRepository vendorProductRepository;
	private final ShopRepository shopRepository;
	private final VendorProductMapper vendorProductMapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public Page<VendorProductResponse> searchCurrentVendorProducts(
			String keyword, ProductType type, ProductStatus status, Pageable pageable) {
		if (!currentUser.hasRole(RoleName.ROLE_VENDOR)) {
			throw new ForbiddenException("Chỉ chủ chi nhánh mới được xem danh sách sản phẩm của chi nhánh");
		}

		Shop shop = shopRepository.findByOwnerId(currentUser.getId())
				.filter(candidate -> !Boolean.TRUE.equals(candidate.getDeleted()))
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh của tài khoản hiện tại"));
		String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

		return vendorProductRepository.searchOwnedProducts(
				shop.getId(), normalizedKeyword,
				type == null ? null : type.name(),
				status == null ? null : status.name(),
				pageable)
			.map(vendorProductMapper::toResponse);
	}
}
