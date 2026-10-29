package com.tcgshop.dto.response;

import java.time.LocalDateTime;

public record TimelineEntryResponse(String statusLabel, String statusColor, LocalDateTime time,
		String actorLabel, String note) { }
