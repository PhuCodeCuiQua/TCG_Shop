package com.tcgshop.service;

import com.tcgshop.dto.response.CheckMyCardResponse;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;

public interface CheckMyCardService {
	CheckMyCardResponse check(Long cardId, CardCondition condition, CardLanguage language);
}
