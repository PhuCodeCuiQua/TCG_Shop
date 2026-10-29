package com.tcgshop.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorPromotionRequest {
	@NotBlank(message = "Vui lòng nhập tên chương trình")
	@Size(max = 255, message = "Tên chương trình tối đa 255 ký tự")
	private String title;

	@NotNull(message = "Vui lòng nhập phần trăm giảm")
	@DecimalMin(value = "1.00", message = "Mức giảm tối thiểu là 1%")
	@DecimalMax(value = "90.00", message = "Mức giảm tối đa là 90%")
	@Digits(integer = 3, fraction = 2, message = "Phần trăm giảm không hợp lệ")
	private BigDecimal discountPercent;

	@NotNull(message = "Vui lòng chọn thời gian bắt đầu")
	private LocalDateTime startDate;

	@NotNull(message = "Vui lòng chọn thời gian kết thúc")
	private LocalDateTime endDate;

	@NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm")
	private List<@NotNull(message = "Mã sản phẩm không hợp lệ") Long> productIds = new ArrayList<>();
}
