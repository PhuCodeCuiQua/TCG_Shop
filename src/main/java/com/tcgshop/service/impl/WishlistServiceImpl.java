package com.tcgshop.service.impl;

import com.tcgshop.dto.response.WishlistProductResponse;
import com.tcgshop.dto.response.WishlistToggleResponse;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.Wishlist;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.WishlistMapper;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.repository.WishlistProductRepository;
import com.tcgshop.repository.WishlistRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.WishlistService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

	private static final String PRODUCT_NOT_FOUND = "Sản phẩm không tồn tại hoặc hiện không được mở bán";
	private static final int MAX_STATUS_BATCH_SIZE = 100;

	private final WishlistRepository wishlistRepository;
	private final WishlistProductRepository productRepository;
	private final UserRepository userRepository;
	private final WishlistMapper wishlistMapper;
	private final CurrentUser currentUser;

	@Override
	@Transactional
	public WishlistToggleResponse toggle(Long productId) {
		Long userId = currentUser.getId();
		Product product = productRepository.findByIdForUpdate(productId)
				.filter(this::isPubliclyAvailable)
				.orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND));

		boolean wishlisted;
		var existing = wishlistRepository.findByUserIdAndProductId(userId, productId);
		if (existing.isPresent()) {
			wishlistRepository.delete(existing.get());
			wishlisted = false;
		} else {
			Wishlist wishlist = new Wishlist();
			wishlist.setUser(userRepository.getReferenceById(userId));
			wishlist.setProduct(product);
			wishlistRepository.save(wishlist);
			wishlisted = true;
		}

		wishlistRepository.flush();
		int favoriteCount = Math.toIntExact(wishlistRepository.countByProductId(productId));
		product.setFavoriteCount(favoriteCount);
		productRepository.saveAndFlush(product);
		return new WishlistToggleResponse(wishlisted, favoriteCount);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, Boolean> getStatuses(Collection<Long> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = productIds.stream().filter(id -> id != null).distinct().toList();
		if (ids.size() > MAX_STATUS_BATCH_SIZE) {
			throw new BadRequestException("Không thể kiểm tra quá 100 sản phẩm trong một lần");
		}
		if (ids.isEmpty()) {
			return Map.of();
		}
		Set<Long> likedIds = wishlistRepository.findProductIdsByUserIdAndProductIdIn(currentUser.getId(), ids)
				.stream().collect(Collectors.toSet());
		Map<Long, Boolean> result = new LinkedHashMap<>();
		ids.forEach(id -> result.put(id, likedIds.contains(id)));
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<WishlistProductResponse> getCurrentUserWishlist(Pageable pageable) {
		return wishlistRepository.findVisibleProductsByUserId(currentUser.getId(), pageable)
				.map(wishlistMapper::toResponse);
	}

	private boolean isPubliclyAvailable(Product product) {
		if (product.getStatus() != ProductStatus.ACTIVE
				|| Boolean.TRUE.equals(product.getDeleted())) {
			return false;
		}
		Shop shop = product.getShop();
		if (shop == null || shop.getStatus() != ShopStatus.APPROVED || Boolean.TRUE.equals(shop.getDeleted())) {
			return false;
		}
		if (product.getCard() != null) {
			var card = product.getCard();
			return card.isActive() && card.getSeries() != null
					&& card.getSeries().isActive()
					&& card.getSeries().getGame() != null && card.getSeries().getGame().isActive();
		}
		return product.getSeries() == null || (product.getSeries().isActive()
				&& product.getSeries().getGame() != null
				&& product.getSeries().getGame().isActive());
	}
}
