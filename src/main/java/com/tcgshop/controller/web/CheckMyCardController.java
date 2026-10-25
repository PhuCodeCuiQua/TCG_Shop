package com.tcgshop.controller.web;

import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.service.CheckMyCardService;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Validated
public class CheckMyCardController {
	private final CheckMyCardService checkMyCardService;

	public CheckMyCardController(CheckMyCardService checkMyCardService) {
		this.checkMyCardService = checkMyCardService;
	}

	@GetMapping("/check-my-card")
	@PreAuthorize("hasRole('USER')")
	public String checkMyCard(
			@RequestParam(required = false) @Positive Long cardId,
			@RequestParam(required = false) CardCondition condition,
			@RequestParam(required = false) CardLanguage language,
			Model model) {
		model.addAttribute("conditions", CardCondition.values());
		model.addAttribute("languages", CardLanguage.values());
		model.addAttribute("cardId", cardId);
		model.addAttribute("selectedCondition", condition);
		model.addAttribute("selectedLanguage", language);
		if (cardId != null) {
			model.addAttribute("result", checkMyCardService.check(cardId, condition, language));
		}
		return "user/check-my-card";
	}
}
