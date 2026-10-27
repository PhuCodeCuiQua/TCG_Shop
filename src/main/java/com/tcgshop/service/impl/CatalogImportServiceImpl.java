package com.tcgshop.service.impl;

import com.tcgshop.dto.request.CatalogImportRequest;
import com.tcgshop.dto.request.CatalogImportRequest.Item;
import com.tcgshop.dto.response.CatalogImportResponse;
import com.tcgshop.entity.*;
import com.tcgshop.entity.enums.*;
import com.tcgshop.mapper.CatalogImportMapper;
import com.tcgshop.repository.*;
import com.tcgshop.service.CatalogImportService;
import java.net.URI;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogImportServiceImpl implements CatalogImportService {
    private final CatalogImportRepository repository;
    private final ShopRepository shopRepository;
    private final ProductRepository products;
    private final CardRepository cards;
    private final SeriesRepository seriesRepository;
    private final GameRepository games;
    private final CatalogImportMapper mapper;
    private final PriceHistoryRepository priceHistories;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CatalogImportResponse importItem(CatalogImportRequest request) {
        return importItem(request, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CatalogImportResponse importItem(CatalogImportRequest request, Long targetShopId) {
        Item item = request.item();
        requireText(item.name(), "name", 255);
        requireText(item.imageUrl(), "imageUrl", 255);
        requireText(item.imagePublicId(), "imagePublicId", 255);
        URI imageUri = URI.create(item.imageUrl());
        if (!"https".equalsIgnoreCase(imageUri.getScheme()) || !"res.cloudinary.com".equalsIgnoreCase(imageUri.getHost()))
            throw new IllegalArgumentException("Ảnh phải là URL HTTPS Cloudinary");
        if (item.priceVnd() == null || item.priceVnd().signum() <= 0 || item.priceVnd().precision() - item.priceVnd().scale() > 13)
            throw new IllegalArgumentException("priceVnd phải lớn hơn 0 và phù hợp DECIMAL(15,2)");
        CardLanguage language = item.language() == null ? CardLanguage.ENGLISH : CardLanguage.valueOf(item.language());
        List<Product> identityMatches = repository.findProducts(item.name()).stream()
                .filter(p -> p.getType() == request.type())
                .filter(p -> matchesCard(p, item, language, request.type())).toList();
        List<Product> importedMatches = identityMatches.stream().filter(p -> hasCatalogImage(p, item)).toList();
        List<Product> matches = importedMatches.isEmpty()
                ? (identityMatches.size() == 1 ? identityMatches : List.of())
                : importedMatches;
        if (matches.stream().anyMatch(p -> Boolean.TRUE.equals(p.getDeleted())))
            throw new IllegalArgumentException("Sản phẩm đã xóa mềm; không tự khôi phục hoặc tạo trùng");
        int created = 0, images = 0;
        Shop targetShop = targetShopId == null ? null
                : shopRepository.findById(targetShopId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shop đích cho catalog"));
        if (matches.isEmpty()) {
            Product product = mapper.toProduct(item);
            product.setShop(targetShop == null ? selectApprovedShop() : targetShop);
            product.setType(request.type());
            product.setLanguage(language);
            product.setStock(10);
            product.setSoldCount(0);
            product.setRatingCount(0);
            product.setFavoriteCount(0);
            product.setDeleted(false);
            product.setStatus(ProductStatus.ACTIVE);
            if (request.type() == ProductType.SINGLE) {
                product.setCard(resolveCard(request, language));
                product.setCardCondition(CardCondition.NEAR_MINT);
            } else if (request.type() != ProductType.SUPPLY) {
                product.setSeries(resolveSeries(request));
            }
            matches = List.of(product);
            created = 1;
        }
        for (Product product : matches) {
            if (targetShop != null) product.setShop(targetShop);
            if (updateImage(product, item)) images++;
            products.saveAndFlush(product);
            seedMissingPriceHistory(product, item.priceVnd());
        }
        return new CatalogImportResponse(created, images);
    }

    @Override
    @Transactional
    public Long findOrCreateYuGiOhShop() {
        Map<Long, Shop> candidates = new LinkedHashMap<>();
        for (String keyword : List.of("Yugi", "Yu-Gi-Oh", "YuGiOh")) {
            for (Shop shop : shopRepository.findByNameContainingIgnoreCase(keyword)) {
                if (!Boolean.TRUE.equals(shop.getDeleted())) candidates.putIfAbsent(shop.getId(), shop);
            }
        }
        if (!candidates.isEmpty()) {
            return candidates.values().stream()
                    .sorted(Comparator.comparing((Shop shop) -> shop.getStatus() != ShopStatus.APPROVED)
                            .thenComparing(Shop::getId))
                    .findFirst().orElseThrow().getId();
        }

        Shop shop = new Shop();
        shop.setName("YuGiOhh");
        String slug = "yugiohh";
        int suffix = 2;
        while (shopRepository.existsBySlug(slug)) slug = "yugiohh-" + suffix++;
        shop.setSlug(slug);
        shop.setDescription("Cửa hàng dữ liệu mẫu Yu-Gi-Oh!");
        shop.setStatus(ShopStatus.APPROVED);
        shop.setDeleted(false);
        return shopRepository.saveAndFlush(shop).getId();
    }

    private Shop selectApprovedShop() {
        List<Long> approvedShopIds = repository.findShopIdsByStatus(ShopStatus.APPROVED);
        if (approvedShopIds.isEmpty()) {
            throw new IllegalArgumentException("Không có shop APPROVED để nhập sản phẩm");
        }

        int firstIndex = ThreadLocalRandom.current().nextInt(approvedShopIds.size());
        for (int offset = 0; offset < approvedShopIds.size(); offset++) {
            Long shopId = approvedShopIds.get((firstIndex + offset) % approvedShopIds.size());
            Shop shop = repository.lockApprovedShop(shopId, ShopStatus.APPROVED);
            if (shop != null) return shop;
        }
        throw new IllegalArgumentException("Không còn shop APPROVED khả dụng để nhập sản phẩm");
    }

    private boolean hasCatalogImage(Product product, Item item) {
        return product.getImages().stream().anyMatch(image ->
                Objects.equals(item.imagePublicId(), image.getImagePublicId())
                        || Objects.equals(item.imageUrl(), image.getImageUrl()));
    }

    private boolean matchesCard(Product product, Item item, CardLanguage language, ProductType type) {
        if (type != ProductType.SINGLE) return true;
        Card card = product.getCard();
        // Tên có thể trùng giữa nhiều bộ hoặc phiên bản; không gán ảnh theo tên một cách mù quáng.
        return card != null && Objects.equals(card.getCardNumber(), item.number()) && card.getLanguage() == language
                && card.getSeries() != null && card.getSeries().getCode().equalsIgnoreCase(item.seriesCode());
    }

    private void seedMissingPriceHistory(Product product, BigDecimal catalogPrice) {
        if (product.getType() != ProductType.SINGLE || product.getCard() == null) return;
        Card card = product.getCard();
        // Lịch sử thuộc Card; chỉ bổ sung các ngày còn thiếu trong cửa sổ chart gần nhất.
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDay = today.minusDays(13);
        java.util.Set<LocalDate> existingDates = priceHistories
                .findByCardIdAndRecordedDateBetweenOrderByRecordedDate(card.getId(), firstDay, today)
                .stream()
                .map(PriceHistory::getRecordedDate)
                .collect(java.util.stream.Collectors.toSet());
        List<PriceHistory> histories = new ArrayList<>(14);
        BigDecimal marketBase = card.getMarketPrice() != null ? card.getMarketPrice()
                : product.getPrice() != null ? product.getPrice() : catalogPrice;
        for (int daysAgo = 13; daysAgo >= 0; daysAgo--) {
            LocalDate recordedDate = today.minusDays(daysAgo);
            if (existingDates.contains(recordedDate)) continue;
            // Dữ liệu demo ±5%; sinh hệ số nguyên theo 1/10.000 để không dùng double cho tiền.
            BigDecimal factor = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(9500, 10501), 4);
            BigDecimal price = marketBase.multiply(factor).setScale(0, RoundingMode.HALF_UP);
            PriceHistory history = new PriceHistory();
            history.setCard(card);
            history.setRecordedDate(recordedDate);
            history.setMarketPrice(price);
            history.setAvgPrice(price);
            history.setSource("ADMIN");
            histories.add(history);
        }
        if (!histories.isEmpty()) priceHistories.saveAllAndFlush(histories);
    }

    private Card resolveCard(CatalogImportRequest request, CardLanguage language) {
        Item item = request.item();
        requireText(item.number(), "number", 255);
        Series series = resolveSeries(request);
        List<Card> existing = repository.findCards(series.getId(), item.number(), language);
        if (!existing.isEmpty()) return existing.get(0);
        Card card = new Card();
        card.setSeries(series);
        card.setName(item.name());
        card.setCardNumber(item.number());
        card.setLanguage(language);
        card.setRarity(item.rarity() == null ? Rarity.COMMON : Rarity.valueOf(item.rarity()));
        card.setMarketPrice(request.item().priceVnd());
        card.setActive(true);
        return cards.saveAndFlush(card);
    }

    private Series resolveSeries(CatalogImportRequest request) {
        requireText(request.item().seriesCode(), "seriesCode", 255);
        var data = request.series().stream().filter(s -> s.code().equalsIgnoreCase(request.item().seriesCode()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Không có series trong catalog"));
        var gameData = request.games().stream().filter(g -> g.code().equalsIgnoreCase(data.gameCode()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Không có game trong catalog"));
        List<Game> foundGames = repository.findGames(gameData.code(), gameData.name());
        if (foundGames.size() > 1) throw new IllegalArgumentException("Game trùng tên/mã, cần đối chiếu thủ công");
        Game game;
        if (foundGames.isEmpty()) {
            game = new Game(); game.setName(gameData.name()); game.setCode(gameData.code());
            game.setDescription(gameData.description()); game.setActive(true);
            game = games.saveAndFlush(game);
        } else game = foundGames.get(0);
        List<Series> found = repository.findSeries(game.getId(), data.code());
        if (!found.isEmpty()) return found.get(0);
        Series series = new Series(); series.setGame(game); series.setCode(data.code()); series.setName(data.name());
        if (data.releaseDate() != null) series.setReleaseDate(LocalDate.parse(data.releaseDate()));
        series.setActive(true);
        return seriesRepository.saveAndFlush(series);
    }

    private boolean updateImage(Product product, Item item) {
        List<ProductImage> images = product.getImages();
        ProductImage target = images.stream().filter(i -> item.imagePublicId().equals(i.getImagePublicId())
                || item.imageUrl().equals(i.getImageUrl())).findFirst()
                .orElseGet(() -> images.stream().filter(i -> Boolean.TRUE.equals(i.getIsMain())).findFirst().orElse(null));
        boolean changed = false;
        if (target == null) {
            target = new ProductImage(); target.setProduct(product); target.setDisplayOrder(0);
            images.add(target); changed = true;
        }
        changed |= !Objects.equals(target.getImageUrl(), item.imageUrl())
                || !Objects.equals(target.getImagePublicId(), item.imagePublicId()) || !Boolean.TRUE.equals(target.getIsMain());
        // Chỉ đồng bộ tham chiếu asset có sẵn, không upload/xóa asset Cloudinary dùng chung của catalog.
        target.setImageUrl(item.imageUrl()); target.setImagePublicId(item.imagePublicId()); target.setIsMain(true);
        for (ProductImage image : images) {
            if (image != target && Boolean.TRUE.equals(image.getIsMain())) { image.setIsMain(false); changed = true; }
        }
        return changed;
    }

    private void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength)
            throw new IllegalArgumentException(field + " thiếu hoặc quá dài");
    }
}
