package com.tcgshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PriceHistoryEditRequest {

	@NotNull(message = "Vui lòng nhập market price")
	@DecimalMin(value = "0.00", inclusive = false, message = "Market price phải lớn hơn 0")
	@Digits(integer = 13, fraction = 2, message = "Market price tối đa 13 chữ số và 2 chữ số thập phân")
	private BigDecimal marketPrice;

	@NotNull(message = "Vui lòng nhập avg price")
	@DecimalMin(value = "0.00", inclusive = false, message = "Avg price phải lớn hơn 0")
	@Digits(integer = 13, fraction = 2, message = "Avg price tối đa 13 chữ số và 2 chữ số thập phân")
	private BigDecimal avgPrice;
}
