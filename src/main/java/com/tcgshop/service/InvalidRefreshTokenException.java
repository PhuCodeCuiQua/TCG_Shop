package com.tcgshop.service;

public class InvalidRefreshTokenException extends RuntimeException {
	public InvalidRefreshTokenException(String message) { super(message); }
}
