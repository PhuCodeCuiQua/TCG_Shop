package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.ChatbotAskRequest;
import com.tcgshop.dto.response.ChatbotReplyResponse;
import com.tcgshop.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotRestController {
	private final ChatbotService chatbotService;

	@PostMapping(value = "/ask", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
	public ResponseEntity<ChatbotReplyResponse> ask(@Valid @RequestBody ChatbotAskRequest request) {
		return ResponseEntity.ok(chatbotService.answer(request.question()));
	}
}
