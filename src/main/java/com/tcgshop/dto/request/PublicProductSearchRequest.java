package com.tcgshop.dto.request;

import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PublicProductSearchRequest {

	@Size(max = 120, message = "Từ khóa tối đa 120 ký tự")
	private String keyword;

	@Positive(message = "Game không hợp lệ")
	private Long gameId;

	@Positive(message = "Series không hợp lệ")
	private Long seriesId;

	private Rarity rarity;
	private ProductType type;
	private CardLanguage language;
	private CardCondition condition;

	@DecimalMin(value = "0.00", message = "Giá từ không được âm")
	@Digits(integer = 13, fraction = 2, message = "Giá từ tối đa 13 chữ số nguyên và 2 chữ số thập phân")
	private BigDecimal priceFrom;

	@DecimalMin(value = "0.00", message = "Giá đến không được âm")
	@Digits(integer = 13, fraction = 2, message = "Giá đến tối đa 13 chữ số nguyên và 2 chữ số thập phân")
	private BigDecimal priceTo;

	@NotNull(message = "Vui lòng chọn cách sắp xếp")
	private SortBy sort = SortBy.PRICE_ASC;

	private TopTab tab;

	@Min(value = 0, message = "Trang không hợp lệ")
	@Max(value = 100000, message = "Trang vượt giới hạn")
	private int page;

	@AssertTrue(message = "Giá từ phải nhỏ hơn hoặc bằng giá đến")
	public boolean isPriceRangeValid() {
		return priceFrom == null || priceTo == null || priceFrom.compareTo(priceTo) <= 0;
	}

	public boolean isTopTwenty() {
		return tab != null;
	}

	public enum SortBy {
		PRICE_ASC("Giá thấp đến cao"),
		PRICE_DESC("Giá cao đến thấp"),
		NEWEST("Mới nhất"),
		BEST_SELLING("Bán chạy"),
		TOP_RATED("Đánh giá cao"),
		FAVORITES("Yêu thích");

		private final String displayName;

		SortBy(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	public enum TopTab {
		NEWEST("Mới nhất", SortBy.NEWEST),
		BEST_SELLING("Bán chạy", SortBy.BEST_SELLING),
		TOP_RATED("Đánh giá cao", SortBy.TOP_RATED),
		FAVORITES("Yêu thích", SortBy.FAVORITES);

		private final String displayName;
		private final SortBy sortBy;

		TopTab(String displayName, SortBy sortBy) {
			this.displayName = displayName;
			this.sortBy = sortBy;
		}

		public String getDisplayName() {
			return displayName;
		}

		public SortBy getSortBy() {
			return sortBy;
		}
	}
}
