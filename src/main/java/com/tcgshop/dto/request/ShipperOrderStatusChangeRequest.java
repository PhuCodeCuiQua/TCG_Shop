package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShipperOrderStatusChangeRequest(
		@NotNull(message = "Vui lòng chọn trạng thái mới") OrderStatus newStatus,
		@Size(max = 255, message = "Lý do tối đa 255 ký tự") String note,
		boolean codCollectedConfirmed) {
}
