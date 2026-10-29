package com.tcgshop.exception;

/** Lớp gốc cho các lỗi xác thực cần ánh xạ HTTP riêng. */
public abstract class AuthException extends RuntimeException {
	protected AuthException(String message) {
		super(message);
	}
}
