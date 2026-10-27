package com.tcgshop.service;

import com.tcgshop.dto.response.ChatbotReplyResponse;

public interface ChatbotService {
	ChatbotReplyResponse answer(String question);
}
