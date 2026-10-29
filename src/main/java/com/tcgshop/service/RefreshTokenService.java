package com.tcgshop.service;

import com.tcgshop.entity.User;

public interface RefreshTokenService {
	String issue(User user);

	Rotation rotate(String rawToken);

	void revoke(String rawToken);

	void revokeAllByUser(Long userId);

	/** Dữ liệu an toàn để cấp access JWT sau khi token refresh đã được xoay vòng. */
	record Rotation(String rawToken, Long userId, String email) { }
}
