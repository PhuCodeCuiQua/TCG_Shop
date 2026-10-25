package com.tcgshop.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcgshop.dto.request.ShipperOrderStatusChangeRequest;
import com.tcgshop.dto.response.ShipperOrderStatusChangeResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.service.ShipperOrderStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShipperOrderStatusRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShipperOrderStatusRestControllerMvcTest {
	@Autowired
	private MockMvc mockMvc;
	@MockBean
	private ShipperOrderStatusService shipperOrderStatusService;

	@Test
	void statusChangeReturnsJsonAndDelegatesValidatedRequest() throws Exception {
		when(shipperOrderStatusService.changeStatus(eq(17L), any(ShipperOrderStatusChangeRequest.class)))
				.thenReturn(new ShipperOrderStatusChangeResponse(true, 17L, OrderStatus.PICKED_UP,
						"Đã lấy hàng", "info", "Cập nhật trạng thái đơn hàng thành công"));

		mockMvc.perform(post("/shipper/orders/17/status")
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.content("""
						{"newStatus":"PICKED_UP","note":null,"codCollectedConfirmed":false}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đã lấy hàng")));

		verify(shipperOrderStatusService).changeStatus(eq(17L), any(ShipperOrderStatusChangeRequest.class));
	}
}
