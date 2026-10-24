package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ReturnStatus;
import java.time.LocalDateTime;

public record ReturnRequestInfoResponse(ReturnStatus status, String statusLabel, String statusColor,
		String reason, String vendorNote, String adminNote, String escalationReason,
		String proofImageUrl, String proofVideoUrl, LocalDateTime createdAt, LocalDateTime decidedAt) {
}
