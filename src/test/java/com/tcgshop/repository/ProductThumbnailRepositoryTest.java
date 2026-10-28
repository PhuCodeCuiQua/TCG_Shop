package com.tcgshop.repository;

import com.tcgshop.entity.*;
import com.tcgshop.entity.enums.*;
import com.tcgshop.mapper.ProductModerationMapper;
import com.tcgshop.mapper.VendorProductMapper;
import com.tcgshop.util.ProductImageUtil;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ProductThumbnailRepositoryTest {
    @Autowired private ProductRepository products;
    @Autowired private ProductImageRepository images;
    @Autowired private VendorProductRepository vendorProducts;
    @Autowired private UserRepository users;
    @Autowired private ShopRepository shops;
    @Autowired private CardRepository cards;
    @Autowired private HomeProductRepository homeProducts;
    @Autowired private PublicShopProductRepository publicShopProducts;
    @Autowired private GameRepository games;
    @Autowired private SeriesRepository series;

    @Test
    void vendorManagerAndDetailUseTheSameMainImageAndFallbacks() {
        User user = new User(); user.setEmail("thumbnail@example.com"); user.setPassword("test"); user.setStatus(UserStatus.ACTIVE);
        user = users.saveAndFlush(user);
        Shop shop = new Shop(); shop.setOwner(user); shop.setName("Thumb Shop"); shop.setSlug("thumb-shop");
        shop.setStatus(ShopStatus.APPROVED); shop = shops.saveAndFlush(shop);
        Card card = new Card(); card.setName("Card"); card.setImageUrl("/img/card.png"); card = cards.saveAndFlush(card);
        Product primary = product(shop, card, "Primary");
        image(primary, " ", true, 0); image(primary, "/img/gallery.png", false, 0); image(primary, "/img/main.png", true, 1);
        products.saveAndFlush(primary);
        Product gallery = product(shop, card, "Gallery"); image(gallery, "", true, 0); image(gallery, "/img/gallery.png", false, 1);
        products.saveAndFlush(gallery);
        Product cardOnly = product(shop, card, "Card fallback"); products.saveAndFlush(cardOnly);
        Product missing = product(shop, null, "Missing"); products.saveAndFlush(missing);
        var expected = new java.util.HashMap<Long, String>();
        expected.put(primary.getId(), "/img/main.png"); expected.put(gallery.getId(), "/img/gallery.png");
        expected.put(cardOnly.getId(), "/img/card.png"); expected.put(missing.getId(), null);
        var managerMapper = Mappers.getMapper(ProductModerationMapper.class);
        var vendorMapper = Mappers.getMapper(VendorProductMapper.class);
        var thumbnails = images.findThumbnails(List.copyOf(expected.keySet()));
        assertThat(thumbnails).hasSize(4);
        thumbnails.forEach(row -> assertThat(row.getImageUrl()).isEqualTo(expected.get(row.getProductId())));
        var vendorPage = vendorProducts.searchOwnedProducts(shop.getId(), null, null, null, PageRequest.of(0, 20));
        assertThat(vendorPage.getContent()).hasSize(4);
        vendorPage.forEach(row -> assertThat(vendorMapper.toResponse(row).imageUrl()).isEqualTo(expected.get(row.getId())));
        for (Product product : List.of(primary, gallery, cardOnly, missing)) {
            assertThat(ProductImageUtil.thumbnail(product)).isEqualTo(expected.get(product.getId()));
            assertThat(managerMapper.toResponse(product).imageUrl()).isEqualTo(expected.get(product.getId()));
            assertThat(Mappers.getMapper(com.tcgshop.mapper.VendorProductEditMapper.class).toResponse(product).imageUrl())
                    .isEqualTo(expected.get(product.getId()));
        }
        Game game = new Game(); game.setCode("COVER"); game.setName("Cover Game"); game = games.saveAndFlush(game);
        Series set = new Series(); set.setCode("SET"); set.setName("Cover Set"); set.setGame(game); set = series.saveAndFlush(set);
        card.setSeries(set); cards.saveAndFlush(card);
        Product expensive = product(shop, card, "Expensive"); expensive.setPrice(new BigDecimal("100"));
        image(expensive, "/img/expensive.png", true, 0); products.saveAndFlush(expensive);
        Product banned = product(shop, card, "Banned expensive"); banned.setPrice(new BigDecimal("1000"));
        banned.setStatus(ProductStatus.BANNED); image(banned, "/img/banned.png", true, 0); products.saveAndFlush(banned);
        var covers = homeProducts.findSeriesCovers(List.of(set.getId()));
        assertThat(covers).hasSize(1);
        assertThat(covers.get(0).getImageUrl()).isEqualTo("/img/expensive.png");
        Product highest = product(shop, card, "Highest uses card image"); highest.setPrice(new BigDecimal("200")); products.saveAndFlush(highest);
        assertThat(homeProducts.findSeriesCovers(List.of(set.getId())).get(0).getImageUrl()).isEqualTo("/img/card.png");

        Product supply = product(shop, null, "Supply image");
        image(supply, "/img/supply-gallery.png", false, 0);
        image(supply, "/img/supply-main.png", true, 1);
        products.saveAndFlush(supply);
        Product supplyWithoutMain = product(shop, null, "Supply without main");
        image(supplyWithoutMain, "/img/supply-gallery-only.png", false, 0);
        products.saveAndFlush(supplyWithoutMain);
        var supplyRows = homeProducts.findSuppliesAndSealed(PageRequest.of(0, 20));
        assertThat(supplyRows).filteredOn(row -> row.getName().equals("Supply image"))
                .extracting(HomeProductProjection::getImageUrl).containsExactly("/img/supply-main.png");
        assertThat(supplyRows).filteredOn(row -> row.getName().equals("Supply without main"))
                .extracting(HomeProductProjection::getImageUrl).containsExactly((String) null);
        assertThat(publicShopProducts.findPublicProducts(shop.getId(), ShopStatus.APPROVED,
                ProductStatus.ACTIVE, ProductType.SUPPLY, "supply image", PageRequest.of(0, 20)))
                .extracting(row -> row.getName()).containsExactly("Supply image");
    }

    private Product product(Shop shop, Card card, String name) {
        Product p = new Product(); p.setShop(shop); p.setCard(card); p.setName(name);
        p.setType(card == null ? ProductType.SUPPLY : ProductType.SINGLE); p.setPrice(BigDecimal.TEN);
        p.setStock(10); p.setStatus(ProductStatus.ACTIVE); p.setDeleted(false); return p;
    }

    private void image(Product product, String url, boolean main, int order) {
        ProductImage image = new ProductImage(); image.setProduct(product); image.setImageUrl(url);
        image.setIsMain(main); image.setDisplayOrder(order); product.getImages().add(image);
    }
}
