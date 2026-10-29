package com.tcgshop.service.impl;

import com.tcgshop.dto.request.AddToCartRequest;
import com.tcgshop.dto.response.OrderDetailResponse;
import com.tcgshop.dto.response.OrderDetailItemResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.service.CartService;
import com.tcgshop.service.UserOrderService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderReorderServiceImplTest {
    private final UserOrderService orders = mock(UserOrderService.class);
    private final CartService cart = mock(CartService.class);
    private final OrderReorderServiceImpl service = new OrderReorderServiceImpl(orders, cart);
    private final OrderDetailResponse order = mock(OrderDetailResponse.class);

    @BeforeEach
    void setUp() { when(orders.getMyOrder(7L)).thenReturn(order); }

    @Test
    void addsOriginalQuantitiesAndContinuesAfterUnavailableProduct() {
        when(order.status()).thenReturn(OrderStatus.DELIVERED);
        when(order.items()).thenReturn(List.of(item(1L, 2), item(2L, 1), item(3L, 4)));
        doThrow(new BadRequestException("Sản phẩm đã hết hàng"))
                .when(cart).addItem(new AddToCartRequest(2L, 1));
        var result = service.reorder(7L);
        assertEquals(2, result.addedItemCount());
        assertEquals(List.of("Product 2: Sản phẩm đã hết hàng"), result.skippedItems());
        verify(cart).addItem(new AddToCartRequest(1L, 2));
        verify(cart).addItem(new AddToCartRequest(3L, 4));
    }

    @Test
    void deniesNonDeliveredOrderBeforeTouchingCart() {
        when(order.status()).thenReturn(OrderStatus.NEW);
        assertThrows(BadRequestException.class, () -> service.reorder(7L));
        verifyNoInteractions(cart);
    }

    @Test
    void propagatesOwnershipFailureBeforeTouchingCart() {
        when(orders.getMyOrder(7L)).thenThrow(new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        assertThrows(ResourceNotFoundException.class, () -> service.reorder(7L));
        verifyNoInteractions(cart);
    }

    @Test
    void skipsMissingProductsAndInvalidHistoricalQuantities() {
        when(order.status()).thenReturn(OrderStatus.DELIVERED);
        when(order.items()).thenReturn(List.of(item(null, 1), item(2L, 0), item(3L, 100), item(4L, 1)));
        doThrow(new ResourceNotFoundException("Sản phẩm không còn tồn tại"))
                .when(cart).addItem(new AddToCartRequest(4L, 1));
        var result = service.reorder(7L);
        assertEquals(0, result.addedItemCount());
        assertEquals(4, result.skippedItems().size());
        verify(cart, times(1)).addItem(any());
    }

    private OrderDetailItemResponse item(Long id, int quantity) {
        return new OrderDetailItemResponse(id, "Product " + id, null, null, null, quantity, null);
    }
}
