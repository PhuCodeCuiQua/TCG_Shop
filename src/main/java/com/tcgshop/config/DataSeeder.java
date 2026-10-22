package com.tcgshop.config;

import com.tcgshop.entity.Card;
import com.tcgshop.entity.CommissionSetting;
import com.tcgshop.entity.Game;
import com.tcgshop.entity.PriceHistory;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.Series;
import com.tcgshop.entity.ShippingCarrier;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.CardLanguage;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.CommissionSettingRepository;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.PriceHistoryRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.repository.ShippingCarrierRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed dữ liệu cơ bản khi khởi động (PA-06).
 * Idempotent: mỗi bảng được kiểm tra trước khi thêm, chạy lại nhiều lần không tạo trùng.
 * CHỈ DÙNG CHO MÔI TRƯỜNG PHÁT TRIỂN: tài khoản mẫu có mật khẩu công khai trong README.
 * Tắt bằng app.seed.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

	private static final String EMAIL_DOMAIN = "@tcgshop.local";

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final ShopRepository shopRepository;
	private final GameRepository gameRepository;
	private final SeriesRepository seriesRepository;
	private final CardRepository cardRepository;
	private final ProductRepository productRepository;
	private final ShippingCarrierRepository shippingCarrierRepository;
	private final PriceHistoryRepository priceHistoryRepository;
	private final CommissionSettingRepository commissionSettingRepository;
	private final BCryptPasswordEncoder passwordEncoder;

	// Mật khẩu mẫu chỉ để dev; mặc định thỏa quy tắc mật khẩu mạnh (>= 8 ký tự, hoa, thường, số)
	@Value("${app.seed.default-password:Tcg@12345}")
	private String defaultPassword;

	private record SeriesSeed(String gameCode, String code, String name, LocalDate releaseDate) {
	}

	private record CardSeed(String gameCode, String seriesCode, String name, String number, Rarity rarity,
			CardLanguage language, long price) {
	}

	@Override
	@Transactional
	public void run(String... args) {
		Map<RoleName, Role> roles = seedRoles();
		Map<String, User> users = seedUsers(roles);
		Map<String, Shop> shops = seedShops(users);
		seedCommission(users.get("admin"));
		Map<String, Game> games = seedGames();
		Map<String, Series> series = seedSeries(games);
		List<Card> cards = seedCards(series);
		seedProducts(shops, cards, series);
		seedCarriers();
		seedPriceHistories(cards);
		log.info("DataSeeder: hoàn tất kiểm tra/seed dữ liệu mẫu");
	}

	// ---------------------------------------------------------------- Role + User + Shop

	private Map<RoleName, Role> seedRoles() {
		Map<RoleName, Role> result = new EnumMap<>(RoleName.class);
		for (RoleName name : RoleName.values()) {
			Role role = roleRepository.findByName(name).orElseGet(() -> {
				Role created = new Role();
				created.setName(name);
				created.setDescription("Vai trò " + name.name());
				return roleRepository.save(created);
			});
			result.put(name, role);
		}
		return result;
	}

	private Map<String, User> seedUsers(Map<RoleName, Role> roles) {
		Map<String, User> users = new HashMap<>();
		users.put("admin", seedUser("admin", "Quản trị viên", "0900000001", roles.get(RoleName.ROLE_ADMIN)));
		users.put("manager", seedUser("manager", "Quản lý sàn", "0900000002", roles.get(RoleName.ROLE_MANAGER)));
		users.put("vendor1", seedUser("vendor1", "Chủ shop Một", "0900000003", roles.get(RoleName.ROLE_VENDOR)));
		users.put("vendor2", seedUser("vendor2", "Chủ shop Hai", "0900000004", roles.get(RoleName.ROLE_VENDOR)));
		users.put("user1", seedUser("user1", "Khách hàng Một", "0900000005", roles.get(RoleName.ROLE_USER)));
		users.put("shipper1", seedUser("shipper1", "Shipper Một", "0900000006", roles.get(RoleName.ROLE_SHIPPER)));
		return users;
	}

	private User seedUser(String key, String fullName, String phone, Role role) {
		String email = key + EMAIL_DOMAIN;
		return userRepository.findByEmail(email).orElseGet(() -> {
			User user = new User();
			user.setEmail(email);
			// Mã hóa bằng BCrypt của Phú, không lưu mật khẩu thô
			user.setPassword(passwordEncoder.encode(defaultPassword));
			user.setFullName(fullName);
			user.setPhoneNumber(phone);
			user.setStatus(UserStatus.ACTIVE);
			user.setEmailVerified(true);
			user.getRoles().add(role);
			return userRepository.save(user);
		});
	}

	private Map<String, Shop> seedShops(Map<String, User> users) {
		Map<String, Shop> shops = new HashMap<>();
		shops.put("vendor1", seedShop(users.get("vendor1"), "Card Paradise", "card-paradise", "0900000003",
				"12 Nguyễn Huệ, Quận 1, TP.HCM"));
		shops.put("vendor2", seedShop(users.get("vendor2"), "Dragon Deck Store", "dragon-deck-store", "0900000004",
				"45 Lê Lợi, Quận 3, TP.HCM"));
		return shops;
	}

	private Shop seedShop(User owner, String name, String slug, String phone, String pickupAddress) {
		return shopRepository.findByOwnerId(owner.getId()).orElseGet(() -> {
			Shop shop = new Shop();
			shop.setOwner(owner);
			shop.setName(name);
			shop.setSlug(slug);
			shop.setPhoneNumber(phone);
			shop.setPickupAddress(pickupAddress);
			shop.setDescription("Cửa hàng mẫu " + name);
			shop.setStatus(ShopStatus.APPROVED);
			shop.setRatingCount(0);
			shop.setDeleted(false);
			return shopRepository.save(shop);
		});
	}

	private void seedCommission(User admin) {
		if (commissionSettingRepository.count() > 0) {
			return;
		}
		CommissionSetting setting = new CommissionSetting();
		setting.setDefaultRate(new BigDecimal("5.00"));
		setting.setUpdatedBy(admin);
		commissionSettingRepository.save(setting);
	}

	// ---------------------------------------------------------------- Catalog

	private Map<String, Game> seedGames() {
		Map<String, Game> byCode = new HashMap<>();
		for (Game g : gameRepository.findAll()) {
			byCode.put(g.getCode(), g);
		}
		String[][] data = {
				{"POKEMON", "Pokémon", "Trò chơi thẻ bài Pokémon TCG"},
				{"YUGIOH", "Yu-Gi-Oh!", "Trò chơi thẻ bài Yu-Gi-Oh! TCG"},
				{"ONEPIECE", "One Piece", "Trò chơi thẻ bài One Piece Card Game"},
				{"MTG", "Magic: The Gathering", "Trò chơi thẻ bài Magic: The Gathering"}
		};
		for (String[] row : data) {
			if (!byCode.containsKey(row[0])) {
				Game game = new Game();
				game.setCode(row[0]);
				game.setName(row[1]);
				game.setDescription(row[2]);
				game.setActive(true);
				byCode.put(row[0], gameRepository.save(game));
			}
		}
		return byCode;
	}

	private Map<String, Series> seedSeries(Map<String, Game> games) {
		List<SeriesSeed> seeds = List.of(
				new SeriesSeed("POKEMON", "SV1", "Scarlet & Violet", LocalDate.of(2023, 3, 31)),
				new SeriesSeed("POKEMON", "SV2A", "Pokémon Card 151", LocalDate.of(2023, 6, 16)),
				new SeriesSeed("YUGIOH", "LOB", "Legend of Blue Eyes White Dragon", LocalDate.of(2002, 3, 8)),
				new SeriesSeed("ONEPIECE", "OP01", "Romance Dawn", LocalDate.of(2022, 12, 2)),
				new SeriesSeed("MTG", "M21", "Core Set 2021", LocalDate.of(2020, 7, 3)));

		// Khóa nhận diện series: gameCode|seriesCode
		Map<String, Series> result = new HashMap<>();
		for (Series s : seriesRepository.findAll()) {
			result.put(s.getGame().getCode() + "|" + s.getCode(), s);
		}
		for (SeriesSeed seed : seeds) {
			String key = seed.gameCode() + "|" + seed.code();
			if (!result.containsKey(key)) {
				Series s = new Series();
				s.setGame(games.get(seed.gameCode()));
				s.setCode(seed.code());
				s.setName(seed.name());
				s.setReleaseDate(seed.releaseDate());
				s.setActive(true);
				result.put(key, seriesRepository.save(s));
			}
		}
		return result;
	}

	private List<Card> seedCards(Map<String, Series> series) {
		List<CardSeed> seeds = List.of(
				new CardSeed("POKEMON", "SV2A", "Pikachu", "025", Rarity.RARE, CardLanguage.ENGLISH, 150_000),
				new CardSeed("POKEMON", "SV2A", "Charizard ex", "006", Rarity.ULTRA_RARE, CardLanguage.ENGLISH, 1_200_000),
				new CardSeed("POKEMON", "SV1", "Miraidon ex", "081", Rarity.ULTRA_RARE, CardLanguage.JAPANESE, 350_000),
				new CardSeed("YUGIOH", "LOB", "Blue-Eyes White Dragon", "001", Rarity.SECRET_RARE, CardLanguage.ENGLISH, 2_500_000),
				new CardSeed("YUGIOH", "LOB", "Dark Magician", "005", Rarity.ULTRA_RARE, CardLanguage.ENGLISH, 900_000),
				new CardSeed("ONEPIECE", "OP01", "Monkey D. Luffy", "003", Rarity.SECRET_RARE, CardLanguage.JAPANESE, 600_000),
				new CardSeed("ONEPIECE", "OP01", "Roronoa Zoro", "025", Rarity.RARE, CardLanguage.JAPANESE, 120_000),
				new CardSeed("MTG", "M21", "Lightning Strike", "152", Rarity.COMMON, CardLanguage.ENGLISH, 10_000),
				new CardSeed("MTG", "M21", "Teferi, Master of Time", "069", Rarity.HOLO_RARE, CardLanguage.ENGLISH, 450_000));

		// Khóa duy nhất theo ràng buộc của bảng: series|số thẻ|ngôn ngữ
		Set<String> existing = new HashSet<>();
		List<Card> all = cardRepository.findAll();
		for (Card c : all) {
			existing.add(cardKey(c.getSeries().getId(), c.getCardNumber(), c.getLanguage()));
		}
		for (CardSeed seed : seeds) {
			Series s = series.get(seed.gameCode() + "|" + seed.seriesCode());
			if (existing.add(cardKey(s.getId(), seed.number(), seed.language()))) {
				Card card = new Card();
				card.setSeries(s);
				card.setName(seed.name());
				card.setCardNumber(seed.number());
				card.setRarity(seed.rarity());
				card.setLanguage(seed.language());
				card.setMarketPrice(BigDecimal.valueOf(seed.price()));
				card.setActive(true);
				all.add(cardRepository.save(card));
			}
		}
		return all;
	}

	private String cardKey(Long seriesId, String number, CardLanguage language) {
		return seriesId + "|" + number + "|" + language;
	}

	// ---------------------------------------------------------------- Product

	private void seedProducts(Map<String, Shop> shops, List<Card> cards, Map<String, Series> series) {
		if (productRepository.count() > 0) {
			return;
		}
		Shop shop1 = shops.get("vendor1");
		Shop shop2 = shops.get("vendor2");

		for (Card card : cards) {
			BigDecimal market = card.getMarketPrice();
			// Hai shop bán cùng một thẻ với tình trạng và giá khác nhau => có dữ liệu cho bảng so sánh giá
			productRepository.save(newSingle(shop1, card, CardCondition.NEAR_MINT, market, 5));
			productRepository.save(newSingle(shop2, card, CardCondition.LIGHTLY_PLAYED,
					market.multiply(new BigDecimal("0.85")).setScale(0, java.math.RoundingMode.HALF_UP), 3));
		}

		// Hàng niêm phong theo series
		Series sv2a = series.get("POKEMON|SV2A");
		Series op01 = series.get("ONEPIECE|OP01");
		productRepository.save(newSealed(shop1, sv2a, ProductType.BOOSTER_BOX,
				"Booster Box Pokémon Card 151", new BigDecimal("1800000"), 10));
		productRepository.save(newSealed(shop2, op01, ProductType.BOOSTER_PACK,
				"Booster Pack One Piece Romance Dawn", new BigDecimal("60000"), 100));
	}

	private Product newSingle(Shop shop, Card card, CardCondition condition, BigDecimal price, int stock) {
		Product p = new Product();
		p.setShop(shop);
		p.setType(ProductType.SINGLE);
		p.setCard(card);
		p.setName(card.getName() + " (" + condition.getAbbreviation() + ")");
		p.setDescription("Thẻ đơn " + card.getName());
		p.setCardCondition(condition);
		p.setLanguage(card.getLanguage());
		p.setPrice(price);
		fillDefaults(p, stock);
		return p;
	}

	private Product newSealed(Shop shop, Series series, ProductType type, String name, BigDecimal price, int stock) {
		Product p = new Product();
		p.setShop(shop);
		p.setType(type);
		p.setSeries(series);
		p.setName(name);
		p.setDescription("Hàng niêm phong chính hãng: " + name);
		p.setLanguage(CardLanguage.ENGLISH);
		p.setPrice(price);
		fillDefaults(p, stock);
		return p;
	}

	// Gán giá trị rõ ràng cho các cột wrapper (Integer/Boolean) để tránh null
	private void fillDefaults(Product p, int stock) {
		p.setStock(stock);
		p.setSoldCount(0);
		p.setRatingCount(0);
		p.setFavoriteCount(0);
		p.setStatus(ProductStatus.ACTIVE);
		p.setDeleted(false);
	}

	// ---------------------------------------------------------------- Carrier + giá

	private void seedCarriers() {
		Set<String> existing = new HashSet<>();
		for (ShippingCarrier c : shippingCarrierRepository.findAll()) {
			existing.add(c.getCode());
		}
		Object[][] data = {
				{"GHN", "Giao Hàng Nhanh", "30000", "500000"},
				{"GHTK", "Giao Hàng Tiết Kiệm", "25000", "400000"},
				{"VTP", "Viettel Post", "28000", null}
		};
		for (Object[] row : data) {
			String code = (String) row[0];
			if (existing.contains(code)) {
				continue;
			}
			ShippingCarrier carrier = new ShippingCarrier();
			carrier.setCode(code);
			carrier.setName((String) row[1]);
			carrier.setBaseFee(new BigDecimal((String) row[2]));
			carrier.setFreeShipThreshold(row[3] == null ? null : new BigDecimal((String) row[3]));
			carrier.setActive(true);
			shippingCarrierRepository.save(carrier);
		}
	}

	private void seedPriceHistories(List<Card> cards) {
		if (priceHistoryRepository.count() > 0) {
			return;
		}
		LocalDate today = LocalDate.now();
		for (Card card : cards) {
			BigDecimal market = card.getMarketPrice();
			// 14 ngày gần nhất, giá dao động nhẹ theo chu kỳ (xác định, không dùng random)
			for (int daysAgo = 13; daysAgo >= 0; daysAgo--) {
				BigDecimal factor = BigDecimal.valueOf(100 + ((daysAgo * 7) % 11) - 5).movePointLeft(2);
				BigDecimal price = market.multiply(factor).setScale(0, java.math.RoundingMode.HALF_UP);
				PriceHistory history = new PriceHistory();
				history.setCard(card);
				history.setRecordedDate(today.minusDays(daysAgo));
				history.setMarketPrice(price);
				history.setAvgPrice(price);
				history.setSource("ADMIN");
				priceHistoryRepository.save(history);
			}
		}
	}
}