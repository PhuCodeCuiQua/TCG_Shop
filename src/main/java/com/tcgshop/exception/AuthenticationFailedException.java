package com.tcgshop.exception;

public class AuthenticationFailedException extends AuthException {
	public AuthenticationFailedException() {
		super("Email hoặc mật khẩu không đúng");
	}
}
