package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.Rarity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class VendorCustomCardRequest {

	@NotBlank(message = "Tên thẻ không được để trống")
	@Size(max = 200, message = "Tên thẻ tối đa 200 ký tự")
	private String name;

	@NotBlank(message = "Số thẻ không được để trống")
	@Size(max = 30, message = "Số thẻ tối đa 30 ký tự")
	private String cardNumber;

	@NotNull(message = "Vui lòng chọn series")
	private Long seriesId;

	@NotNull(message = "Vui lòng chọn độ hiếm")
	private Rarity rarity;

	@NotNull(message = "Vui lòng chọn ngôn ngữ")
	private CardLanguage language;

	private MultipartFile image;
}
