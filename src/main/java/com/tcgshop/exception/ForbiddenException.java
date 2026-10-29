package com.tcgshop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {
	private static final String DEFAULT_MESSAGE = "Bạn không có quyền thực hiện thao tác này";

	public ForbiddenException() {
		super(DEFAULT_MESSAGE);
	}

	public ForbiddenException(String message) {
		super(message);
	}
}
