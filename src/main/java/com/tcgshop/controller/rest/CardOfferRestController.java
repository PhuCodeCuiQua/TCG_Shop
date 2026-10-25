package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.CardOffersResponse;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.service.CardOfferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/cards/** là công khai (SECURITY_SPEC mục 2). */
@RestController
@RequestMapping("/api/cards")
public class CardOfferRestController {
	private final CardOfferService service;

	public CardOfferRestController(CardOfferService service) {
		this.service = service;
	}

	@GetMapping("/{cardId}/offers")
	public CardOffersResponse offers(@PathVariable Long cardId,
			@RequestParam(required = false) CardCondition condition,
			@RequestParam(defaultValue = "price") String sort,
			@RequestParam(defaultValue = "asc") String dir) {
		return service.getOffers(cardId, condition, sort, dir);
	}
}