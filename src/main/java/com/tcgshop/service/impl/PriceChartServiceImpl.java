package com.tcgshop.service.impl;

import com.tcgshop.dto.response.PriceChartPointResponse;
import com.tcgshop.dto.response.PriceChartResponse;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.PriceChartRepository;
import com.tcgshop.service.PriceChartService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PriceChartServiceImpl implements PriceChartService {
	private static final Set<Integer> RANGES = Set.of(7, 30, 90);
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	private final PriceChartRepository repository;
	private final CardRepository cardRepository;
	private final Clock clock;

	public PriceChartServiceImpl(PriceChartRepository repository, CardRepository cardRepository, Clock clock) {
		this.repository = repository;
		this.cardRepository = cardRepository;
		this.clock = clock;
	}

	@Override
	public PriceChartResponse getChart(Long cardId, int range) {
		if (!RANGES.contains(range)) {
			throw new BadRequestException("Khoảng thời gian chỉ được chọn 7, 30 hoặc 90 ngày");
		}
		if (!cardRepository.existsById(cardId)) {
			throw new ResourceNotFoundException("Thẻ", cardId);
		}

		LocalDate to = LocalDate.now(clock);
		LocalDate from = to.minusDays(range - 1L);
		List<PriceHistory> rows = repository.findRange(cardId, from, to);
		if (rows.isEmpty()) {
			return new PriceChartResponse(cardId, range, from, to, List.of(), null, null, null, true);
		}

		List<PriceChartPointResponse> points = rows.stream()
				.map(r -> new PriceChartPointResponse(r.getRecordedDate(), r.getMarketPrice(), r.getAvgPrice()))
				.toList();
		BigDecimal current = rows.get(rows.size() - 1).getMarketPrice();

		// Mốc so sánh: giá cuối của kỳ liền trước; không có thì dùng điểm đầu của kỳ hiện tại.
		LocalDate previousFrom = from.minusDays(range);
		List<PriceHistory> previousRows = repository.findRange(cardId, previousFrom, from.minusDays(1));
		boolean comparedToPrevious = !previousRows.isEmpty();
		BigDecimal baseline = null;
		if (comparedToPrevious) {
			baseline = previousRows.get(previousRows.size() - 1).getMarketPrice();
		} else if (rows.size() > 1) {
			baseline = rows.get(0).getMarketPrice();
		}

		return new PriceChartResponse(cardId, range, from, to, points, current, baseline,
				percentChange(current, baseline), comparedToPrevious);
	}

	private BigDecimal percentChange(BigDecimal current, BigDecimal baseline) {
		if (current == null || baseline == null || baseline.signum() == 0) {
			return null;
		}
		return current.subtract(baseline).multiply(ONE_HUNDRED).divide(baseline, 2, RoundingMode.HALF_UP);
	}
}