package com.tcgshop.service;

import com.tcgshop.dto.request.AddToCartRequest;
import com.tcgshop.dto.request.SelectAllRequest;
import com.tcgshop.dto.request.UpdateCartItemRequest;
import com.tcgshop.dto.request.UpdateSelectedRequest;
import com.tcgshop.dto.response.AddToCartResponse;
import com.tcgshop.dto.response.CartResponse;

public interface CartService {
	AddToCartResponse addItem(AddToCartRequest request);
	CartResponse getCart();
	int getCartCount();
	CartResponse updateQuantity(Long itemId, UpdateCartItemRequest request);
	CartResponse updateSelected(Long itemId, UpdateSelectedRequest request);
	CartResponse selectAll(SelectAllRequest request);
	CartResponse removeItem(Long itemId);
	CartResponse removeUnavailableItems();
}
