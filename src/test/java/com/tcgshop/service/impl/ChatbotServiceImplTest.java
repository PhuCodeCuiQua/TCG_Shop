package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tcgshop.dto.response.ChatbotPriceOfferResponse;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.ProductImageRepository;
import com.tcgshop.service.CardOfferService;
import com.tcgshop.service.ChatbotPriceSearchService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceImplTest {
	@Mock
	private CardRepository cardRepository;
	@Mock
	private CardOfferService cardOfferService;
	@Mock
	private ProductImageRepository productImageRepository;
	@Mock
	private ChatbotPriceSearchService chatbotPriceSearchService;
	@InjectMocks
	private ChatbotServiceImpl chatbotService;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@ParameterizedTest
	@CsvSource({
			"'có lá nào tầm giá 200 ngàn vnđ không', 150000, 250000, 200000",
			"'khoảng 200k', 150000, 250000, 200000",
			"'dưới 500.000đ', 0, 500000, 500000",
			"'200 nghìn', 150000, 250000, 200000"
	})
	void recognizesVietnameseBudgetPhrasesAndSearchesWithinExpectedRange(
			String question, String minPrice, String maxPrice, String targetPrice) {
		when(chatbotPriceSearchService.search(
				new BigDecimal(minPrice), new BigDecimal(maxPrice), new BigDecimal(targetPrice), 5))
				.thenReturn(List.of());

		chatbotService.answer(question);

		verify(chatbotPriceSearchService).search(
				new BigDecimal(minPrice), new BigDecimal(maxPrice), new BigDecimal(targetPrice), 5);
		verifyNoInteractions(cardRepository, cardOfferService, productImageRepository);
	}

	@Test
	void rendersBudgetMatchesAsProductCardsWithImageNamePriceAndShop() {
		ChatbotPriceOfferResponse offer = new ChatbotPriceOfferResponse(
				42L, "Sogeking", "https://res.cloudinary.com/demo/sogeking.png",
				"Shop One", new BigDecimal("200000"));
		when(chatbotPriceSearchService.search(
				new BigDecimal("150000"), new BigDecimal("250000"), new BigDecimal("200000"), 5))
				.thenReturn(List.of(offer));

		var response = chatbotService.answer("có lá nào tầm giá 200 ngàn vnđ không");

		assertEquals("Tìm thấy 1 thẻ trong khoảng 150.000đ–250.000đ.", response.reply());
		assertTrue(response.html().contains("href=\"/products/42\""));
		assertTrue(response.html().contains("https://res.cloudinary.com/demo/sogeking.png"));
		assertTrue(response.html().contains("Sogeking"));
		assertTrue(response.html().contains("Shop One"));
		assertTrue(response.html().contains("200.000đ"));
	}

	@ParameterizedTest
	@CsvSource({
			"'trung tâm hỗ trợ'",
			"'cho tôi các câu hỏi thường gặp'",
			"'liên hệ'",
			"'tìm thẻ bài qua trung tâm hỗ trợ'"
	})
	void routesSupportAndFaqQuestionsToHelpPages(String question) {
		var response = chatbotService.answer(question);

		assertEquals("Bạn có thể xem Câu hỏi thường gặp (/faq) hoặc trang Liên hệ (/contact) để được hỗ trợ.",
				response.reply());
		assertTrue(response.html().contains("href=\"/faq\""));
		assertTrue(response.html().contains("href=\"/contact\""));
		verifyNoInteractions(cardRepository, cardOfferService, productImageRepository, chatbotPriceSearchService);
	}

	@Test
	void supportIntentWinsOverVendorCouponScenario() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"vendor", "n/a", List.of(new SimpleGrantedAuthority("ROLE_VENDOR"))));

		var response = chatbotService.answer("cho tôi các câu hỏi thường gặp");

		assertFalse(response.reply().contains("Mã giảm giá của Shop"));
		assertTrue(response.html().contains("href=\"/faq\""));
		verifyNoInteractions(cardRepository, cardOfferService, productImageRepository, chatbotPriceSearchService);
	}

	@Test
	void onlyExplicitForgotPasswordPhraseReturnsPasswordResetGuidance() {
		var response = chatbotService.answer("quên mật khẩu");

		assertTrue(response.reply().contains("đặt lại mật khẩu"));
		assertFalse(response.reply().contains("tạo tài khoản"));
	}

	@Test
	void registrationPhraseDoesNotReturnForgotPasswordScenario() {
		var response = chatbotService.answer("đăng ký");

		assertTrue(response.reply().contains("tạo tài khoản mới"));
		assertFalse(response.reply().contains("Quên mật khẩu"));
	}

	@Test
	void unmatchedQuestionReturnsFriendlyFallbackWithFaqLink() {
		var response = chatbotService.answer("hệ thống có giao diện tối không vậy");

		assertEquals("Xin lỗi, tôi chưa hiểu ý bạn. Bạn có thể hỏi tôi về tìm kiếm thẻ bài, hoặc các câu hỏi thường gặp tại đây (link /faq).",
				response.reply());
		assertTrue(response.html().contains("href=\"/faq\""));
		verifyNoInteractions(cardRepository, cardOfferService, productImageRepository, chatbotPriceSearchService);
	}
}
