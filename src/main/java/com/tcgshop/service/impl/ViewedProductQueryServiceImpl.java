package com.tcgshop.service.impl;

import com.tcgshop.dto.response.ViewedProductResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.ViewedProductMapper;
import com.tcgshop.repository.ViewedProductListingRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ViewedProductQueryService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewedProductQueryServiceImpl implements ViewedProductQueryService {
	private static final int MAX_HISTORY_SIZE = 20;

	private final ViewedProductListingRepository listingRepository;
	private final ViewedProductMapper mapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public Page<ViewedProductResponse> getRecentProducts(Long userId, Pageable pageable) {
		if (!currentUser.isAuthenticated() || !currentUser.hasRole(RoleName.ROLE_USER)
				|| !Objects.equals(userId, currentUser.getId())) {
			throw new ForbiddenException("Bạn chỉ có thể xem lịch sử sản phẩm của chính mình");
		}

		int pageSize = pageable.getPageSize();
		Pageable firstTwenty = PageRequest.of(0, MAX_HISTORY_SIZE);
		Page<ViewedProductResponse> latest = listingRepository
				.findVisibleHistoryByUserId(userId, firstTwenty)
				.map(mapper::toResponse);
		long total = Math.min(latest.getTotalElements(), MAX_HISTORY_SIZE);
		long fromIndex = pageable.getOffset();
		if (fromIndex >= total) {
			return new PageImpl<>(List.of(), pageable, total);
		}
		int start = Math.toIntExact(fromIndex);
		int end = (int) Math.min(start + (long) pageSize, latest.getContent().size());
		return new PageImpl<>(latest.getContent().subList(start, end), pageable, total);
	}
}
