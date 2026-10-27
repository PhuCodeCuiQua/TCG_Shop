package com.tcgshop.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.ProductImage;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.repository.DataCleanupRepository;
import com.tcgshop.service.DataCleanupService;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCleanupServiceImpl implements DataCleanupService {
	private static final List<String> IMAGE_GROUPS = List.of("cards", "sealedProducts", "supplies");
	private static final List<String> PLACEHOLDER_MARKERS = List.of(
			"placeholder", "no-image", "no_image", "default-image", "default_product", "image-not-found");

	private final DataCleanupRepository dataCleanupRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public CleanupResult cleanupProductsWithoutRealImages(boolean apply) {
		CatalogAssets catalogAssets = loadCatalogAssets();
		if (!catalogAssets.loaded() || catalogAssets.urls().isEmpty() || catalogAssets.publicIds().isEmpty()) {
			log.warn("Product cleanup skipped: no complete Cloudinary asset list could be loaded from seed/*.json");
			return new CleanupResult(List.of(), 0, false);
		}

		List<Product> candidates = new ArrayList<>();
		for (Product product : dataCleanupRepository.findProductsForImageReview()) {
			if (!isCatalogProduct(product, catalogAssets) && !hasRealImage(product)) {
				candidates.add(product);
			}
		}

		if (!apply || candidates.isEmpty()) {
			return new CleanupResult(candidateIds(candidates), 0, true);
		}
		for (Product product : candidates) {
			product.setDeleted(true);
			product.setStatus(ProductStatus.INACTIVE);
		}
		dataCleanupRepository.saveAll(candidates);
		return new CleanupResult(candidateIds(candidates), candidates.size(), true);
	}

	private List<Long> candidateIds(List<Product> candidates) {
		return candidates.stream().map(Product::getId).toList();
	}

	private CatalogAssets loadCatalogAssets() {
		Set<String> urls = new HashSet<>();
		Set<String> publicIds = new HashSet<>();
		boolean loaded = false;
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:seed/*.json");
			for (Resource resource : resources) {
				try (InputStream input = resource.getInputStream()) {
					JsonNode root = objectMapper.readTree(input);
					for (String group : IMAGE_GROUPS) {
						JsonNode entries = root.path(group);
						if (!entries.isArray()) continue;
						for (JsonNode entry : entries) {
							addIfPresent(urls, entry.path("imageUrl").asText(null));
							addIfPresent(publicIds, entry.path("imagePublicId").asText(null));
						}
					}
					loaded = true;
				} catch (Exception exception) {
					throw new IllegalStateException("Could not read catalog asset list from " + resource.getFilename(), exception);
				}
			}
		} catch (Exception exception) {
			log.error("Product cleanup could not read seed catalog; no products will be hidden", exception);
			return new CatalogAssets(Set.of(), Set.of(), false);
		}
		boolean complete = loaded && !urls.isEmpty() && !publicIds.isEmpty();
		return new CatalogAssets(Set.copyOf(urls), Set.copyOf(publicIds), complete);
	}

	private boolean isCatalogProduct(Product product, CatalogAssets assets) {
		if (product.getCard() != null && matchesCatalogAsset(
				product.getCard().getImageUrl(), product.getCard().getImagePublicId(), assets)) {
			return true;
		}
		return product.getImages() != null && product.getImages().stream()
				.filter(image -> image != null)
				.anyMatch(image -> matchesCatalogAsset(image.getImageUrl(), image.getImagePublicId(), assets));
	}

	private boolean matchesCatalogAsset(String url, String publicId, CatalogAssets assets) {
		return (url != null && assets.urls().contains(url.trim()))
				|| (publicId != null && assets.publicIds().contains(publicId.trim()));
	}

	private boolean hasRealImage(Product product) {
		if (product.getCard() != null && isRealImageUrl(product.getCard().getImageUrl())) return true;
		return product.getImages() != null && product.getImages().stream()
				.filter(image -> image != null)
				.map(ProductImage::getImageUrl)
				.anyMatch(this::isRealImageUrl);
	}

	private boolean isRealImageUrl(String url) {
		if (url == null || url.isBlank()) return false;
		String normalized = url.trim().toLowerCase(Locale.ROOT);
		return PLACEHOLDER_MARKERS.stream().noneMatch(normalized::contains);
	}

	private void addIfPresent(Set<String> target, String value) {
		if (value != null && !value.isBlank()) target.add(value.trim());
	}

	private record CatalogAssets(Set<String> urls, Set<String> publicIds, boolean loaded) { }
}
