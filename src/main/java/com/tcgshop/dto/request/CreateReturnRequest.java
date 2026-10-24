package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.ReturnReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateReturnRequest {
	@NotNull(message = "Vui lòng chọn lý do trả hàng")
	private ReturnReason reason;

	@NotBlank
	@Size(min = 10, max = 500, message = "Mô tả chi tiết từ 10 đến 500 ký tự")
	private String detail;
}
