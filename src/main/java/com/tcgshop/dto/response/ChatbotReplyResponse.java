package com.tcgshop.dto.response;

public record ChatbotReplyResponse(String reply, String html, String imageUrl) {
	public ChatbotReplyResponse(String reply) {
		this(reply, null, null);
	}

	public ChatbotReplyResponse(String reply, String html) {
		this(reply, html, null);
	}
}
