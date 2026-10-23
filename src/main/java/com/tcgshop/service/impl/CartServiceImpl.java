package com.tcgshop.service.impl;

import com.tcgshop.dto.request.AddToCartRequest;
import com.tcgshop.dto.request.SelectAllRequest;
import com.tcgshop.dto.request.UpdateCartItemRequest;
import com.tcgshop.dto.request.UpdateSelectedRequest;
import com.tcgshop.dto.response.AddToCartResponse;
import com.tcgshop.dto.response.CartItemResponse;
import com.tcgshop.dto.response.CartResponse;
import com.tcgshop.dto.response.CartShopGroupResponse;
import com.tcgshop.entity.Cart;
import com.tcgshop.entity.CartItem;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.CartMapper;
import com.tcgshop.repository.CartItemRepository;
import com.tcgshop.repository.CartRepository;
import com.tcgshop.repository.ProductImageRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CartService;
import com.tcgshop.service.PromotionService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

	private static final String ADDED_MESSAGE = "Đã thêm vào giỏ hàng";
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductRepository productRepository;
	private final ProductImageRepository productImageRepository;
	private final PromotionService promotionService;
	private final CurrentUser currentUser;
	private final CartMapper cartMapper;

	public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
			ProductRepository productRepository, ProductImageRepository productImageRepository,
			PromotionService promotionService, CurrentUser currentUser, CartMapper cartMapper) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.productRepository = productRepository;
		this.productImageRepository = productImageRepository;
		this.promotionService = promotionService;
		this.currentUser = currentUser;
		this.cartMapper = cartMapper;
	}

	@Override
	@Transactional
	public AddToCartResponse addItem(AddToCartRequest request) {
		requireUserRole();
		Cart cart = getOrCreateCart();
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", request.productId()));
		validatePurchasable(product);

		CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()).orElse(null);
		int existingQuantity = item == null ? 0 : item.getQuantity();
		int requestedQuantity = request.quantity();
		validateStock(product, existingQuantity + requestedQuantity, existingQuantity, existingQuantity > 0);
		BigDecimal effectivePrice = promotionService.getEffectivePrice(product.getId());

		if (item == null) {
			item = new CartItem();
			item.setCart(cart);
			item.setProduct(product);
			item.setQuantity(requestedQuantity);
			item.setSelected(true);
		} else {
			item.setQuantity(existingQuantity + requestedQuantity);
		}
		// Giá snapshot phải lấy từ DB/service ở thời điểm thao tác; không nhận giá do client gửi lên.
		item.setPriceWhenAdded(effectivePrice);
		try {
			// saveAndFlush buộc lỗi unique xảy ra ngay tại đây để có thể thử đọc dòng do request khác tạo.
			cartItemRepository.saveAndFlush(item);
		} catch (DataIntegrityViolationException conflict) {
			// Phục hồi này là best-effort: một số JPA provider đánh dấu transaction rollback-only sau lỗi SQL,
			// nên đọc/cập nhật tiếp trong cùng transaction có thể vẫn thất bại khi commit. Muốn bảo đảm cần
			// retry trong transaction mới hoặc dùng câu UPDATE atomic ở repository.
			CartItem concurrentItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
					.orElse(null);
			if (concurrentItem == null) {
				throw conflict;
			}
			validateStock(product, concurrentItem.getQuantity() + requestedQuantity,
					concurrentItem.getQuantity(), true);
			concurrentItem.setQuantity(concurrentItem.getQuantity() + requestedQuantity);
			concurrentItem.setPriceWhenAdded(effectivePrice);
			cartItemRepository.save(concurrentItem);
		}
		return new AddToCartResponse(true, ADDED_MESSAGE, cartItemRepository.countByCartId(cart.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public CartResponse getCart() {
		requireUserRole();
		Optional<Cart> cart = cartRepository.findByUserId(currentUser.getId());
		return cart.map(value -> buildCart(cartItemRepository.findAllByCartIdOrderByIdDesc(value.getId())))
				.orElseGet(() -> emptyCart());
	}

	@Override
	@Transactional(readOnly = true)
	public int getCartCount() {
		requireUserRole();
		return cartRepository.findByUserId(currentUser.getId())
				.map(cart -> Math.toIntExact(cartItemRepository.countByCartId(cart.getId())))
				.orElse(0);
	}

	@Override
	@Transactional
	public CartResponse updateQuantity(Long itemId, UpdateCartItemRequest request) {
		requireUserRole();
		CartItem item = findOwnedItem(itemId);
		Product product = item.getProduct();
		validatePurchasable(product);
		validateStock(product, request.quantity(), 0, false);
		item.setQuantity(request.quantity());
		item.setPriceWhenAdded(promotionService.getEffectivePrice(product.getId()));
		cartItemRepository.save(item);
		return getCartForCurrentUser();
	}

	@Override
	@Transactional
	public CartResponse updateSelected(Long itemId, UpdateSelectedRequest request) {
		requireUserRole();
		CartItem item = findOwnedItem(itemId);
		if (request.selected() && !isAvailable(item.getProduct())) {
			throw new BadRequestException("Không thể chọn sản phẩm không còn khả dụng");
		}
		item.setSelected(request.selected());
		cartItemRepository.save(item);
		return getCartForCurrentUser();
	}

	@Override
	@Transactional
	public CartResponse selectAll(SelectAllRequest request) {
		requireUserRole();
		Optional<Cart> cart = cartRepository.findByUserId(currentUser.getId());
		if (cart.isEmpty()) {
			return emptyCart();
		}
		List<CartItem> items = cartItemRepository.findAllByCartIdOrderByIdDesc(cart.get().getId());
		for (CartItem item : items) {
			if (request.shopId() != null && !request.shopId().equals(item.getProduct().getShop().getId())) {
				continue;
			}
			item.setSelected(request.selected() && isAvailable(item.getProduct()));
		}
		cartItemRepository.saveAll(items);
		return buildCart(items);
	}

	@Override
	@Transactional
	public CartResponse removeItem(Long itemId) {
		requireUserRole();
		cartItemRepository.delete(findOwnedItem(itemId));
		return getCartForCurrentUser();
	}

	@Override
	@Transactional
	public CartResponse removeUnavailableItems() {
		requireUserRole();
		Optional<Cart> cart = cartRepository.findByUserId(currentUser.getId());
		if (cart.isEmpty()) {
			return emptyCart();
		}
		List<CartItem> items = cartItemRepository.findAllByCartIdOrderByIdDesc(cart.get().getId());
		cartItemRepository.deleteAll(items.stream().filter(item -> !isAvailable(item.getProduct())).toList());
		return getCartForCurrentUser();
	}

	private Cart getOrCreateCart() {
		Long userId = currentUser.getId();
		return cartRepository.findByUserId(userId).orElseGet(() -> {
			Cart newCart = new Cart();
			com.tcgshop.entity.User user = new com.tcgshop.entity.User();
			user.setId(userId);
			newCart.setUser(user);
			try {
				return cartRepository.saveAndFlush(newCart);
			} catch (DataIntegrityViolationException conflict) {
				// Unique user_id сериализует гонку создания; повторное чтение возвращает корзину-победителя.
				return cartRepository.findByUserId(userId).orElseThrow(() -> conflict);
			}
		});
	}

	private void validatePurchasable(Product product) {
		if (Boolean.TRUE.equals(product.getDeleted())) {
			throw new BadRequestException("Sản phẩm không còn bán");
		}
		if (product.getStatus() != ProductStatus.ACTIVE) {
			throw new BadRequestException("Sản phẩm không còn bán");
		}
		Shop shop = product.getShop();
		if (shop == null || Boolean.TRUE.equals(shop.getDeleted()) || shop.getStatus() != ShopStatus.APPROVED) {
			throw new BadRequestException("Cửa hàng hiện không được phép bán sản phẩm");
		}
		if (shop.getOwner() != null && currentUser.getId().equals(shop.getOwner().getId())) {
			throw new BadRequestException("Bạn không thể mua sản phẩm của cửa hàng của chính mình");
		}
		if (product.getStock() == null || product.getStock() <= 0) {
			throw new BadRequestException("Sản phẩm hiện đã hết hàng");
		}
	}

	private void validateStock(Product product, int requestedTotal, int existingQuantity, boolean addingQuantity) {
		int stock = product.getStock() == null ? 0 : product.getStock();
		if (requestedTotal > stock) {
			if (addingQuantity) {
				throw new BadRequestException("Chỉ còn " + stock + " sản phẩm trong kho (bạn đã có "
						+ existingQuantity + " trong giỏ)");
			}
			throw new BadRequestException("Chỉ còn " + stock + " sản phẩm trong kho");
		}
	}

	private CartItem findOwnedItem(Long itemId) {
		return cartItemRepository.findByIdAndCartUserId(itemId, currentUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Dòng giỏ hàng", itemId));
	}

	private CartResponse getCartForCurrentUser() {
		return cartRepository.findByUserId(currentUser.getId())
				.map(cart -> buildCart(cartItemRepository.findAllByCartIdOrderByIdDesc(cart.getId())))
				.orElseGet(() -> emptyCart());
	}

	private CartResponse buildCart(List<CartItem> items) {
		if (items.isEmpty()) {
			return emptyCart();
		}
		Set<Long> productIds = new LinkedHashSet<>();
		for (CartItem item : items) {
			productIds.add(item.getProduct().getId());
		}
		// Lấy giá hiện tại và ảnh trong batch để tránh N+1 khi giỏ có nhiều sản phẩm.
		Map<Long, BigDecimal> effectivePrices = promotionService.getEffectivePrices(productIds);
		Map<Long, String> imageUrls = new LinkedHashMap<>();
		for (ProductImage image : productImageRepository.findPrimaryOrFirstImages(productIds)) {
			imageUrls.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
		}

		Map<Long, List<CartItemResponse>> grouped = new LinkedHashMap<>();
		Map<Long, BigDecimal> subtotals = new LinkedHashMap<>();
		Map<Long, String> shopNames = new LinkedHashMap<>();
		Map<Long, Boolean> allSelected = new LinkedHashMap<>();
		long selectedCount = 0;
		boolean hasUnavailable = false;
		BigDecimal total = BigDecimal.ZERO;
		for (CartItem item : items) {
			Product product = item.getProduct();
			Shop shop = product.getShop();
			Long shopId = shop == null ? null : shop.getId();
			BigDecimal currentPrice = effectivePrices.getOrDefault(product.getId(), product.getPrice());
			boolean available = isAvailable(product);
			boolean exceedsStock = product.getStock() != null && item.getQuantity() > product.getStock();
			String unavailableReason = unavailableReason(product);
			boolean priceChanged = item.getPriceWhenAdded() != null
					&& currentPrice.compareTo(item.getPriceWhenAdded()) != 0;
			String image = imageUrls.get(product.getId());
			if (image == null && product.getType() == ProductType.SINGLE && product.getCard() != null) {
				image = product.getCard().getImageUrl();
			}
			CartItemResponse mapped = cartMapper.toResponse(item);
			CartItemResponse response = new CartItemResponse(mapped.itemId(), mapped.productId(), mapped.productName(),
					image, mapped.cardCondition(), currentPrice, mapped.originalPrice(), mapped.quantity(), mapped.stock(),
						currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())), item.isSelected() && available,
						available, unavailableReason, exceedsStock, priceChanged,
						priceChanged ? item.getPriceWhenAdded() : null);
			grouped.computeIfAbsent(shopId, ignored -> new ArrayList<>()).add(response);
			shopNames.putIfAbsent(shopId, shop == null ? "Cửa hàng không tồn tại" : shop.getName());
			subtotals.putIfAbsent(shopId, BigDecimal.ZERO);
			allSelected.putIfAbsent(shopId, true);
			if (!available || exceedsStock) {
				hasUnavailable = true;
				allSelected.put(shopId, false);
			}
			if (item.isSelected() && available && !exceedsStock) {
				selectedCount++;
				BigDecimal line = currentPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
				subtotals.put(shopId, subtotals.get(shopId).add(line));
				total = total.add(line);
			} else {
				allSelected.put(shopId, false);
			}
		}
		List<CartShopGroupResponse> shops = grouped.entrySet().stream()
				.map(entry -> new CartShopGroupResponse(entry.getKey(), shopNames.get(entry.getKey()), entry.getValue(),
						subtotals.get(entry.getKey()), allSelected.get(entry.getKey())))
				.toList();
		return new CartResponse(shops, items.size(), selectedCount, total, hasUnavailable);
	}

	private boolean isAvailable(Product product) {
		Shop shop = product.getShop();
		return !Boolean.TRUE.equals(product.getDeleted()) && product.getStatus() == ProductStatus.ACTIVE
				&& shop != null && !Boolean.TRUE.equals(shop.getDeleted()) && shop.getStatus() == ShopStatus.APPROVED
				&& product.getStock() != null && product.getStock() > 0;
	}

	private String unavailableReason(Product product) {
		if (Boolean.TRUE.equals(product.getDeleted())) {
			return "Sản phẩm không còn bán";
		}
		if (product.getStatus() != ProductStatus.ACTIVE) {
			return "Sản phẩm tạm ngừng bán";
		}
		Shop shop = product.getShop();
		if (shop == null || Boolean.TRUE.equals(shop.getDeleted()) || shop.getStatus() != ShopStatus.APPROVED) {
			return "Cửa hàng tạm ngừng hoạt động";
		}
		if (product.getStock() == null || product.getStock() <= 0) {
			return "Hết hàng";
		}
		return null;
	}

	private void requireUserRole() {
		if (!currentUser.hasRole(RoleName.ROLE_USER)) {
			throw new com.tcgshop.exception.ForbiddenException("Tài khoản không có quyền ROLE_USER");
		}
	}

	private CartResponse emptyCart() {
		return new CartResponse(List.of(), 0, 0, BigDecimal.ZERO, false);
	}
}
