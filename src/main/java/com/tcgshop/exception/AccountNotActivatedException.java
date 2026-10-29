package com.tcgshop.exception;

public class AccountNotActivatedException extends AuthException {
	private final String email;

	public AccountNotActivatedException(String email) {
		super("Tài khoản chưa được kích hoạt");
		this.email = email;
	}

	public String getEmail() {
		return email;
	}
}
