package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.CancelOrderRequest;
import com.tcgshop.dto.response.CancelOrderResultResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.CancelReason;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.OrderRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.OrderService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.Mockito.mock;

class OrderCancellationServiceImplTest {
	private static final Long ORDER_ID = 91L;
	private static final Long BUYER_ID = 12L;
	private static final String ORDER_CODE = "ORDER-IT-91";
	private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
	private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

	private final OrderRepository orderRepository = mock(OrderRepository.class);
	private final CurrentUser currentUser = mock(CurrentUser.class);
	private final OrderService orderService = mock(OrderService.class);
	private final OrderTransitionPolicy policy = new OrderTransitionPolicy();
	private final OrderCancellationServiceImpl service = new OrderCancellationServiceImpl(
			orderRepository, currentUser, policy, orderService);

	@BeforeEach
	void setUp() {
		when(currentUser.getId()).thenReturn(BUYER_ID);
	}

	@AfterAll
	static void closeValidatorFactory() {
		VALIDATOR_FACTORY.close();
	}

	@Test
	void missingOrForeignOrderIsNotFoundWithoutCallingOrderService() {
		when(orderRepository.findByIdAndUserId(ORDER_ID, BUYER_ID)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.cancelMyOrder(ORDER_ID, request(CancelReason.OTHER, "A reason long enough")));
		verifyNoInteractions(orderService);
	}

	@Test
	void newAndConfirmedOrdersUseReasonLabelAndNormalizedDetail() {
		for (OrderStatus status : new OrderStatus[] {OrderStatus.NEW, OrderStatus.CONFIRMED}) {
			Order order = order(status);
			when(orderRepository.findByIdAndUserId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order));
			CancelOrderRequest request = request(CancelReason.OTHER, "  Need   a different   item  ");

			CancelOrderResultResponse result = service.cancelMyOrder(ORDER_ID, request);

			assertEquals(new CancelOrderResultResponse(ORDER_ID, ORDER_CODE, false), result);
		}
		verify(orderService, times(2)).changeStatus(ORDER_ID, OrderStatus.CANCELLED,
				"Lý do khác: Need a different item");
		Order order = order(OrderStatus.NEW);
		when(orderRepository.findByIdAndUserId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order));
		service.cancelMyOrder(ORDER_ID, request(CancelReason.BETTER_PRICE, "   "));
		verify(orderService).changeStatus(ORDER_ID, OrderStatus.CANCELLED, CancelReason.BETTER_PRICE.getLabel());
	}

	@Test
	void alreadyCancelledReturnsIdempotentResultWithoutChangingStatusAgain() {
		when(orderRepository.findByIdAndUserId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));
		assertEquals(new CancelOrderResultResponse(ORDER_ID, ORDER_CODE, true),
				service.cancelMyOrder(ORDER_ID, request(CancelReason.CHANGED_MIND, null)));
		verify(orderService, never()).changeStatus(ORDER_ID, OrderStatus.CANCELLED, null);
	}

	@Test
	void laterStatusesAreRejectedWithDisplayLabel() {
		for (OrderStatus status : new OrderStatus[] {OrderStatus.PICKED_UP, OrderStatus.SHIPPING, OrderStatus.DELIVERED}) {
			when(orderRepository.findByIdAndUserId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order(status)));
			BusinessException exception = assertThrows(BusinessException.class,
					() -> service.cancelMyOrder(ORDER_ID, request(CancelReason.CHANGED_MIND, null)));
			assertTrue(exception.getMessage().contains(status.getDisplayName()));
		}
		verifyNoInteractions(orderService);
	}

	@Test
	void propagatesBusinessAndOptimisticLockFailuresUnchanged() {
		Order order = order(OrderStatus.NEW);
		when(orderRepository.findByIdAndUserId(ORDER_ID, BUYER_ID)).thenReturn(Optional.of(order));
		BusinessException business = new BusinessException("business failure");
		org.mockito.Mockito.doThrow(business).when(orderService)
				.changeStatus(ORDER_ID, OrderStatus.CANCELLED, CancelReason.CHANGED_MIND.getLabel());
		assertSame(business, assertThrows(BusinessException.class,
				() -> service.cancelMyOrder(ORDER_ID, request(CancelReason.CHANGED_MIND, null))));

		ObjectOptimisticLockingFailureException optimistic =
				new ObjectOptimisticLockingFailureException(Order.class, ORDER_ID);
		org.mockito.Mockito.doThrow(optimistic).when(orderService)
				.changeStatus(ORDER_ID, OrderStatus.CANCELLED, CancelReason.BETTER_PRICE.getLabel());
		assertSame(optimistic, assertThrows(ObjectOptimisticLockingFailureException.class,
				() -> service.cancelMyOrder(ORDER_ID, request(CancelReason.BETTER_PRICE, null))));
	}

	@Test
	void cancellationServiceIsTransactionalAndNotReadOnly() throws Exception {
		Transactional annotation = OrderCancellationServiceImpl.class.getAnnotation(Transactional.class);
		assertTrue(annotation != null);
		assertFalse(annotation.readOnly());
	}

	@Test
	void requestValidationCoversReasonOtherDetailsAndLengthLimits() {
		assertFalse(VALIDATOR.validate(new CancelOrderRequest()).isEmpty());
		assertFalse(VALIDATOR.validate(request(CancelReason.OTHER, null)).isEmpty());
		assertFalse(VALIDATOR.validate(request(CancelReason.OTHER, "123456789")).isEmpty());
		assertTrue(VALIDATOR.validate(request(CancelReason.OTHER, "1234567890")).isEmpty());
		assertTrue(VALIDATOR.validate(request(CancelReason.OTHER, "x".repeat(200))).isEmpty());
		assertFalse(VALIDATOR.validate(request(CancelReason.OTHER, "x".repeat(201))).isEmpty());
		assertTrue(VALIDATOR.validate(request(CancelReason.CHANGED_MIND, null)).isEmpty());
	}

	private Order order(OrderStatus status) {
		Order order = new Order();
		order.setId(ORDER_ID);
		order.setOrderCode(ORDER_CODE);
		order.setStatus(status);
		return order;
	}

	private CancelOrderRequest request(CancelReason reason, String detail) {
		CancelOrderRequest request = new CancelOrderRequest();
		request.setReason(reason);
		request.setDetail(detail);
		return request;
	}
}
