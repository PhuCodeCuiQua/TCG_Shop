package com.tcgshop.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;
import java.util.List;

public record CatalogImportRequest(Item item, ProductType type, List<GameData> games, List<SeriesData> series) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String name, BigDecimal priceVnd, String imageUrl, String imagePublicId,
            String description, String seriesCode, String number, String rarity, String language, String type,
            CardCondition cardCondition, String edition) {
        public Item(String name, BigDecimal priceVnd, String imageUrl, String imagePublicId,
                String description, String seriesCode, String number, String rarity, String language, String type) {
            this(name, priceVnd, imageUrl, imagePublicId, description, seriesCode, number, rarity, language, type,
                    null, null);
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GameData(String code, String name, String description) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeriesData(String gameCode, String code, String name, String releaseDate) {}
}
