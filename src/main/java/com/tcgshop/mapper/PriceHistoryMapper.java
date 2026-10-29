package com.tcgshop.mapper;

import com.tcgshop.dto.response.CardPriceOptionResponse;
import com.tcgshop.dto.response.PriceHistoryResponse;
import com.tcgshop.entity.Card;
import com.tcgshop.entity.PriceHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PriceHistoryMapper {

	@Mapping(target = "cardId", source = "card.id")
	@Mapping(target = "cardName", source = "card.name")
	@Mapping(target = "cardNumber", source = "card.cardNumber")
	@Mapping(target = "seriesName", source = "card.series.name")
	PriceHistoryResponse toResponse(PriceHistory history);

	CardPriceOptionResponse toOption(Card card);
}
