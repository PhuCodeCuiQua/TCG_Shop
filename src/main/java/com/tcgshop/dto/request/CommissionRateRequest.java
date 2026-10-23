package com.tcgshop.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CommissionRateRequest {

	@NotNull(message = "Vui lòng nhập tỷ lệ hoa hồng")
	@DecimalMin(value = "0.00", message = "Tỷ lệ hoa hồng không được nhỏ hơn 0%")
	@DecimalMax(value = "100.00", message = "Tỷ lệ hoa hồng không được vượt quá 100%")
	@Digits(integer = 3, fraction = 2, message = "Tỷ lệ hoa hồng tối đa 3 chữ số nguyên và 2 chữ số thập phân")
	private BigDecimal rate;

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}
}
