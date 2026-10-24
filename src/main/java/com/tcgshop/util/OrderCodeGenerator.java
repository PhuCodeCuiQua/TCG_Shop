package com.tcgshop.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class OrderCodeGenerator {
	private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");
	private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final int RANDOM_SUFFIX_LENGTH = 6;
	private static final int MAX_ATTEMPTS = 5;
	private final SecureRandom secureRandom = new SecureRandom();

	public String generateOrderCode() {
		return generateCode("TCG");
	}

	public String generateOrderCode(Predicate<String> exists) {
		return generateUnique("TCG", exists);
	}

	public String generateGroupCode() {
		return generateCode("GRP");
	}

	public String generateGroupCode(Predicate<String> exists) {
		return generateUnique("GRP", exists);
	}

	public String generateUnique(String prefix, Predicate<String> exists) {
		Objects.requireNonNull(prefix, "prefix must not be null");
		Objects.requireNonNull(exists, "exists must not be null");
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String code = generateCode(prefix);
			if (!exists.test(code)) {
				return code;
			}
		}
		throw new IllegalStateException("Không thể tạo mã đơn hàng duy nhất sau 5 lần thử");
	}

	private String generateCode(String prefix) {
		StringBuilder code = new StringBuilder(prefix)
				.append(LocalDate.now(VIETNAM_ZONE).format(DATE_FORMAT));
		for (int index = 0; index < RANDOM_SUFFIX_LENGTH; index++) {
			code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}
}
