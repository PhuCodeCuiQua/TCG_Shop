package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.ActiveStatus;
import com.tcgshop.entity.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorCouponRequest {
	@NotBlank(message = "Vui lòng nhập mã giảm giá")
	@Size(max = 50, message = "Mã giảm giá tối đa 50 ký tự")
	@Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã chỉ gồm chữ, số, dấu gạch ngang hoặc gạch dưới")
	private String code;

	@NotNull(message = "Vui lòng chọn loại giảm giá")
	private DiscountType discountType;

	@NotNull(message = "Vui lòng nhập giá trị giảm")
	@DecimalMin(value = "0.00", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
	@Digits(integer = 13, fraction = 2, message = "Giá trị tối đa 13 chữ số và 2 chữ số thập phân")
	private BigDecimal discountValue;

	@DecimalMin(value = "0.00", message = "Đơn tối thiểu không được âm")
	@Digits(integer = 13, fraction = 2, message = "Đơn tối thiểu tối đa 13 chữ số và 2 chữ số thập phân")
	private BigDecimal minOrderValue = BigDecimal.ZERO;

	@DecimalMin(value = "0.00", inclusive = false, message = "Mức giảm tối đa phải lớn hơn 0")
	@Digits(integer = 13, fraction = 2, message = "Mức giảm tối đa tối đa 13 chữ số và 2 chữ số thập phân")
	private BigDecimal maxDiscountAmount;

	@NotNull(message = "Vui lòng nhập tổng lượt sử dụng")
	@Min(value = 1, message = "Tổng lượt sử dụng phải lớn hơn 0")
	private Integer totalQuantity;

	@NotNull(message = "Vui lòng nhập số lần dùng tối đa mỗi người")
	@Min(value = 1, message = "Số lần mỗi người phải lớn hơn 0")
	private Integer limitPerUser = 1;

	@NotNull(message = "Vui lòng chọn thời gian bắt đầu")
	private LocalDateTime startDate;

	@NotNull(message = "Vui lòng chọn thời gian kết thúc")
	private LocalDateTime endDate;

	@NotNull(message = "Vui lòng chọn trạng thái")
	private ActiveStatus status = ActiveStatus.ACTIVE;
}
