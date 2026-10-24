package com.tcgshop.dto.request;

import jakarta.validation.constraints.NotNull;

/** Chọn shipper để phân công. Mã đơn lấy từ đường dẫn, người thao tác lấy từ CurrentUser. */
public record ShipperAssignRequest(
		@NotNull(message = "Vui lòng chọn shipper")
		Long shipperId) {
}