package com.tcgshop.service.impl;

import com.tcgshop.dto.response.CardOfferResponse;
import com.tcgshop.dto.response.CardOffersResponse;
import com.tcgshop.dto.response.ChatbotReplyResponse;
import com.tcgshop.dto.response.ChatbotPriceOfferResponse;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.ChatbotCardCandidateProjection;
import com.tcgshop.repository.ProductImageRepository;
import com.tcgshop.service.CardOfferService;
import com.tcgshop.service.ChatbotPriceSearchService;
import com.tcgshop.service.ChatbotService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Service
public class ChatbotServiceImpl implements ChatbotService {
	private static final String DEFAULT_REPLY = "Xin lỗi, tôi chưa hiểu ý bạn. Bạn có thể hỏi tôi về tìm kiếm thẻ bài, hoặc các câu hỏi thường gặp tại đây (link /faq).";
	private static final String SUPPORT_REPLY = "Bạn có thể xem Câu hỏi thường gặp (/faq) hoặc trang Liên hệ (/contact) để được hỗ trợ.";
	private static final String SUPPORT_HTML = "<p>Bạn có thể xem <a href=\"/faq\">Câu hỏi thường gặp</a> hoặc <a href=\"/contact\">trang Liên hệ</a> để được hỗ trợ.</p>";
	private static final String DEFAULT_HTML = "<p>Xin lỗi, tôi chưa hiểu ý bạn. Bạn có thể hỏi tôi về tìm kiếm thẻ bài, "
			+ "hoặc các câu hỏi thường gặp tại đây (<a href=\"/faq\">link /faq</a>).</p>";
	private static final List<String> SUPPORT_KEYWORDS = List.of(
			"trung tâm hỗ trợ", "câu hỏi thường gặp", "faq", "liên hệ", "hỗ trợ");
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	private static final Pattern NON_WORDS = Pattern.compile("[^\\p{L}\\p{N}]+");
	private static final Pattern PRICE_AMOUNT = Pattern.compile(
			"(?<![\\p{L}\\d])(?<amount>\\d{1,3}(?:[.,\\s]\\d{3})+|\\d+(?:[.,]\\d+)?)(?:\\s*)(?<unit>ngan|nghin|k|trieu|tr)?(?:\\s*(?<currency>vnd|dong|d))?(?![\\p{L}])",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PRICE_PREFIX = Pattern.compile(
			"(?:^|\\s)(gia|tam|khoang|duoi|tren|toi da|toi thieu|khong qua|re hon)\\s*$");
	private static final BigDecimal AROUND_TOLERANCE = new BigDecimal("0.25");
	private static final int CHATBOT_PRICE_RESULT_LIMIT = 5;
	private static final Set<String> CARD_FILLER_WORDS = Set.of(
			"mua", "ban", "gia", "check", "kiem", "tra", "tim", "shop", "the", "bai", "card", "price", "cost",
			"find", "search", "look", "for",
			"bao", "nhieu", "la", "hien", "tai", "thi", "truong", "nay", "nao", "co", "cho", "cua", "minh",
			"toi", "giup", "voi", "nhe", "vay", "hoi", "hang", "san", "pham", "dang", "o", "tot", "nhat");
	private static final Set<String> NATURAL_QUESTION_WORDS = Set.of(
			"cau", "hoi", "gi", "sao", "nao", "tai", "lam", "the", "ban", "toi", "minh", "giup", "ve",
			"cho", "co", "khong", "duoc", "la", "o", "dau", "nhung", "cac", "xin", "tu", "van");
	private static final List<Intent> INTENTS = List.of(
			new Intent(List.of("chào", "hello", "hi"), Set.of(), "Xin chào! Tôi là trợ lý ảo của TCG Shop. Tôi có thể giúp gì cho bạn?"),
			new Intent(List.of("cảm ơn", "thanks"), Set.of(), "Rất vui được giúp bạn! Chúc bạn một ngày tốt lành."),
			new Intent(List.of("giỏ hàng ở đâu", "xem giỏ", "giỏ hàng"), Set.of(), "Bạn nhìn lên thanh menu trên cùng, bấm vào biểu tượng Giỏ hàng (nằm cạnh chuông thông báo) để xem các sản phẩm đã chọn nhé."),
			new Intent(List.of("đổi ảnh", "avatar"), Set.of(), "Bạn bấm vào Tên tài khoản ở góc trên bên phải, chọn Hồ sơ cá nhân, sau đó bấm Đổi ảnh để tải lên ảnh mới."),
			new Intent(List.of("đơn hàng của tôi", "xem đơn"), Set.of(), "Bạn bấm vào Tên tài khoản góc trên phải, chọn Đơn hàng để xem lịch sử và trạng thái đơn hàng."),
			new Intent(List.of("yêu thích", "wishlist"), Set.of(), "Bạn bấm vào Tên tài khoản góc trên phải, chọn Yêu thích để xem các thẻ bài đã lưu."),
			new Intent(List.of("đăng xuất"), Set.of(), "Bạn bấm vào Tên tài khoản góc trên phải và chọn Đăng xuất."),
			new Intent(List.of("tìm kiếm", "tìm thẻ"), Set.of(), "Bạn gõ tên thẻ hoặc số thẻ vào ô Tìm kiếm ở giữa thanh menu trên cùng rồi bấm nút Tìm."),
			new Intent(List.of("đăng bán", "bán thẻ", "tạo sản phẩm"), Set.of("ROLE_VENDOR"), "Vào Kênh người bán (góc trên phải) -> chọn Sản phẩm -> Bấm nút Đăng bán thẻ đơn hoặc Đăng bán hàng niêm phong."),
			new Intent(List.of("tạo mã giảm giá", "mã giảm giá của shop", "tạo coupon"), Set.of("ROLE_VENDOR"), "Vào Kênh người bán -> Mã giảm giá của Shop -> Bấm Tạo mã giảm giá mới."),
			new Intent(List.of("xác nhận đơn", "đơn mới"), Set.of("ROLE_VENDOR"), "Vào Kênh người bán -> Đơn hàng -> Chọn tab 'Mới' -> Bấm 'Xác nhận' hoặc 'Từ chối'."),
			new Intent(List.of("doanh thu", "rút tiền", "kiếm được"), Set.of("ROLE_VENDOR"), "Vào Kênh người bán -> chọn Doanh thu để xem tổng tiền, hoa hồng và số tiền thực nhận."),
			new Intent(List.of("giao thất bại", "không giao được"), Set.of("ROLE_SHIPPER"), "Nếu giao thất bại, hãy bấm Giao thất bại và nhập lý do cụ thể."),
			new Intent(List.of("giao hàng", "lấy hàng", "cập nhật giao"), Set.of("ROLE_SHIPPER"), "Vào mục Đơn cần giao ở menu trái -> Chọn đơn -> Bấm Lấy hàng hoặc Giao thành công. Nhớ xác nhận đã thu tiền nếu là đơn COD."),
			new Intent(List.of("không đăng nhập được"), Set.of(), "Bạn hãy kiểm tra lại email/mật khẩu. Nếu quên mật khẩu, hãy bấm 'Quên mật khẩu' ở trang Đăng nhập để đặt lại."),
			new Intent(List.of("lỗi", "không vào được"), Set.of(), "Nếu gặp lỗi, bạn vui lòng tải lại trang (Ctrl + F5). Nếu vẫn lỗi, hãy liên hệ hotline 0987654321 để được hỗ trợ."),
			new Intent(List.of("quên mật khẩu"), Set.of(), "Để đặt lại mật khẩu, hãy bấm 'Quên mật khẩu' ở trang Đăng nhập và làm theo hướng dẫn."),
			new Intent(List.of("tạo tài khoản", "đăng ký"), Set.of(), "Bạn có thể bấm 'Đăng ký' ở góc phải trên cùng để tạo tài khoản mới."),
			new Intent(List.of("trả hàng", "hoàn tiền", "đổi trả", "refund"), Set.of(), "Bạn chỉ có thể yêu cầu trả hàng trong vòng 7 ngày kể từ khi đơn giao thành công. Vào Đơn hàng -> Xem chi tiết -> Bấm Yêu cầu trả hàng."),
			new Intent(List.of("phí giao hàng", "phí giao", "vận chuyển", "ship"), Set.of(), "Phí ship được tính tự động dựa trên nhà vận chuyển bạn chọn. Miễn phí ship cho đơn từ 500.000đ."),
			new Intent(List.of("mã giảm giá", "coupon"), Set.of(), "Tại trang Thanh toán, bạn nhập mã vào ô Mã giảm giá và bấm Áp dụng. Nếu có mã gợi ý, bạn có thể bấm chọn luôn."),
			new Intent(List.of("thanh toán momo", "momo"), Set.of(), "Tại trang Thanh toán, ở phần Phương thức thanh toán, bạn chọn Thanh toán qua ví MoMo. Hệ thống sẽ hiện mã QR, bạn bấm Xác nhận để hoàn tất."),
			new Intent(List.of("mua hàng", "đặt hàng"), Set.of(), "Để mua hàng, bạn chọn thẻ bài muốn mua, bấm Thêm vào giỏ. Sau đó vào Giỏ hàng, tick chọn sản phẩm và bấm Thanh toán."),
			new Intent(List.of("kiểm tra giá", "giá thẻ", "check giá", "bao nhiêu", "giá"), Set.of(), "__CARD_LOOKUP__"));

	private final CardRepository cardRepository;
	private final CardOfferService cardOfferService;
	private final ProductImageRepository productImageRepository;
	private final ChatbotPriceSearchService chatbotPriceSearchService;

	public ChatbotServiceImpl(CardRepository cardRepository, CardOfferService cardOfferService,
			ProductImageRepository productImageRepository, ChatbotPriceSearchService chatbotPriceSearchService) {
		this.cardRepository = cardRepository;
		this.cardOfferService = cardOfferService;
		this.productImageRepository = productImageRepository;
		this.chatbotPriceSearchService = chatbotPriceSearchService;
	}

	@Override
	@Transactional(readOnly = true)
	public ChatbotReplyResponse answer(String question) {
		String normalizedQuestion = normalize(question);
		if (SUPPORT_KEYWORDS.stream().map(this::normalize)
				.anyMatch(keyword -> containsPhrase(normalizedQuestion, keyword))) {
			return new ChatbotReplyResponse(SUPPORT_REPLY, SUPPORT_HTML);
		}
		PriceQuery priceQuery = parsePriceQuery(question);
		if (priceQuery != null) return findOffersInBudget(priceQuery);

		if (hasCardLookupIntent(normalizedQuestion)) {
			String cardKeyword = extractCardKeyword(normalizedQuestion);
			if (cardKeyword.isBlank()) {
				return new ChatbotReplyResponse("Bạn muốn tìm thẻ nào? Hãy nhập tên hoặc mã thẻ nhé.");
			}
			ChatbotReplyResponse cardResponse = findCardOffers(cardKeyword);
			if (cardResponse != null) return cardResponse;
			return new ChatbotReplyResponse("Chưa tìm thấy thẻ phù hợp với “" + cardKeyword + "”. Bạn thử nhập tên hoặc mã thẻ khác nhé.");
		}

		Set<String> roles = currentRoles();
		Intent matched = null;
		int bestSpecificity = -1;
		for (Intent intent : INTENTS) {
			if (!intent.allowedRoles().isEmpty() && intent.allowedRoles().stream().noneMatch(roles::contains)) continue;
			for (String keyword : intent.keywords()) {
				int specificity = containsPhrase(normalizedQuestion, normalize(keyword)) ? normalize(keyword).length() : -1;
				if (specificity > bestSpecificity) {
					matched = intent;
					bestSpecificity = specificity;
				}
			}
		}
		if (matched == null && !hasCardLookupIntent(normalizedQuestion)
				&& isLikelyStandaloneCardName(normalizedQuestion)) {
			String cardKeyword = extractCardKeyword(normalizedQuestion);
			ChatbotReplyResponse cardResponse = findCardOffers(cardKeyword);
			if (cardResponse != null) return cardResponse;
		}
		if (matched == null) return new ChatbotReplyResponse(DEFAULT_REPLY, DEFAULT_HTML);
		if ("__CARD_LOOKUP__".equals(matched.reply())) {
			return new ChatbotReplyResponse("Bạn muốn kiểm tra giá thẻ nào? Hãy gõ tên thẻ (VD: giá Pikachu).");
		}
		return new ChatbotReplyResponse(matched.reply());
	}

	private boolean isLikelyStandaloneCardName(String question) {
		String[] tokens = question.split("\\s+");
		return tokens.length > 0 && tokens.length <= 4
				&& java.util.Arrays.stream(tokens)
						.noneMatch(token -> NATURAL_QUESTION_WORDS.contains(token));
	}

	private ChatbotReplyResponse findCardOffers(String cardKeyword) {
		Map<Long, ChatbotCardCandidateProjection> candidatesById = new LinkedHashMap<>();
		for (String token : cardKeyword.split("\\s+")) {
			if (token.length() < 3) continue;
			cardRepository.findPublicChatbotMatches(token, PageRequest.of(0, 30))
					.forEach(card -> candidatesById.putIfAbsent(card.getId(), card));
			if (token.length() >= 6) {
				String typoTolerantPrefix = token.substring(0, token.length() - 2);
				cardRepository.findPublicChatbotMatches(typoTolerantPrefix, PageRequest.of(0, 100))
						.forEach(card -> candidatesById.putIfAbsent(card.getId(), card));
			}
		}
		ChatbotCardCandidateProjection match = bestCardMatch(cardKeyword, candidatesById.values());
		if (match == null) {
			List<ChatbotCardCandidateProjection> fuzzyCandidates = cardRepository
					.findPublicChatbotCandidates(PageRequest.of(0, 2000));
			match = bestCardMatch(cardKeyword, fuzzyCandidates);
		}
		if (match == null) return null;

		CardOffersResponse offersResponse = cardOfferService.getOffers(match.getId(), null, "price", "asc");
		List<CardOfferResponse> offers = offersResponse.offers().stream()
				.sorted(Comparator.comparing(CardOfferResponse::effectivePrice, Comparator.nullsLast(Comparator.naturalOrder())))
				.limit(3).toList();
		if (offers.isEmpty()) {
			String reply = "Đã tìm thấy thẻ " + match.getName() + ", nhưng hiện chưa có shop nào đang bán.";
			String imageUrl = safeImageUrl(match.getImageUrl());
			return new ChatbotReplyResponse(reply, buildCardHtml(match, List.of(), imageUrl), imageUrl);
		}

		NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
		format.setMaximumFractionDigits(0);
		String bestPrice = formatPrice(offers.get(0).effectivePrice(), format);
		String reply = "Tìm thấy " + match.getName() + ". Shop " + offers.get(0).shopName()
				+ " đang bán với giá tốt nhất (" + bestPrice + "), bạn nên mua ở đây.";
		String imageUrl = productImageUrl(offers.get(0).productId(), match.getImageUrl());
		return new ChatbotReplyResponse(reply, buildCardHtml(match, offers, imageUrl), imageUrl);
	}

	private ChatbotReplyResponse findOffersInBudget(PriceQuery query) {
		List<ChatbotPriceOfferResponse> offers = chatbotPriceSearchService.search(
				query.minPrice(), query.maxPrice(), query.targetPrice(), CHATBOT_PRICE_RESULT_LIMIT);
		if (offers.isEmpty()) {
			return new ChatbotReplyResponse("Chưa tìm thấy thẻ đang bán trong khoảng "
					+ formatVnd(query.minPrice()) + "–" + formatVnd(query.maxPrice()) + ". Bạn thử mức giá khác nhé.");
		}
		String reply = "Tìm thấy " + offers.size() + " thẻ trong khoảng "
				+ formatVnd(query.minPrice()) + "–" + formatVnd(query.maxPrice()) + ".";
		return new ChatbotReplyResponse(reply, buildPriceResultsHtml(offers), null);
	}

	private String buildPriceResultsHtml(List<ChatbotPriceOfferResponse> offers) {
		NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
		format.setMaximumFractionDigits(0);
		StringBuilder html = new StringBuilder("<div class=\"chat-price-results\">");
		for (ChatbotPriceOfferResponse offer : offers) {
			String image = safeImageUrl(offer.imageUrl());
			html.append("<div class=\"chat-card\"><a class=\"chat-card-link\" href=\"/products/")
					.append(offer.productId()).append("\" aria-label=\"Xem sản phẩm ")
					.append(HtmlUtils.htmlEscape(offer.cardName())).append("\"><img class=\"chat-card-img\" src=\"")
					.append(HtmlUtils.htmlEscape(image)).append("\" alt=\"Ảnh thẻ ")
					.append(HtmlUtils.htmlEscape(offer.cardName())).append("\" loading=\"lazy\"></a>")
					.append("<div class=\"chat-card-info\"><strong class=\"chat-card-name\">")
					.append(HtmlUtils.htmlEscape(offer.cardName())).append("</strong><ul class=\"chat-shop-list\"><li><span>")
					.append(HtmlUtils.htmlEscape(offer.shopName())).append("</span><strong>")
					.append(format.format(offer.price().setScale(0, RoundingMode.HALF_UP))).append("đ</strong></li></ul></div></div>");
		}
		return html.append("</div>").toString();
	}

	private PriceQuery parsePriceQuery(String question) {
		String normalized = normalizePriceText(question);
		Matcher matcher = PRICE_AMOUNT.matcher(normalized);
		while (matcher.find()) {
			String unit = matcher.group("unit");
			String currency = matcher.group("currency");
			String rawAmount = matcher.group("amount");
			boolean groupedAmount = rawAmount.matches("\\d{1,3}(?:[.,\\s]\\d{3})+");
			String beforeAmount = normalized.substring(0, matcher.start()).trim();
			if (unit == null && currency == null && !groupedAmount
					&& !PRICE_PREFIX.matcher(beforeAmount).find()) continue;

			BigDecimal amount = parseAmount(rawAmount);
			if (amount == null || amount.signum() <= 0) continue;
			if (unit != null) {
				amount = switch (unit.toLowerCase(Locale.ROOT)) {
					case "k", "ngan", "nghin" -> amount.multiply(BigDecimal.valueOf(1_000));
					case "tr", "trieu" -> amount.multiply(BigDecimal.valueOf(1_000_000));
					default -> amount;
				};
			}
			amount = amount.setScale(0, RoundingMode.HALF_UP);
			if (containsPricePhrase(normalized, "duoi", "toi da", "khong qua", "re hon")) {
				return new PriceQuery(BigDecimal.ZERO, amount, amount);
			}
			if (containsPricePhrase(normalized, "tren", "toi thieu", "lon hon")) {
				return new PriceQuery(amount, amount.multiply(new BigDecimal("1.5")), amount);
			}
			BigDecimal minPrice = amount.multiply(BigDecimal.ONE.subtract(AROUND_TOLERANCE)).setScale(0, RoundingMode.DOWN);
			BigDecimal maxPrice = amount.multiply(BigDecimal.ONE.add(AROUND_TOLERANCE)).setScale(0, RoundingMode.HALF_UP);
			return new PriceQuery(minPrice, maxPrice, amount);
		}
		return null;
	}

	private BigDecimal parseAmount(String rawAmount) {
		String amount = rawAmount.replaceAll("\\s+", "");
		try {
			if (amount.matches("\\d{1,3}(?:[.,]\\d{3})+")) {
				return new BigDecimal(amount.replaceAll("[.,]", ""));
			}
			return new BigDecimal(amount.replace(',', '.'));
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private String normalizePriceText(String value) {
		if (value == null) return "";
		String noMarks = DIACRITICS.matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
				.replaceAll("").replace('đ', 'd');
		return noMarks.replaceAll("[^\\p{L}\\p{N}.,\\s]", " ").trim().replaceAll("\\s+", " ");
	}

	private boolean containsPricePhrase(String question, String... phrases) {
		for (String phrase : phrases) {
			if (Pattern.compile("(?:^|\\s)" + Pattern.quote(phrase) + "(?:\\s|$)").matcher(question).find()) {
				return true;
			}
		}
		return false;
	}

	private String formatVnd(BigDecimal amount) {
		NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
		format.setMaximumFractionDigits(0);
		return format.format(amount) + "đ";
	}

	private String buildCardHtml(ChatbotCardCandidateProjection card, List<CardOfferResponse> offers, String imageUrl) {
		String image = safeImageUrl(imageUrl);
		StringBuilder html = new StringBuilder("<div class=\"chat-card\">");
		if (!offers.isEmpty()) {
			html.append("<a class=\"chat-card-link\" href=\"/products/")
					.append(offers.get(0).productId()).append("\" aria-label=\"Xem sản phẩm ")
					.append(HtmlUtils.htmlEscape(card.getName())).append("\">");
		}
		html.append("<img class=\"chat-card-img\" src=\"")
				.append(HtmlUtils.htmlEscape(image))
				.append("\" onerror=\"this.onerror=null;this.src='/img/placeholder-card.svg';\" alt=\"Ảnh thẻ ")
				.append(HtmlUtils.htmlEscape(card.getName())).append("\" loading=\"lazy\">");
		if (!offers.isEmpty()) html.append("</a>");
		html.append("<div class=\"chat-card-info\"><strong class=\"chat-card-name\">")
				.append(HtmlUtils.htmlEscape(card.getName())).append("</strong>");
		if (offers.isEmpty()) {
			html.append("<p>Hiện chưa có shop nào đang bán thẻ này.</p>");
		} else {
			NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
			format.setMaximumFractionDigits(0);
			html.append("<ul class=\"chat-shop-list\">");
			for (CardOfferResponse offer : offers) {
				html.append("<li><span>").append(HtmlUtils.htmlEscape(offer.shopName())).append("</span><strong>")
						.append(formatPrice(offer.effectivePrice(), format)).append("đ</strong></li>");
			}
			html.append("</ul><p class=\"chat-best-advice\">Shop ")
					.append(HtmlUtils.htmlEscape(offers.get(0).shopName())).append(" đang có giá tốt nhất: <strong>")
					.append(formatPrice(offers.get(0).effectivePrice(), format)).append("đ</strong>. Bạn có thể xem sản phẩm để đặt mua.</p>");
		}
		return html.append("</div></div>").toString();
	}

	private String productImageUrl(Long productId, String cardImageUrl) {
		return productImageRepository.findThumbnails(List.of(productId)).stream()
				.map(com.tcgshop.repository.ProductThumbnailProjection::getImageUrl)
				.filter(imageUrl -> imageUrl != null && !imageUrl.isBlank())
				.findFirst()
				.map(this::safeImageUrl)
				.orElseGet(() -> safeImageUrl(cardImageUrl));
	}

	private String safeImageUrl(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) return "/img/placeholder-card.svg";
		String value = imageUrl.trim();
		if (value.startsWith("https://") || value.startsWith("http://") || (value.startsWith("/") && !value.startsWith("//"))) return value;
		return "/img/placeholder-card.svg";
	}

	private String formatPrice(BigDecimal price, NumberFormat format) {
		return price == null ? "Liên hệ" : format.format(price.setScale(0, RoundingMode.HALF_UP));
	}

	private String extractCardKeyword(String normalizedQuestion) {
		StringBuilder remaining = new StringBuilder();
		for (String token : normalizedQuestion.split("\\s+")) {
			if (!token.isBlank() && !isCardFiller(token)) {
				if (!remaining.isEmpty()) remaining.append(' ');
				remaining.append(token);
			}
		}
		return remaining.toString();
	}

	private ChatbotCardCandidateProjection bestCardMatch(String keyword,
			java.util.Collection<ChatbotCardCandidateProjection> cards) {
		return cards.stream()
				.map(card -> new ScoredCard(card, scoreCard(keyword, card)))
				.filter(scored -> scored.score() >= 0.55)
				.max(Comparator.comparingDouble(ScoredCard::score))
				.map(ScoredCard::card)
				.orElse(null);
	}

	private boolean hasCardLookupIntent(String question) {
		String[] words = question.split("\\s+");
		boolean hasSearchVerb = List.of("mua", "check", "kiem", "tra", "tim", "find", "search")
				.stream()
				.anyMatch(token -> java.util.Arrays.stream(words).anyMatch(word -> word.equals(token)
						|| (word.length() >= 5 && levenshtein(word, token) <= 1)));
		boolean hasPriceSignal = java.util.Arrays.asList(words).contains("gia");
		boolean hasCardMarker = List.of("the", "la", "bai", "card")
				.stream().anyMatch(marker -> java.util.Arrays.asList(words).contains(marker));
		return (hasCardMarker && (hasSearchVerb || hasPriceSignal))
				|| (!extractCardKeyword(question).isBlank() && (hasSearchVerb || hasCardMarker));
	}

	private boolean isCardFiller(String token) {
		if (CARD_FILLER_WORDS.contains(token)) return true;
		if (token.length() >= 5 && CARD_FILLER_WORDS.stream()
				.anyMatch(filler -> filler.length() >= 4 && levenshtein(token, filler) == 1)) return true;
		return Set.of("muons", "muon").contains(token);
	}

	private double scoreCard(String keyword, ChatbotCardCandidateProjection card) {
		List<String> queryTokens = List.of(keyword.split("\\s+")).stream().filter(token -> token.length() > 1).toList();
		String name = normalize(card.getName());
		String number = normalize(card.getCardNumber());
		if (name.contains(keyword) || number.contains(keyword)) return 1.0;
		List<String> cardTokens = new ArrayList<>(List.of(name.split("\\s+")));
		cardTokens.addAll(List.of(number.split("\\s+")));
		double total = 0;
		for (String queryToken : queryTokens) {
			int best = Integer.MAX_VALUE;
			for (String cardToken : cardTokens) {
				best = Math.min(best, levenshtein(queryToken, cardToken));
			}
			int allowance = queryToken.length() >= 8 ? 2 : 1;
			total += best <= allowance ? 1.0 : Math.max(0, 1.0 - (double) best / Math.max(queryToken.length(), 1));
		}
		return queryTokens.isEmpty() ? 0 : total / queryTokens.size();
	}

	private int levenshtein(String left, String right) {
		int[] previous = new int[right.length() + 1];
		for (int j = 0; j <= right.length(); j++) previous[j] = j;
		for (int i = 1; i <= left.length(); i++) {
			int[] current = new int[right.length() + 1];
			current[0] = i;
			for (int j = 1; j <= right.length(); j++) {
				int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
			}
			previous = current;
		}
		return previous[right.length()];
	}

	private Set<String> currentRoles() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) return Set.of();
		return authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private String normalize(String value) {
		if (value == null) return "";
		String noMarks = DIACRITICS.matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
				.replaceAll("").replace('đ', 'd');
		return NON_WORDS.matcher(noMarks).replaceAll(" ").trim().replaceAll("\\s+", " ");
	}

	private boolean containsPhrase(String question, String keyword) {
		return (" " + question + " ").contains(" " + keyword + " ");
	}

	private record Intent(List<String> keywords, Set<String> allowedRoles, String reply) { }
	private record ScoredCard(ChatbotCardCandidateProjection card, double score) { }
	private record PriceQuery(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal targetPrice) { }
}
