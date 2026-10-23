package com.tcgshop.service;

import com.tcgshop.dto.request.CardRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.CardResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.entity.enums.Rarity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CardService {

	/**
	 * Tìm thẻ gốc để Vendor chọn khi đăng bán. Chỉ trả thẻ và series đang hiện.
	 *
	 * @param keyword  tìm theo tên hoặc số thẻ, có thể null
	 * @param seriesId lọc theo series, có thể null
	 */
	PageResponse<CardListingResponse> searchForListing(String keyword, Long seriesId, Pageable pageable);

	/** Danh sách cho trang quản lý (Manager/Admin), mọi tham số lọc đều có thể null. */
	PageResponse<CardResponse> search(String keyword, Long gameId, Long seriesId, Rarity rarity,
			Boolean active, Pageable pageable);

	CardResponse getById(Long id);

	/** Tạo thẻ mới; image có thể null hoặc rỗng. */
	CardResponse create(CardRequest request, MultipartFile image);

	/** Sửa thẻ; nếu có image mới thì thay ảnh và xóa ảnh cũ trên Cloudinary sau khi lưu thành công. */
	CardResponse update(Long id, CardRequest request, MultipartFile image);

	/** Ẩn hoặc hiện lại thẻ (không xóa cứng vì còn sản phẩm, lịch sử giá tham chiếu). */
	CardResponse setActive(Long id, boolean active);
}