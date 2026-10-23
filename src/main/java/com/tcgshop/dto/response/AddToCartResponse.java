package com.tcgshop.dto.response;

public record AddToCartResponse(boolean success, String message, long cartCount) { }
