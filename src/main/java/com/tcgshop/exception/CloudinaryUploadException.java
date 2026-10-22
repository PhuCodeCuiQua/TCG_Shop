package com.tcgshop.exception;

public class CloudinaryUploadException extends RuntimeException {
	private static final String SAFE_MESSAGE = "Không thể tải tệp lên dịch vụ lưu trữ. Vui lòng thử lại sau.";

	public CloudinaryUploadException(Throwable cause) {
		super(SAFE_MESSAGE, cause);
	}
}
