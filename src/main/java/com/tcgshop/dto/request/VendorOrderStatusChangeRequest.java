package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorOrderStatusChangeRequest {
	@NotNull(message = "Vui lòng chọn thao tác hợp lệ")
	private OrderStatus newStatus;

	@Size(max = 255, message = "Lý do tối đa 255 ký tự")
	private String note;
}
