package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.VendorOrderMapper;
import com.tcgshop.repository.OrderStatusHistoryRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.VendorOrderRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.OrderService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorOrderOwnershipTest {
	private static final Long CURRENT_VENDOR_ID = 11L;
	private static final Long OTHER_VENDOR_ID = 22L;
	private static final Long ORDER_ID = 400L;

	@Mock private VendorOrderRepository orderRepository;
	@Mock private OrderStatusHistoryRepository historyRepository;
	@Mock private ShopRepository shopRepository;
	@Mock private VendorOrderMapper mapper;
	@Mock private CurrentUser currentUser;
	@Mock private OrderService orderService;
	@InjectMocks private VendorOrderServiceImpl service;

	@Test
	void viewingOrderFromAnotherVendorsShopIsForbidden() {
		User currentOwner = new User();
		currentOwner.setId(CURRENT_VENDOR_ID);
		Shop currentShop = new Shop();
		currentShop.setId(100L);
		currentShop.setOwner(currentOwner);

		User otherOwner = new User();
		otherOwner.setId(OTHER_VENDOR_ID);
		Shop otherShop = new Shop();
		otherShop.setId(200L);
		otherShop.setOwner(otherOwner);
		Order otherShopOrder = new Order();
		otherShopOrder.setShop(otherShop);

		when(currentUser.hasRole(RoleName.ROLE_VENDOR)).thenReturn(true);
		when(currentUser.getId()).thenReturn(CURRENT_VENDOR_ID);
		when(shopRepository.findByOwnerId(CURRENT_VENDOR_ID)).thenReturn(Optional.of(currentShop));
		when(orderRepository.findDetailById(ORDER_ID)).thenReturn(Optional.of(otherShopOrder));

		assertThatThrownBy(() -> service.getOrderDetail(ORDER_ID))
				.isInstanceOf(ForbiddenException.class);
	}
}
