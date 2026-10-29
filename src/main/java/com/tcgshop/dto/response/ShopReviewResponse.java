package com.tcgshop.dto.response;

import com.tcgshop.entity.enums.ShopStatus;
import java.time.LocalDateTime;

/** Hồ sơ shop cho Manager/Admin xem. Không chứa mật khẩu hay dữ liệu nhạy cảm của chủ shop. */
public record ShopReviewResponse(
		Long id,
		String name,
		String slug,
		String logoUrl,
		String bannerUrl,
		String phoneNumber,
		String pickupAddress,
		String description,
		ShopStatus status,
		String rejectReason,
		Long ownerId,
		String ownerEmail,
		String ownerFullName,
		LocalDateTime createdAt) {
}