package com.tcgshop.dto.response;

import java.util.List;

public record PublicShopFilterOptionsResponse(List<GameResponse> games, List<SeriesResponse> series) {
}
