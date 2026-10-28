package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.PromotionType;
import com.tcgshop.entity.enums.ActiveStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class PlatformPromotionRequest {

	@NotBlank(message = "Vui lòng nhập tên chương trình")
	@Size(max = 255, message = "Tên chương trình tối đa 255 ký tự")
	private String title;

	@NotNull(message = "Vui lòng chọn loại khuyến mãi")
	private PromotionType type;

	@DecimalMin(value = "0.00", inclusive = false, message = "Phần trăm giảm phải lớn hơn 0")
	@DecimalMax(value = "100.00", message = "Phần trăm giảm không được vượt quá 100%")
	@Digits(integer = 3, fraction = 2, message = "Phần trăm tối đa 3 chữ số nguyên và 2 chữ số thập phân")
	private BigDecimal discountPercent;

	@DecimalMin(value = "0.00", inclusive = false, message = "Số tiền giảm phải lớn hơn 0")
	@Digits(integer = 13, fraction = 2, message = "Số tiền tối đa 13 chữ số và 2 chữ số thập phân")
	private BigDecimal discountAmount;

	@DecimalMin(value = "0.00", message = "Đơn tối thiểu không được âm")
	@Digits(integer = 13, fraction = 2, message = "Đơn tối thiểu tối đa 13 chữ số và 2 chữ số thập phân")
	private BigDecimal minOrderValue;

	private Long gameId;

	private Long seriesId;

	private List<Long> productIds = new ArrayList<>();

	private boolean applyToAll;

	@NotNull(message = "Vui lòng chọn thời gian bắt đầu")
	private LocalDateTime startDate;

	@NotNull(message = "Vui lòng chọn thời gian kết thúc")
	private LocalDateTime endDate;

	@NotNull(message = "Vui lòng chọn trạng thái")
	private ActiveStatus status = ActiveStatus.ACTIVE;
}
