package com.tcgshop.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.ShipperOrderDetailResponse;
import com.tcgshop.dto.response.ShipperOrderItemResponse;
import com.tcgshop.dto.response.ShipperOrderListResponse;
import com.tcgshop.dto.response.ShipperOrderSummaryResponse;
import com.tcgshop.dto.response.ShipperOrderTabResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.service.ShipperOrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShipperOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShipperOrderControllerMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ShipperOrderService shipperOrderService;

	@Test
	void ordersPageRendersTabsTableStatusAndPagination() throws Exception {
		ShipperOrderSummaryResponse summary = new ShipperOrderSummaryResponse(
				9L, "ORD-000009", LocalDateTime.of(2026, 10, 25, 10, 30),
				"12 Đường Thẻ Bài, Hà Nội", new BigDecimal("250000"),
				OrderStatus.CONFIRMED, "Đã xác nhận", "info");
		PageResponse<ShipperOrderSummaryResponse> page = new PageResponse<>(
				List.of(summary), 0, 10, 11, 2, true, false);
		when(shipperOrderService.getOrders(eq("pickup"), any())).thenReturn(new ShipperOrderListResponse(
				List.of(
						new ShipperOrderTabResponse("pickup", "Cần lấy hàng", 1, true),
						new ShipperOrderTabResponse("shipping", "Đang giao", 2, false),
						new ShipperOrderTabResponse("completed", "Hoàn tất", 3, false),
						new ShipperOrderTabResponse("failed", "Thất bại", 0, false),
						new ShipperOrderTabResponse("returns", "Trả hàng cần lấy về", 1, false)),
				page, "pickup"));

		mockMvc.perform(get("/shipper/orders").requestAttr("_csrf", csrfToken()))
				.andExpect(status().isOk())
				.andExpect(view().name("shipper/orders"))
				.andExpect(content().string(containsString("Cần lấy hàng")))
				.andExpect(content().string(containsString("Trả hàng cần lấy về")))
				.andExpect(content().string(containsString("ORD-000009")))
				.andExpect(content().string(containsString("250,000 ₫")))
				.andExpect(content().string(containsString("/shipper/orders/9")))
				.andExpect(content().string(containsString("data-pagination")));
		verify(shipperOrderService).getOrders(eq("pickup"), any());
	}

	@Test
	void orderDetailRendersPickupDeliveryItemsAndCod() throws Exception {
		ShipperOrderDetailResponse detail = new ShipperOrderDetailResponse(
				9L, "ORD-000009", LocalDateTime.of(2026, 10, 25, 10, 30), OrderStatus.SHIPPING,
				"Đang giao", "primary", "Moonlight Cards", "10 Đường Shop, Hà Nội", "0900000000",
				"Gọi shop khi đến", "Người nhận", "0912345678", "20 Đường Nhận, Hà Nội",
				new BigDecimal("250000"), new BigDecimal("250000"),
				List.of(new ShipperOrderItemResponse("Rare Card", null, 2,
						new BigDecimal("125000"), new BigDecimal("250000"))));
		when(shipperOrderService.getOrderDetail(9L)).thenReturn(detail);

		mockMvc.perform(get("/shipper/orders/9").requestAttr("_csrf", csrfToken()))
				.andExpect(status().isOk())
				.andExpect(view().name("shipper/order-detail"))
				.andExpect(content().string(containsString("Thông tin lấy hàng")))
				.andExpect(content().string(containsString("Thông tin người nhận")))
				.andExpect(content().string(containsString("10 Đường Shop, Hà Nội")))
				.andExpect(content().string(containsString("20 Đường Nhận, Hà Nội")))
				.andExpect(content().string(containsString("250,000 ₫")))
				.andExpect(content().string(containsString("Rare Card")))
				.andExpect(content().string(containsString("PG-24")));
		verify(shipperOrderService).getOrderDetail(9L);
	}

	private DefaultCsrfToken csrfToken() {
		return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
	}
}
