package com.tcgshop.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcgshop.dto.request.CatalogImportRequest;
import com.tcgshop.dto.request.CatalogImportRequest.*;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.service.CatalogImportService;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.catalog-import.enabled", havingValue = "true")
public class CatalogDataImporter implements CommandLineRunner {
    private final ObjectMapper objectMapper;
    private final CatalogImportService service;

    @Override
    public void run(String... args) {
        int products = 0, images = 0, failures = 0, files = 0;
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:seed/*.json");
            Arrays.sort(resources, Comparator.comparing(resource -> resource.getFilename() == null
                    ? "" : resource.getFilename(), String.CASE_INSENSITIVE_ORDER));
            if (resources.length == 0) {
                failures++;
                log.error("Catalog import: không tìm thấy file JSON nào trong seed/");
            }
            for (Resource resource : resources) {
                files++;
                ImportStats stats = importFile(resource);
                products += stats.products();
                images += stats.images();
                failures += stats.failures();
            }
        } catch (Exception exception) {
            failures++;
            log.error("Catalog import: không quét được thư mục seed/: {}", exception.getMessage());
        }
        log.info("Catalog import hoàn tất: {} file, thêm {} sản phẩm, cập nhật {} ảnh, lỗi {} item/file",
                files, products, images, failures);
    }

    private ImportStats importFile(Resource resource) {
        int products = 0, images = 0, failures = 0;
        String fileName = resource.getFilename() == null ? resource.getDescription() : resource.getFilename();
        try (InputStream input = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            List<GameData> games = objectMapper.readerForListOf(GameData.class).readValue(root.path("games").traverse(objectMapper));
            List<SeriesData> series = objectMapper.readerForListOf(SeriesData.class).readValue(root.path("series").traverse(objectMapper));
            for (String group : List.of("cards", "sealedProducts", "supplies")) {
                JsonNode entries = root.path(group);
                if (!entries.isArray()) throw new IllegalArgumentException("Thiếu mảng " + group);
                for (JsonNode node : entries) {
                    try {
                        Item item = objectMapper.treeToValue(node, Item.class);
                        ProductType type = switch (group) {
                            case "cards" -> ProductType.SINGLE;
                            case "supplies" -> ProductType.SUPPLY;
                            default -> ProductType.valueOf(item.type());
                        };
                        if (group.equals("sealedProducts") && !List.of(ProductType.BOOSTER_BOX, ProductType.BOOSTER_PACK, ProductType.BUNDLE).contains(type))
                            throw new IllegalArgumentException("Loại hàng niêm phong không hợp lệ");
                        var result = service.importItem(new CatalogImportRequest(item, type, games, series));
                        products += result.productsCreated();
                        images += result.imagesUpdated();
                    } catch (Exception exception) {
                        failures++;
                        log.warn("Catalog import [{}]: bỏ qua {} ({}): {}", fileName, group,
                                node.path("name").asText(), exception.getMessage());
                    }
                }
            }
        } catch (Exception exception) {
            failures++;
            log.error("Catalog import: không đọc được {}: {}", fileName, exception.getMessage());
        }
        log.info("Catalog import [{}]: thêm {} sản phẩm, cập nhật {} ảnh, lỗi {} item",
                fileName, products, images, failures);
        return new ImportStats(products, images, failures);
    }

    private record ImportStats(int products, int images, int failures) {}
}
