package com.tcgshop.service;

import com.tcgshop.dto.response.CouponSuggestionResponse;
import java.util.List;

public interface CouponSuggestionService {
    List<CouponSuggestionResponse> getSuggestions();
}
