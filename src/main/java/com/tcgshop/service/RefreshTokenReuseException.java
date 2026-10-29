package com.tcgshop.service;

public class RefreshTokenReuseException extends RuntimeException {
	public RefreshTokenReuseException() { super("A revoked refresh token was reused"); }
}
