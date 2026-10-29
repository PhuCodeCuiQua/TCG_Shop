package com.tcgshop.service.impl;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.ViewedProduct;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.repository.ViewedProductRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.ViewedProductService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewedProductServiceImpl implements ViewedProductService {
	private static final int MAX_HISTORY_SIZE = 20;

	private final ViewedProductRepository viewedProductRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final CurrentUser currentUser;

	@Override
	@Transactional
	public void record(Long userId, Long productId) {
		validateActor(userId);
		User user = userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
		Product product = productRepository.findById(productId)
				.filter(candidate -> !Boolean.TRUE.equals(candidate.getDeleted()))
				.filter(this::isPubliclyVisible)
				.orElse(null);
		if (product == null) {
			log.debug("Bỏ qua lượt xem sản phẩm không còn công khai, productId={}", productId);
			return;
		}

		LocalDateTime viewedAt = LocalDateTime.now();
		ViewedProduct viewedProduct = viewedProductRepository.findByUserIdAndProductId(userId, productId)
				.orElseGet(() -> {
					ViewedProduct created = new ViewedProduct();
					created.setUser(user);
					created.setProduct(product);
					return created;
				});
		viewedProduct.setViewedAt(viewedAt);
		viewedProductRepository.saveAndFlush(viewedProduct);
		trimHistory(userId);
	}

	private void validateActor(Long userId) {
		if (!currentUser.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Authentication is required");
		}
		if (!currentUser.hasRole(RoleName.ROLE_USER) || !currentUser.getId().equals(userId)) {
			throw new ForbiddenException("Không thể ghi lịch sử xem cho tài khoản khác");
		}
	}

	private boolean isPubliclyVisible(Product product) {
		boolean statusVisible = product.getStatus() == ProductStatus.ACTIVE
				|| product.getStatus() == ProductStatus.OUT_OF_STOCK;
		if (!statusVisible || Boolean.TRUE.equals(product.getDeleted())) {
			return false;
		}
		Shop shop = product.getShop();
		if (shop == null || shop.getStatus() != ShopStatus.APPROVED || Boolean.TRUE.equals(shop.getDeleted())) {
			return false;
		}
		if (product.getCard() != null) {
			var card = product.getCard();
			return card.isActive() && card.getSeries() != null && card.getSeries().isActive()
					&& card.getSeries().getGame() != null && card.getSeries().getGame().isActive();
		}
		return product.getSeries() == null || (product.getSeries().isActive()
				&& product.getSeries().getGame() != null && product.getSeries().getGame().isActive());
	}

	private void trimHistory(Long userId) {
		Page<ViewedProduct> recent = viewedProductRepository.findByUserId(userId,
				PageRequest.of(0, MAX_HISTORY_SIZE, Sort.by(
						Sort.Order.desc("viewedAt"), Sort.Order.desc("id"))));
		if (recent.getTotalElements() > MAX_HISTORY_SIZE) {
			List<Long> keepIds = recent.getContent().stream().map(ViewedProduct::getId).toList();
			viewedProductRepository.deleteByUserIdKeepingIds(userId, keepIds);
		}
	}
}
