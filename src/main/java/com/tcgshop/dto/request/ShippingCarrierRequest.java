package com.tcgshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.web.multipart.MultipartFile;

public class ShippingCarrierRequest {

	@NotBlank(message = "Vui lòng nhập tên nhà vận chuyển")
	@Size(max = 150, message = "Tên nhà vận chuyển tối đa 150 ký tự")
	private String name;

	@NotBlank(message = "Vui lòng nhập mã nhà vận chuyển")
	@Size(max = 50, message = "Mã nhà vận chuyển tối đa 50 ký tự")
	private String code;

	@NotNull(message = "Vui lòng nhập phí vận chuyển")
	@DecimalMin(value = "0.00", message = "Phí vận chuyển không được âm")
	@Digits(integer = 13, fraction = 2, message = "Phí vận chuyển tối đa 13 chữ số nguyên và 2 chữ số thập phân")
	private BigDecimal baseFee;

	@DecimalMin(value = "0.00", message = "Ngưỡng miễn phí vận chuyển không được âm")
	@Digits(integer = 13, fraction = 2,
			message = "Ngưỡng miễn phí vận chuyển tối đa 13 chữ số nguyên và 2 chữ số thập phân")
	private BigDecimal freeShipThreshold;

	private MultipartFile logo;
	private boolean active;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public BigDecimal getBaseFee() {
		return baseFee;
	}

	public void setBaseFee(BigDecimal baseFee) {
		this.baseFee = baseFee;
	}

	public BigDecimal getFreeShipThreshold() {
		return freeShipThreshold;
	}

	public void setFreeShipThreshold(BigDecimal freeShipThreshold) {
		this.freeShipThreshold = freeShipThreshold;
	}

	public MultipartFile getLogo() {
		return logo;
	}

	public void setLogo(MultipartFile logo) {
		this.logo = logo;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
