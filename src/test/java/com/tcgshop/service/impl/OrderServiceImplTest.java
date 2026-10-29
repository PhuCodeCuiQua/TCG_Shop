package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcgshop.config.OrderProperties;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.OrderStatusHistory;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.event.OrderStatusChangedEvent;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.repository.OrderItemQuantity;
import com.tcgshop.repository.OrderItemRepository;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
	@Mock private OrderRepository orderRepository;
	@Mock private OrderStatusHistoryRepository historyRepository;
	@Mock private UserRepository userRepository;
	@Mock private CurrentUser currentUser;
	@Mock private OrderStatusSideEffectHandler sideEffectHandler;
	@Mock private OrderItemRepository orderItemRepository;
	@Mock private ProductRepository productRepository;
	@Mock private com.tcgshop.service.CommissionService commissionService;
	@Mock private ApplicationEventPublisher eventPublisher;

	private OrderServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new OrderServiceImpl(orderRepository, historyRepository, userRepository, currentUser,
				new OrderTransitionPolicy(), sideEffectHandler, eventPublisher);
	}

	@Test
	void shopOwnerConfirmsOrderAndPublishesStatusEvent() {
		Order order = order(OrderStatus.NEW, 11L, 21L, null);
		User owner = user(21L, "Chủ shop", UserStatus.ACTIVE, Set.of());
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.getId()).thenReturn(21L);
		when(currentUser.hasRole(any(RoleName.class))).thenReturn(false);
		when(userRepository.findById(21L)).thenReturn(Optional.of(owner));

		service.changeStatus(1L, OrderStatus.CONFIRMED, null);

		assertEquals(OrderStatus.CONFIRMED, order.getStatus());
		verify(sideEffectHandler).validateBefore(order, OrderStatus.NEW, OrderStatus.CONFIRMED);
		verify(sideEffectHandler).apply(order, OrderStatus.NEW, OrderStatus.CONFIRMED, null);
		ArgumentCaptor<OrderStatusChangedEvent> event = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
		verify(eventPublisher).publishEvent(event.capture());
		assertEquals(OrderStatus.NEW, event.getValue().oldStatus());
		assertEquals(OrderStatus.CONFIRMED, event.getValue().newStatus());
		assertEquals(21L, event.getValue().actorId());
		verify(historyRepository).save(any(OrderStatusHistory.class));
	}

	@Test
	void differentShopOwnerCannotConfirmOrder() {
		Order order = order(OrderStatus.NEW, 11L, 21L, null);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.getId()).thenReturn(99L);
		when(currentUser.hasRole(any(RoleName.class))).thenReturn(false);

		assertThrows(ForbiddenException.class,
				() -> service.changeStatus(1L, OrderStatus.CONFIRMED, null));
		verifyNoInteractions(sideEffectHandler);
		verify(historyRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void cancellationWithoutRequiredReasonIsRejected() {
		Order order = order(OrderStatus.NEW, 11L, 21L, null);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.getId()).thenReturn(21L);
		when(currentUser.hasRole(any(RoleName.class))).thenReturn(false);

		assertThrows(BadRequestException.class,
				() -> service.changeStatus(1L, OrderStatus.CANCELLED, "  "));
		verifyNoInteractions(sideEffectHandler);
		verify(historyRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void shipperNotAssignedToOrderCannotMarkItPickedUp() {
		Order order = order(OrderStatus.CONFIRMED, 11L, 21L, 50L);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.getId()).thenReturn(51L);
		when(currentUser.hasRole(any(RoleName.class))).thenReturn(false);

		assertThrows(ForbiddenException.class,
				() -> service.changeStatus(1L, OrderStatus.PICKED_UP, null));
		verifyNoInteractions(sideEffectHandler);
		verify(historyRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void validatesBeforeStatusChangeAndAppliesBeforeHistoryAndEvent() {
		Order order = order(OrderStatus.NEW, 11L, 21L, null);
		User owner = user(21L, "Chủ shop", UserStatus.ACTIVE, Set.of());
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.getId()).thenReturn(21L);
		when(currentUser.hasRole(any(RoleName.class))).thenReturn(false);
		when(userRepository.findById(21L)).thenReturn(Optional.of(owner));
		doAnswer(invocation -> {
			assertEquals(OrderStatus.NEW, order.getStatus());
			return null;
		}).when(sideEffectHandler).validateBefore(order, OrderStatus.NEW, OrderStatus.CONFIRMED);
		doAnswer(invocation -> {
			assertEquals(OrderStatus.CONFIRMED, order.getStatus());
			return null;
		}).when(sideEffectHandler).apply(order, OrderStatus.NEW, OrderStatus.CONFIRMED, null);

		var ordered = inOrder(sideEffectHandler, orderRepository, historyRepository, eventPublisher);
		service.changeStatus(1L, OrderStatus.CONFIRMED, null);

		ordered.verify(sideEffectHandler).validateBefore(order, OrderStatus.NEW, OrderStatus.CONFIRMED);
		ordered.verify(sideEffectHandler).apply(order, OrderStatus.NEW, OrderStatus.CONFIRMED, null);
		ordered.verify(orderRepository).save(order);
		ordered.verify(historyRepository).save(any(OrderStatusHistory.class));
		ordered.verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class));
	}

	@Test
	void sideEffectFailurePreventsSavingHistoryAndPublishingEvent() {
		Order order = order(OrderStatus.NEW, 11L, 21L, null);
		User owner = user(21L, "Chủ shop", UserStatus.ACTIVE, Set.of());
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.getId()).thenReturn(21L);
		when(currentUser.hasRole(any(RoleName.class))).thenReturn(false);
		when(userRepository.findById(21L)).thenReturn(Optional.of(owner));
		doThrow(new IllegalStateException("side effect failed"))
				.when(sideEffectHandler).apply(order, OrderStatus.NEW, OrderStatus.CONFIRMED, null);

		assertThrows(IllegalStateException.class,
				() -> service.changeStatus(1L, OrderStatus.CONFIRMED, null));
		verify(sideEffectHandler).validateBefore(order, OrderStatus.NEW, OrderStatus.CONFIRMED);
		verify(orderRepository, never()).save(any(Order.class));
		verify(historyRepository, never()).save(any());
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void invalidTransitionDoesNotCallSideEffectHandler() {
		Order order = order(OrderStatus.NEW, 11L, 21L, null);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

		assertThrows(BusinessException.class,
				() -> service.changeStatus(1L, OrderStatus.SHIPPING, null));
		verifyNoInteractions(sideEffectHandler);
	}

	@Test
	void cancellingAgainDoesNotRestoreStockTwice() {
		Order order = order(OrderStatus.NEW, 11L, 21L, null);
		com.tcgshop.entity.OrderGroup group = new com.tcgshop.entity.OrderGroup();
		group.setPaymentMethod(com.tcgshop.entity.enums.PaymentMethod.COD);
		order.setOrderGroup(group);
		order.setTotalAmount(new BigDecimal("100"));
		OrderItemQuantity quantity = org.mockito.Mockito.mock(OrderItemQuantity.class);
		when(quantity.getProductId()).thenReturn(77L);
		when(quantity.getQuantity()).thenReturn(2L);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.getId()).thenReturn(11L);
		when(userRepository.findById(11L)).thenReturn(Optional.of(order.getUser()));
		when(orderItemRepository.findQuantitiesByOrderId(1L)).thenReturn(java.util.List.of(quantity));
		when(productRepository.increaseStock(77L, 2)).thenReturn(1);
		OrderStatusSideEffectHandler realHandler = new OrderStatusSideEffectHandler(
				orderItemRepository, productRepository, commissionService, new OrderProperties(7, 3),
				Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC),
				org.mockito.Mockito.mock(com.tcgshop.repository.ReturnRequestRepository.class),
				new ReturnWindowPolicy(new OrderProperties(7, 3),
						Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC)));
		service = new OrderServiceImpl(orderRepository, historyRepository, userRepository, currentUser,
				new OrderTransitionPolicy(), realHandler, eventPublisher);

		service.changeStatus(1L, OrderStatus.CANCELLED, "  Khách yêu cầu hủy  ");
		assertThrows(BusinessException.class,
				() -> service.changeStatus(1L, OrderStatus.CANCELLED, "Hủy lần nữa"));

		verify(productRepository).increaseStock(77L, 2);
	}

	@Test
	void assigningUserWithoutShipperRoleIsRejected() {
		Order order = order(OrderStatus.CONFIRMED, 11L, 21L, null);
		User notShipper = user(50L, "Người dùng", UserStatus.ACTIVE, Set.of());
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(userRepository.findById(50L)).thenReturn(Optional.of(notShipper));

		assertThrows(BadRequestException.class, () -> service.assignShipper(1L, 50L));
		verify(historyRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void assigningShipperToNonConfirmedOrderIsRejected() {
		Order order = order(OrderStatus.NEW, 11L, 21L, null);
		when(currentUser.isAuthenticated()).thenReturn(true);
		when(currentUser.hasRole(RoleName.ROLE_MANAGER)).thenReturn(true);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

		assertThrows(BusinessException.class, () -> service.assignShipper(1L, 50L));
		verify(userRepository, never()).findById(50L);
	}

	private Order order(OrderStatus status, Long buyerId, Long ownerId, Long shipperId) {
		User buyer = user(buyerId, "Người mua", UserStatus.ACTIVE, Set.of());
		User owner = user(ownerId, "Chủ shop", UserStatus.ACTIVE, Set.of());
		Shop shop = new Shop();
		shop.setId(31L);
		shop.setOwner(owner);
		Order order = new Order();
		order.setId(1L);
		order.setOrderCode("ORD-1");
		order.setUser(buyer);
		order.setShop(shop);
		order.setShipper(shipperId == null ? null : user(shipperId, "Shipper", UserStatus.ACTIVE, Set.of()));
		order.setStatus(status);
		return order;
	}

	private User user(Long id, String name, UserStatus status, Set<RoleName> roleNames) {
		User user = new User();
		user.setId(id);
		user.setFullName(name);
		user.setStatus(status);
		Set<Role> roles = new HashSet<>();
		for (RoleName roleName : roleNames) {
			Role role = new Role();
			role.setName(roleName);
			roles.add(role);
		}
		user.setRoles(roles);
		return user;
	}
}
