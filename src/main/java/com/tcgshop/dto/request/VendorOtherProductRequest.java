package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class VendorOtherProductRequest {

	@NotNull(message = "Vui lòng chọn loại sản phẩm")
	private ProductType type;

	private Long seriesId;

	private CardLanguage language;

	private String accessoryCategory;

	@NotBlank(message = "Vui lòng nhập tên sản phẩm")
	@Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
	private String name;

	@NotBlank(message = "Vui lòng nhập mô tả sản phẩm")
	@Size(max = 5000, message = "Mô tả không được vượt quá 5.000 ký tự")
	private String description;

	@NotNull(message = "Vui lòng nhập giá bán")
	@DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0")
	@Digits(integer = 13, fraction = 2, message = "Giá bán tối đa 13 chữ số nguyên và 2 chữ số thập phân")
	private BigDecimal price;

	@NotNull(message = "Vui lòng nhập số lượng")
	@Min(value = 1, message = "Số lượng phải ít nhất là 1")
	private Integer stock;

	private MultipartFile image;
}
