package com.tcgshop.service.impl;

import com.tcgshop.dto.request.SeriesRequest;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.Series;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.SeriesMapper;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.service.SeriesCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeriesCatalogServiceImpl implements SeriesCatalogService {

	private final SeriesRepository seriesRepository;
	private final GameRepository gameRepository;
	private final CardRepository cardRepository;
	private final ProductRepository productRepository;
	private final SeriesMapper seriesMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<SeriesResponse> search(String keyword, Long gameId, Boolean active, Pageable pageable) {
		return PageResponse.from(seriesRepository
				.search(normalizeKeyword(keyword), gameId, active, pageable)
				.map(seriesMapper::toResponse));
	}

	@Override
	@Transactional(readOnly = true)
	public SeriesResponse getById(Long id) {
		return seriesMapper.toResponse(findOrThrow(id));
	}

	@Override
	@Transactional
	public SeriesResponse create(SeriesRequest request) {
		Game game = gameRepository.findById(request.gameId())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy game"));
		if (!game.isActive()) {
			throw new ConflictException("Game đang bị ẩn, không thể thêm series");
		}
		String name = request.name().trim();
		// Trùng tên hoặc mã chỉ tính trong cùng một Game
		if (seriesRepository.existsByGameIdAndNameIgnoreCase(game.getId(), name)) {
			throw new ConflictException("Tên series đã tồn tại trong game này");
		}
		if (seriesRepository.existsByGameIdAndCodeIgnoreCase(game.getId(), request.code())) {
			throw new ConflictException("Mã series đã tồn tại trong game này");
		}
		Series series = seriesMapper.toEntity(request);
		series.setGame(game);
		series.setName(name);
		series.setActive(true);
		return seriesMapper.toResponse(seriesRepository.save(series));
	}

	@Override
	@Transactional
	public SeriesResponse update(Long id, SeriesRequest request) {
		Series series = findOrThrow(id);
		// Không cho đổi Game: thẻ và sản phẩm đã gắn với Series này sẽ bị lệch dữ liệu
		Long gameId = series.getGame().getId();
		String name = request.name().trim();
		if (seriesRepository.existsByGameIdAndNameIgnoreCaseAndIdNot(gameId, name, id)) {
			throw new ConflictException("Tên series đã tồn tại trong game này");
		}
		if (seriesRepository.existsByGameIdAndCodeIgnoreCaseAndIdNot(gameId, request.code(), id)) {
			throw new ConflictException("Mã series đã tồn tại trong game này");
		}
		seriesMapper.updateEntity(request, series);
		series.setName(name);
		return seriesMapper.toResponse(series);
	}

	@Override
	@Transactional
	public SeriesResponse setActive(Long id, boolean active) {
		Series series = findOrThrow(id);
		series.setActive(active);
		return seriesMapper.toResponse(series);
	}

	@Override
	@Transactional
	public boolean removeOrHide(Long id) {
		Series series = findOrThrow(id);
		// Còn thẻ hoặc sản phẩm (hàng niêm phong) liên quan thì chỉ ẩn
		if (cardRepository.existsBySeriesId(id) || productRepository.existsBySeriesId(id)) {
			series.setActive(false);
			return false;
		}
		seriesRepository.delete(series);
		return true;
	}

	private Series findOrThrow(Long id) {
		return seriesRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy series"));
	}

	private String normalizeKeyword(String keyword) {
		return keyword == null || keyword.isBlank() ? null : keyword.trim();
	}
}