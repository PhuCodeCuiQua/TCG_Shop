package com.tcgshop.exception;

/** Lỗi nghiệp vụ, ví dụ chuyển trạng thái đơn không hợp lệ hoặc sản phẩm hết hàng. */
public class BusinessException extends RuntimeException {
	public BusinessException(String message) {
		super(message);
	}
}
