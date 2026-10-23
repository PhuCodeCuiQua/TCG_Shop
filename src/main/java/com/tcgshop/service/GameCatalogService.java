package com.tcgshop.service;

import com.tcgshop.dto.request.GameRequest;
import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface GameCatalogService {

	PageResponse<GameResponse> search(String keyword, Boolean active, Pageable pageable);

	GameResponse getById(Long id);

	GameResponse create(GameRequest request);

	GameResponse update(Long id, GameRequest request);

	/** Ẩn hoặc hiện lại Game. */
	GameResponse setActive(Long id, boolean active);

	/**
	 * Còn Series liên quan thì chỉ ẩn, không còn thì xóa cứng.
	 *
	 * @return true nếu đã xóa cứng, false nếu chỉ chuyển sang ẩn
	 */
	boolean removeOrHide(Long id);
}