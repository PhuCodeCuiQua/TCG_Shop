package com.tcgshop.service;

import com.tcgshop.dto.response.CardOffersResponse;
import com.tcgshop.entity.enums.CardCondition;

public interface CardOfferService {
	/**
	 * @param condition null = không lọc
	 * @param sort      price | condition | rating
	 * @param dir       asc | desc
	 */
	CardOffersResponse getOffers(Long cardId, CardCondition condition, String sort, String dir);
}