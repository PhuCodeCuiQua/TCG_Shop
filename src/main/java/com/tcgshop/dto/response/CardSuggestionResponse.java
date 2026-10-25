package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.CardLanguage;

public record CardSuggestionResponse(
		Long id,
		String name,
		String cardNumber,
		String seriesName,
		String gameName,
		CardLanguage language) {
}
