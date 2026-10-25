package com.tcgshop.repository;

import com.tcgshop.entity.enums.MediaType;

public interface ReviewMediaListingRow {
	Long getReviewId();
	String getMediaUrl();
	MediaType getMediaType();
}
