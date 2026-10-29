package com.tcgshop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Các phép toán và định dạng tiền VND dùng chung. */
public final class MoneyUtils {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final Locale VIETNAMESE_LOCALE = Locale.forLanguageTag("vi-VN");

	private MoneyUtils() {
	}

	/** Làm tròn tiền đến đồng gần nhất; nửa đồng được làm tròn xa số 0. */
	public static BigDecimal round(BigDecimal amount) {
		return amount == null ? ZERO : amount.setScale(0, RoundingMode.HALF_UP);
	}

	/** Định dạng số tiền theo nhóm hàng nghìn kiểu Việt Nam và thêm ký hiệu đồng. */
	public static String format(BigDecimal amount) {
		NumberFormat formatter = NumberFormat.getNumberInstance(VIETNAMESE_LOCALE);
		formatter.setGroupingUsed(true);
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(0);
		return formatter.format(round(amount)) + " ₫";
	}

	/** Cộng hai số tiền, xem giá trị null như 0 để tránh lỗi ở các giá trị tùy chọn. */
	public static BigDecimal add(BigDecimal left, BigDecimal right) {
		return valueOrZero(left).add(valueOrZero(right));
	}

	/** Trừ hai số tiền, xem giá trị null như 0. */
	public static BigDecimal subtract(BigDecimal left, BigDecimal right) {
		return valueOrZero(left).subtract(valueOrZero(right));
	}

	/** Chặn số tiền âm; null cũng được chuẩn hóa thành 0. */
	public static BigDecimal nonNegative(BigDecimal amount) {
		BigDecimal value = valueOrZero(amount);
		return value.signum() < 0 ? ZERO : value;
	}

	private static BigDecimal valueOrZero(BigDecimal value) {
		return value == null ? ZERO : value;
	}
}
