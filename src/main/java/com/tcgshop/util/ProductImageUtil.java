package com.tcgshop.util;

import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import java.util.Comparator;

public final class ProductImageUtil {
    private ProductImageUtil() {}

    public static String thumbnail(Product product) {
        if (product == null) return null;
        if (product.getImages() != null) {
            var selected = product.getImages().stream()
                    .filter(image -> image.getImageUrl() != null && !image.getImageUrl().isBlank())
                    .sorted(Comparator.comparing((ProductImage image) -> !Boolean.TRUE.equals(image.getIsMain()))
                            .thenComparing(ProductImage::getDisplayOrder, Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(ProductImage::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .findFirst();
            if (selected.isPresent()) return selected.get().getImageUrl();
        }
        var card = product.getCard();
        return card != null && card.getImageUrl() != null && !card.getImageUrl().isBlank() ? card.getImageUrl() : null;
    }
}
