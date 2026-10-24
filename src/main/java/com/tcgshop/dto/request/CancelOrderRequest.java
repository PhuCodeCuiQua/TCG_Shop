package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.CancelReason;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelOrderRequest {
	@NotNull(message = "Vui lòng chọn lý do hủy đơn")
	private CancelReason reason;

	@Size(max = 200, message = "Lý do cụ thể tối đa 200 ký tự")
	private String detail;

	@AssertTrue(message = "Vui lòng nhập lý do cụ thể (từ 10 đến 200 ký tự)")
	public boolean isDetailValid() {
		if (reason == null || reason != CancelReason.OTHER) {
			return true;
		}
		if (detail == null) {
			return false;
		}
		String normalized = detail.trim().replaceAll("\\s+", " ");
		return normalized.length() >= 10 && normalized.length() <= 200;
	}
}
