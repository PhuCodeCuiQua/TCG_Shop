package com.tcgshop.service;

import com.tcgshop.dto.response.WishlistProductResponse;
import com.tcgshop.dto.response.WishlistToggleResponse;
import java.util.Collection;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistService {
	WishlistToggleResponse toggle(Long productId);

	Map<Long, Boolean> getStatuses(Collection<Long> productIds);

	Page<WishlistProductResponse> getCurrentUserWishlist(Pageable pageable);
}
