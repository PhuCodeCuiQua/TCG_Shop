package com.tcgshop.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcgshop.config.CatalogDataImporter;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.mapper.CatalogImportMapperImpl;
import com.tcgshop.service.CatalogImportService;
import com.tcgshop.service.impl.CatalogImportServiceImpl;
import com.tcgshop.dto.request.CatalogImportRequest;
import com.tcgshop.dto.request.CatalogImportRequest.Item;
import com.tcgshop.dto.request.CatalogImportRequest.GameData;
import com.tcgshop.dto.request.CatalogImportRequest.SeriesData;
import com.tcgshop.entity.enums.ProductType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({CatalogImportServiceImpl.class, CatalogImportMapperImpl.class, com.tcgshop.config.OrderSideEffectConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogImportIntegrationTest {
    @Autowired private CatalogImportService service;
    @Autowired private UserRepository users;
    @Autowired private ShopRepository shops;
    @Autowired private ProductRepository products;
    @Autowired private ProductImageRepository images;
    @Autowired private CardRepository cards;
    @Autowired private GameRepository games;
    @Autowired private SeriesRepository series;
    @Autowired private PriceHistoryRepository histories;

    @Test
    void importsRealCatalogAndSecondPassAddsNoProductsOrImages() {
        User owner = new User(); owner.setEmail("catalog-test@example.com"); owner.setPassword("test-hash");
        owner.setStatus(UserStatus.ACTIVE); owner = users.saveAndFlush(owner);
        Shop shop = new Shop(); shop.setName("Catalog Shop"); shop.setSlug("catalog-shop"); shop.setOwner(owner);
        shop.setStatus(ShopStatus.APPROVED); shop.setDeleted(false); shop = shops.saveAndFlush(shop);
        assertThat(shop.getId()).isEqualTo(1L);
        User secondOwner = new User(); secondOwner.setEmail("catalog-test-second@example.com");
        secondOwner.setPassword("test-hash"); secondOwner.setStatus(UserStatus.ACTIVE);
        secondOwner = users.saveAndFlush(secondOwner);
        Shop secondShop = new Shop(); secondShop.setName("Catalog Shop Two");
        secondShop.setSlug("catalog-shop-two"); secondShop.setOwner(secondOwner);
        secondShop.setStatus(ShopStatus.APPROVED); secondShop.setDeleted(false);
        secondShop = shops.saveAndFlush(secondShop);
        Shop yuGiOhShop = new Shop(); yuGiOhShop.setName("YuGiOhh Catalog Shop");
        yuGiOhShop.setSlug("yugiohh-catalog-shop"); yuGiOhShop.setStatus(ShopStatus.APPROVED);
        yuGiOhShop.setDeleted(false); yuGiOhShop = shops.saveAndFlush(yuGiOhShop);
        List<Long> approvedShopIds = List.of(shop.getId(), secondShop.getId(), yuGiOhShop.getId());
        // Lỗi xảy ra sau khi tạo Game/Series phải rollback riêng item đó; đợt import sau vẫn chạy được.
        var invalid = new CatalogImportRequest(new Item("Broken Card", BigDecimal.TEN,
                "https://res.cloudinary.com/demo/image/upload/test.png", "test/card", null,
                "TEST", "1", "INVALID_RARITY", "ENGLISH", null), ProductType.SINGLE,
                List.of(new GameData("TEST", "Test Game", null)),
                List.of(new SeriesData("TEST", "TEST", "Test Series", null)));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> service.importItem(invalid));
        assertThat(games.count()).isZero();
        assertThat(series.count()).isZero();
        assertThat(products.count()).isZero();
        var runner = new CatalogDataImporter(new ObjectMapper(), service);
        runner.run();
        assertThat(products.count()).isEqualTo(373);
        assertThat(images.count()).isEqualTo(373);
        assertThat(cards.count()).isEqualTo(332);
        assertThat(histories.count()).isEqualTo(4648);
        assertThat(products.findAll()).allSatisfy(product ->
                assertThat(approvedShopIds).contains(product.getShop().getId()));
        var initialHistories = histories.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(com.tcgshop.entity.PriceHistory::getId,
                        com.tcgshop.entity.PriceHistory::getMarketPrice));
        runner.run();
        assertThat(products.count()).isEqualTo(373);
        assertThat(images.count()).isEqualTo(373);
        assertThat(cards.count()).isEqualTo(332);
        assertThat(histories.count()).isEqualTo(4648);
        assertThat(histories.findAll()).allSatisfy(row ->
                assertThat(row.getMarketPrice()).isEqualByComparingTo(initialHistories.get(row.getId())));
        // Mô phỏng DB đã import sản phẩm nhưng chưa có lịch sử giá trước bản sửa này.
        histories.deleteAllInBatch();
        runner.run();
        assertThat(products.count()).isEqualTo(373);
        assertThat(histories.count()).isEqualTo(4648);
    }
}
