package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.AddToCartRequest;
import com.tcgshop.dto.request.UpdateCartItemRequest;
import com.tcgshop.dto.request.UpdateSelectedRequest;
import com.tcgshop.dto.response.CartResponse;
import com.tcgshop.entity.Cart;
import com.tcgshop.entity.CartItem;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.CardCondition;
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
import com.tcgshop.service.PromotionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

	@Mock private CartRepository cartRepository;
	@Mock private CartItemRepository cartItemRepository;
	@Mock private ProductRepository productRepository;
	@Mock private ProductImageRepository productImageRepository;
	@Mock private PromotionService promotionService;
	@Mock private CurrentUser currentUser;
	@InjectMocks private CartServiceImpl cartService;
	private CartMapper cartMapper;

	@BeforeEach
	void setUp() {
		cartMapper = Mappers.getMapper(CartMapper.class);
		cartService = new CartServiceImpl(cartRepository, cartItemRepository, productRepository,
				productImageRepository, promotionService, currentUser, cartMapper);
		when(currentUser.getId()).thenReturn(10L);
		when(currentUser.hasRole(RoleName.ROLE_USER)).thenReturn(true);
	}

	@Test
	void addsNewProductAndSnapshotsCurrentDatabasePrice() {
		Cart cart = cart(1L);
		Product product = product(5L, 8, 100);
		when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));
		when(cartItemRepository.findByCartIdAndProductId(1L, 5L)).thenReturn(Optional.empty());
		when(promotionService.getEffectivePrice(5L)).thenReturn(new BigDecimal("90.00"));
		when(cartItemRepository.saveAndFlush(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(cartItemRepository.countByCartId(1L)).thenReturn(1L);

		var response = cartService.addItem(new AddToCartRequest(5L, 2));

		assertTrue(response.success());
		assertEquals("Đã thêm vào giỏ hàng", response.message());
		assertEquals(1, response.cartCount());
		verify(cartItemRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(item ->
				item.getPriceWhenAdded().compareTo(new BigDecimal("90.00")) == 0));
	}

	@Test
	void addsQuantityToExistingLine() {
		Cart cart = cart(1L);
		Product product = product(5L, 8, 100);
		CartItem existing = item(20L, cart, product, 2, true, new BigDecimal("100.00"));
		when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));
		when(cartItemRepository.findByCartIdAndProductId(1L, 5L)).thenReturn(Optional.of(existing));
		when(promotionService.getEffectivePrice(5L)).thenReturn(new BigDecimal("90.00"));
		when(cartItemRepository.saveAndFlush(existing)).thenReturn(existing);
		when(cartItemRepository.countByCartId(1L)).thenReturn(1L);

		cartService.addItem(new AddToCartRequest(5L, 3));

		assertEquals(5, existing.getQuantity());
		assertEquals(0, existing.getPriceWhenAdded().compareTo(new BigDecimal("90.00")));
	}

	@Test
	void rejectsQuantityOverStockWithExistingQuantityInMessage() {
		Cart cart = cart(1L);
		Product product = product(5L, 4, 100);
		when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));
		when(cartItemRepository.findByCartIdAndProductId(1L, 5L))
				.thenReturn(Optional.of(item(20L, cart, product, 3, true, BigDecimal.TEN)));

		BadRequestException error = assertThrows(BadRequestException.class,
				() -> cartService.addItem(new AddToCartRequest(5L, 2)));

		assertEquals("Chỉ còn 4 sản phẩm trong kho (bạn đã có 3 trong giỏ)", error.getMessage());
	}

	@Test
	void rejectsOwnShopProduct() {
		Cart cart = cart(1L);
		Product product = product(5L, 8, 100);
		product.getShop().getOwner().setId(10L);
		when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));

		BadRequestException error = assertThrows(BadRequestException.class,
				() -> cartService.addItem(new AddToCartRequest(5L, 1)));

		assertEquals("Bạn không thể mua sản phẩm của cửa hàng của chính mình", error.getMessage());
	}

	@Test
	void rejectsDeletedInactiveUnapprovedAndOutOfStockProducts() {
		Cart cart = cart(1L);
		Product product = product(5L, 8, 100);
		when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));
		product.setDeleted(true);
		assertThrows(BadRequestException.class, () -> cartService.addItem(new AddToCartRequest(5L, 1)));
		product.setDeleted(false);
		product.setStatus(ProductStatus.INACTIVE);
		assertThrows(BadRequestException.class, () -> cartService.addItem(new AddToCartRequest(5L, 1)));
		product.setStatus(ProductStatus.ACTIVE);
		product.getShop().setStatus(ShopStatus.PENDING);
		assertThrows(BadRequestException.class, () -> cartService.addItem(new AddToCartRequest(5L, 1)));
		product.getShop().setStatus(ShopStatus.APPROVED);
		product.setStock(0);
		assertThrows(BadRequestException.class, () -> cartService.addItem(new AddToCartRequest(5L, 1)));
		verify(promotionService, never()).getEffectivePrice(anyLong());
	}

	@Test
	void recoversConcurrentCartCreationAndItemInsert() {
		Cart cart = cart(1L);
		Product product = product(5L, 8, 100);
		CartItem concurrent = item(20L, cart, product, 2, true, BigDecimal.TEN);
		when(cartRepository.findByUserId(10L)).thenReturn(Optional.empty(), Optional.of(cart));
		when(cartRepository.saveAndFlush(any(Cart.class))).thenThrow(new DataIntegrityViolationException("duplicate cart"));
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));
		when(cartItemRepository.findByCartIdAndProductId(1L, 5L)).thenReturn(Optional.empty(), Optional.of(concurrent));
		when(promotionService.getEffectivePrice(5L)).thenReturn(new BigDecimal("90.00"));
		when(cartItemRepository.saveAndFlush(any(CartItem.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate line"));
		when(cartItemRepository.save(concurrent)).thenReturn(concurrent);
		when(cartItemRepository.countByCartId(1L)).thenReturn(1L);

		cartService.addItem(new AddToCartRequest(5L, 3));

		assertEquals(5, concurrent.getQuantity());
		verify(cartRepository, times(2)).findByUserId(10L);
	}

	@Test
	void updateQuantityDoesNotRevealAnotherUsersLine() {
		when(cartItemRepository.findByIdAndCartUserId(20L, 10L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> cartService.updateQuantity(20L, new UpdateCartItemRequest(2)));
	}

	@Test
	void getCartGroupsByShopAndCalculatesValidSelectedSubtotalAndWarnings() {
		Cart cart = cart(1L);
		Product valid = product(5L, 4, 100);
		Product tooMany = product(6L, 1, 50);
		Product unavailable = product(7L, 3, 30);
		unavailable.setStatus(ProductStatus.INACTIVE);
		List<CartItem> items = List.of(item(1L, cart, valid, 2, true, new BigDecimal("100.00")),
				item(2L, cart, tooMany, 2, true, new BigDecimal("50.00")),
				item(3L, cart, unavailable, 1, true, new BigDecimal("30.00")));
		when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findAllByCartIdOrderByIdDesc(1L)).thenReturn(items);
		when(promotionService.getEffectivePrices(anyCollection())).thenReturn(Map.of(5L, new BigDecimal("90.00"),
				6L, new BigDecimal("50.00"), 7L, new BigDecimal("30.00")));
		when(productImageRepository.findPrimaryOrFirstImages(anyCollection())).thenReturn(List.of());

		CartResponse response = cartService.getCart();

		assertEquals(3, response.totalItemCount());
		assertEquals(1, response.selectedItemCount());
		assertEquals(new BigDecimal("180.00"), response.selectedSubtotal());
		assertTrue(response.hasUnavailableItems());
		assertEquals(3, response.shops().get(0).items().size());
		assertTrue(response.shops().get(0).items().get(1).quantityExceedsStock());
		assertTrue(response.shops().get(0).items().get(0).priceChanged());
		assertEquals(new BigDecimal("100.00"), response.shops().get(0).items().get(0).previousPrice());
		verify(promotionService, times(1)).getEffectivePrices(anyCollection());
	}

	@Test
	void cannotSelectUnavailableProduct() {
		Cart cart = cart(1L);
		Product product = product(5L, 2, 100);
		product.setStatus(ProductStatus.INACTIVE);
		when(cartItemRepository.findByIdAndCartUserId(20L, 10L))
				.thenReturn(Optional.of(item(20L, cart, product, 1, false, BigDecimal.TEN)));

		assertThrows(BadRequestException.class,
				() -> cartService.updateSelected(20L, new UpdateSelectedRequest(true)));
	}

	private static Cart cart(Long id) {
		Cart cart = new Cart();
		cart.setId(id);
		return cart;
	}

	private static Product product(Long id, int stock, int price) {
		Shop shop = new Shop();
		shop.setId(50L);
		shop.setName("Shop");
		shop.setStatus(ShopStatus.APPROVED);
		shop.setDeleted(false);
		shop.setOwner(new User());
		Product product = new Product();
		product.setId(id);
		product.setName("Card " + id);
		product.setType(ProductType.SINGLE);
		product.setCardCondition(CardCondition.NEAR_MINT);
		product.setPrice(BigDecimal.valueOf(price));
		product.setStock(stock);
		product.setStatus(ProductStatus.ACTIVE);
		product.setDeleted(false);
		product.setShop(shop);
		product.setCard(new Card());
		return product;
	}

	private static CartItem item(Long id, Cart cart, Product product, int quantity, boolean selected, BigDecimal price) {
		CartItem item = new CartItem();
		item.setId(id);
		item.setCart(cart);
		item.setProduct(product);
		item.setQuantity(quantity);
		item.setSelected(selected);
		item.setPriceWhenAdded(price);
		return item;
	}
}
