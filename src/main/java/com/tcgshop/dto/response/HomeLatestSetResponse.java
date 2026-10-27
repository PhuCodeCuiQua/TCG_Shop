package com.tcgshop.dto.response;

import java.time.LocalDate;

public record HomeLatestSetResponse(Long id, String name, String gameName, LocalDate releaseDate, String logoUrl,
        String coverImageUrl) {
    public HomeLatestSetResponse(Long id, String name, String gameName, LocalDate releaseDate, String logoUrl) {
        this(id, name, gameName, releaseDate, logoUrl, null);
    }
}
