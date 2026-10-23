package com.tcgshop.service.impl;

import com.tcgshop.dto.request.GameRequest;
import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.entity.Game;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.GameMapper;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.service.GameCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameCatalogServiceImpl implements GameCatalogService {

	private final GameRepository gameRepository;
	private final SeriesRepository seriesRepository;
	private final GameMapper gameMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<GameResponse> search(String keyword, Boolean active, Pageable pageable) {
		return PageResponse.from(
				gameRepository.search(normalizeKeyword(keyword), active, pageable).map(gameMapper::toResponse));
	}

	@Override
	@Transactional(readOnly = true)
	public GameResponse getById(Long id) {
		return gameMapper.toResponse(findOrThrow(id));
	}

	@Override
	@Transactional
	public GameResponse create(GameRequest request) {
		String name = request.name().trim();
		// Tên và mã phải duy nhất toàn hệ thống (ràng buộc unique ở DB, kiểm tra trước để báo lỗi rõ ràng)
		if (gameRepository.existsByNameIgnoreCase(name)) {
			throw new ConflictException("Tên game đã tồn tại");
		}
		if (gameRepository.existsByCodeIgnoreCase(request.code())) {
			throw new ConflictException("Mã game đã tồn tại");
		}
		Game game = gameMapper.toEntity(request);
		game.setName(name);
		game.setActive(true);
		return gameMapper.toResponse(gameRepository.save(game));
	}

	@Override
	@Transactional
	public GameResponse update(Long id, GameRequest request) {
		Game game = findOrThrow(id);
		String name = request.name().trim();
		// Loại trừ chính bản ghi đang sửa khi kiểm tra trùng
		if (gameRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("Tên game đã tồn tại");
		}
		if (gameRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
			throw new ConflictException("Mã game đã tồn tại");
		}
		gameMapper.updateEntity(request, game);
		game.setName(name);
		return gameMapper.toResponse(game); // dirty checking tự UPDATE khi commit
	}

	@Override
	@Transactional
	public GameResponse setActive(Long id, boolean active) {
		Game game = findOrThrow(id);
		game.setActive(active);
		return gameMapper.toResponse(game);
	}

	@Override
	@Transactional
	public boolean removeOrHide(Long id) {
		Game game = findOrThrow(id);
		// Còn Series thì không xóa cứng (vi phạm khóa ngoại và mất dữ liệu liên quan): chỉ ẩn
		if (seriesRepository.existsByGameId(id)) {
			game.setActive(false);
			return false;
		}
		gameRepository.delete(game);
		return true;
	}

	private Game findOrThrow(Long id) {
		return gameRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy game"));
	}

	private String normalizeKeyword(String keyword) {
		return keyword == null || keyword.isBlank() ? null : keyword.trim();
	}
}