package com.tcgshop.service.impl;

import com.tcgshop.dto.request.CatalogImportRequest;
import com.tcgshop.dto.request.CatalogImportRequest.Item;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.CardCondition;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.mapper.CatalogImportMapper;
import com.tcgshop.repository.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogImportServiceImplTest {
    private final CatalogImportRepository repository = mock(CatalogImportRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final CardRepository cards = mock(CardRepository.class);
    private final SeriesRepository series = mock(SeriesRepository.class);
    private final GameRepository games = mock(GameRepository.class);
    private final CatalogImportMapper mapper = mock(CatalogImportMapper.class);
    private final PriceHistoryRepository histories = mock(PriceHistoryRepository.class);
    private final java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-26T10:00:00Z"), java.time.ZoneOffset.UTC);
    private final CatalogImportServiceImpl service = new CatalogImportServiceImpl(repository, products, cards, series, games, mapper, histories, clock);
    private final Item item = new Item("Sleeves", BigDecimal.TEN, "https://res.cloudinary.com/demo/image/upload/sleeves.png",
            "catalog/sleeves", null, null, null, null, null, null);
    private CatalogImportRequest request() { return new CatalogImportRequest(item, ProductType.SUPPLY, List.of(), List.of()); }

    @Test
    void createsProductAndMainImageThenRepeatedImportDoesNotDuplicateOrResetStock() {
        Shop shop = new Shop(); shop.setId(1L);
        Product product = new Product(); product.setName("Sleeves");
        when(repository.findShopIdsByStatus(ShopStatus.APPROVED)).thenReturn(List.of(1L));
        when(repository.lockApprovedShop(1L, ShopStatus.APPROVED)).thenReturn(shop);
        when(repository.findProducts("Sleeves")).thenReturn(List.of());
        when(mapper.toProduct(item)).thenReturn(product);
        var first = service.importItem(request());
        assertEquals(1, first.productsCreated()); assertEquals(1, first.imagesUpdated());
        assertEquals(10, product.getStock()); assertSame(shop, product.getShop());
        assertEquals(1, product.getImages().size());
        assertTrue(product.getImages().get(0).getIsMain());
        product.setStock(7);
        when(repository.findProducts("Sleeves")).thenReturn(List.of(product));
        var second = service.importItem(request());
        assertEquals(0, second.productsCreated()); assertEquals(0, second.imagesUpdated());
        assertEquals(7, product.getStock()); assertEquals(1, product.getImages().size());
        verifyNoInteractions(histories);
    }

    @Test
    void createsMissingListingForEveryApprovedShop() {
        Shop firstShop = new Shop(); firstShop.setId(1L);
        Shop secondShop = new Shop(); secondShop.setId(2L);
        when(repository.findShopIdsByStatus(ShopStatus.APPROVED)).thenReturn(List.of(1L, 2L));
        when(repository.lockApprovedShop(1L, ShopStatus.APPROVED)).thenReturn(firstShop);
        when(repository.lockApprovedShop(2L, ShopStatus.APPROVED)).thenReturn(secondShop);
        when(repository.findProducts("Sleeves")).thenReturn(List.of());
        when(mapper.toProduct(item)).thenAnswer(invocation -> {
            Product product = new Product(); product.setType(ProductType.SUPPLY);
            return product;
        });

        var result = service.importItem(request());

        assertEquals(2, result.productsCreated());
        assertEquals(2, result.imagesUpdated());
        var savedProducts = org.mockito.ArgumentCaptor.forClass(Product.class);
        verify(products, times(2)).saveAndFlush(savedProducts.capture());
        assertEquals(List.of(firstShop, secondShop),
                savedProducts.getAllValues().stream().map(Product::getShop).toList());
    }

    @Test
    void skipsExistingListingWithoutUpdatingItsImageOrBusinessData() {
        Shop shop = new Shop(); shop.setId(1L);
        Product product = new Product(); product.setType(ProductType.SUPPLY);
        product.setShop(shop);
        product.setPrice(new BigDecimal("12345"));
        ProductImage image = new ProductImage(); image.setProduct(product); image.setIsMain(true);
        image.setImageUrl("https://res.cloudinary.com/demo/image/upload/old.png"); image.setImagePublicId("old");
        product.getImages().add(image);
        when(repository.findShopIdsByStatus(ShopStatus.APPROVED)).thenReturn(List.of(1L));
        when(repository.lockApprovedShop(1L, ShopStatus.APPROVED)).thenReturn(shop);
        when(repository.findProducts("Sleeves")).thenReturn(List.of(product));

        var result = service.importItem(request());

        assertEquals(0, result.productsCreated()); assertEquals(0, result.imagesUpdated());
        assertEquals(1, product.getImages().size()); assertEquals("old", image.getImagePublicId());
        assertEquals(new BigDecimal("12345"), product.getPrice());
        verify(products, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void missingDefaultShopRejectsItem() {
        assertThrows(IllegalArgumentException.class, () -> service.importItem(request()));
        verifyNoInteractions(products, mapper);
    }

    @Test
    void backfillsFourteenDaysForExistingSingleUsingCardMarketPrice() {
        var request = existingSingle();
        approvedShop(1L);
        when(histories.findByCardIdAndRecordedDateBetweenOrderByRecordedDate(77L,
                java.time.LocalDate.of(2026, 10, 13), java.time.LocalDate.of(2026, 10, 26))).thenReturn(List.of());
        service.importItem(request);
        var captured = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(histories).saveAllAndFlush(captured.capture());
        assertEquals(14, captured.getValue().size());
        for (int index = 0; index < 14; index++) {
            var row = (com.tcgshop.entity.PriceHistory) captured.getValue().get(index);
            assertEquals(java.time.LocalDate.of(2026, 10, 13).plusDays(index), row.getRecordedDate());
            assertEquals(77L, row.getCard().getId());
            assertEquals(row.getMarketPrice(), row.getAvgPrice());
            assertEquals("ADMIN", row.getSource());
            assertTrue(row.getMarketPrice().compareTo(new BigDecimal("5700")) >= 0);
            assertTrue(row.getMarketPrice().compareTo(new BigDecimal("6300")) <= 0);
        }
    }

    @Test
    void backfillsOnlyMissingDatesAndDoesNotReplaceExistingHistory() {
        var request = existingSingle();
        approvedShop(1L);
        var existing = new com.tcgshop.entity.PriceHistory();
        existing.setRecordedDate(java.time.LocalDate.of(2026, 10, 20));
        existing.setMarketPrice(new BigDecimal("1234"));
        existing.setAvgPrice(new BigDecimal("1200"));
        existing.setSource("JOB");
        when(histories.findByCardIdAndRecordedDateBetweenOrderByRecordedDate(77L,
                java.time.LocalDate.of(2026, 10, 13), java.time.LocalDate.of(2026, 10, 26)))
                .thenReturn(List.of(existing));

        service.importItem(request);

        var captured = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(histories).saveAllAndFlush(captured.capture());
        assertEquals(13, captured.getValue().size());
        for (Object value : captured.getValue()) {
            var row = (com.tcgshop.entity.PriceHistory) value;
            assertNotEquals(java.time.LocalDate.of(2026, 10, 20), row.getRecordedDate());
        }
        assertEquals(new BigDecimal("1234"), existing.getMarketPrice());
        assertEquals("JOB", existing.getSource());
    }

    private CatalogImportRequest existingSingle() {
        var card = new com.tcgshop.entity.Card(); card.setId(77L); card.setCardNumber("1");
        card.setMarketPrice(new BigDecimal("6000"));
        card.setLanguage(com.tcgshop.entity.enums.CardLanguage.ENGLISH);
        var set = new com.tcgshop.entity.Series(); set.setCode("sv1"); card.setSeries(set);
        Product product = new Product(); product.setType(ProductType.SINGLE); product.setCard(card);
        product.setCardCondition(CardCondition.NEAR_MINT);
        Shop shop = new Shop(); shop.setId(1L); product.setShop(shop);
        product.setPrice(new BigDecimal("6000"));
        when(repository.findProducts("Altaria")).thenReturn(List.of(product));
        return new CatalogImportRequest(new Item("Altaria", BigDecimal.TEN, item.imageUrl(), item.imagePublicId(),
                null, "sv1", "1", null, "ENGLISH", null), ProductType.SINGLE, List.of(), List.of());
    }

    private void approvedShop(Long shopId) {
        Shop shop = new Shop(); shop.setId(shopId);
        when(repository.findShopIdsByStatus(ShopStatus.APPROVED)).thenReturn(List.of(shopId));
        when(repository.lockApprovedShop(shopId, ShopStatus.APPROVED)).thenReturn(shop);
    }
}
