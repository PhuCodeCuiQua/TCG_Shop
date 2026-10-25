package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.MediaType;

public record ReviewMediaResponse(String mediaUrl, MediaType mediaType) {
}
