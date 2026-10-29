package com.tcgshop.service;

import com.tcgshop.dto.request.SeriesRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import org.springframework.data.domain.Pageable;

public interface SeriesCatalogService {

	PageResponse<SeriesResponse> search(String keyword, Long gameId, Boolean active, Pageable pageable);

	SeriesResponse getById(Long id);

	SeriesResponse create(SeriesRequest request);

	/** Sửa thông tin Series; không cho đổi sang Game khác. */
	SeriesResponse update(Long id, SeriesRequest request);

	SeriesResponse setActive(Long id, boolean active);

	/**
	 * Còn Card hoặc Product liên quan thì chỉ ẩn, không thì xóa cứng.
	 *
	 * @return true nếu đã xóa cứng, false nếu chỉ chuyển sang ẩn
	 */
	boolean removeOrHide(Long id);
}