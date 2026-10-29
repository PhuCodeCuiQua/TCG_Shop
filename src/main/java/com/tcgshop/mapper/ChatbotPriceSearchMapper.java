package com.tcgshop.mapper;

import com.tcgshop.dto.response.ChatbotPriceOfferResponse;
import com.tcgshop.repository.ChatbotPriceCandidateProjection;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatbotPriceSearchMapper {
	@Mapping(target = "price", source = "effectivePrice")
	ChatbotPriceOfferResponse toResponse(ChatbotPriceCandidateProjection candidate, BigDecimal effectivePrice);
}
