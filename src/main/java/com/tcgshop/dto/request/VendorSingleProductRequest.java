package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
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
public class VendorSingleProductRequest {

	@NotNull(message = "Không tìm thấy thẻ đã chọn")
	private Long cardId;

	@NotNull(message = "Vui lòng chọn tình trạng thẻ")
	private CardCondition condition;

	@NotNull(message = "Vui lòng chọn ngôn ngữ")
	private CardLanguage language;

	@Size(max = 255, message = "Phiên bản tối đa 255 ký tự")
	private String edition;

	@NotNull(message = "Vui lòng nhập giá bán")
	@DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0")
	@Digits(integer = 13, fraction = 2, message = "Giá bán tối đa 13 chữ số nguyên và 2 chữ số thập phân")
	private BigDecimal price;

	@NotNull(message = "Vui lòng nhập số lượng")
	@Min(value = 1, message = "Số lượng phải ít nhất là 1")
	private Integer stock;

	private MultipartFile image;
}
