package com.tcgshop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class VendorProductEditRequest {
	@NotNull(message = "Vui lòng nhập giá sản phẩm")
	@DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
	@Digits(integer = 13, fraction = 2, message = "Giá không hợp lệ")
	private BigDecimal price;

	@NotNull(message = "Vui lòng nhập số lượng tồn kho")
	@Min(value = 0, message = "Tồn kho không được âm")
	private Integer stock;

	@Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
	private String description;

	private MultipartFile image;
}
