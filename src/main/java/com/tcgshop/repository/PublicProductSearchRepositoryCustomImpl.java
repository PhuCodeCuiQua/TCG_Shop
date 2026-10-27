package com.tcgshop.repository;

import com.tcgshop.dto.request.PublicProductSearchRequest.SortBy;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.enums.ProductType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class PublicProductSearchRepositoryCustomImpl implements PublicProductSearchRepositoryCustom {

	private static final int MIN_RATING_COUNT = 5;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Page<PublicProductSearchProjection> searchPublic(
			Specification<Product> specification,
			Pageable pageable,
			SortBy sort,
			boolean topTwenty) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
		Root<Product> product = query.from(Product.class);
		Join<Object, Object> card = product.join("card", JoinType.LEFT);
		Join<Object, Object> cardSeries = card.join("series", JoinType.LEFT);
		Join<Object, Object> cardGame = cardSeries.join("game", JoinType.LEFT);
		Join<Object, Object> productSeries = product.join("series", JoinType.LEFT);
		Join<Object, Object> productGame = productSeries.join("game", JoinType.LEFT);
		Join<Object, Object> shop = product.join("shop", JoinType.INNER);

		Expression<Boolean> single = criteriaBuilder.equal(product.get("type"), ProductType.SINGLE);
		Expression<Long> itemId = criteriaBuilder.<Long>selectCase()
				.when(single, card.get("id"))
				.otherwise(product.get("id"));
		Expression<Long> cardId = criteriaBuilder.max(criteriaBuilder.<Long>selectCase()
				.when(single, card.get("id"))
				.otherwise(criteriaBuilder.nullLiteral(Long.class)));
		// SINGLE được gộp theo Card; nút yêu thích vẫn cần ID Product thật trong nhóm đã lọc.
		Expression<Long> productId = criteriaBuilder.min(product.<Long>get("id"));
		Expression<Long> shopId = criteriaBuilder.max(criteriaBuilder.<Long>selectCase()
				.when(single, criteriaBuilder.nullLiteral(Long.class))
				.otherwise(shop.get("id")));
		Expression<String> shopName = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, criteriaBuilder.nullLiteral(String.class))
				.otherwise(shop.get("name")));
		Expression<String> name = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, card.get("name"))
				.otherwise(product.get("name")));
		Expression<String> cardNumber = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, card.get("cardNumber"))
				.otherwise(criteriaBuilder.nullLiteral(String.class)));
		Expression<String> gameName = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, cardGame.get("name"))
				.otherwise(productGame.get("name")));
		Expression<String> seriesName = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, cardSeries.get("name"))
				.otherwise(productSeries.get("name")));
		Expression<String> typeName = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, criteriaBuilder.literal(ProductType.SINGLE.name()))
				.otherwise(product.get("type").as(String.class)));
		Expression<String> languageName = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, criteriaBuilder.coalesce(
						product.get("language").as(String.class), card.get("language").as(String.class)))
				.otherwise(product.get("language").as(String.class)));
		Expression<String> rarityName = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, card.get("rarity").as(String.class))
				.otherwise(criteriaBuilder.nullLiteral(String.class)));
		Expression<BigDecimal> price = criteriaBuilder.min(product.<BigDecimal>get("price"));
		Expression<Long> sellerCount = criteriaBuilder.countDistinct(shop.get("id"));
		Expression<Long> soldCount = criteriaBuilder.sum(criteriaBuilder.coalesce(
				product.<Integer>get("soldCount").as(Long.class), 0L));
		Expression<Long> ratingCount = criteriaBuilder.sum(criteriaBuilder.coalesce(
				product.<Integer>get("ratingCount").as(Long.class), 0L));
		Expression<Long> favoriteCount = criteriaBuilder.sum(criteriaBuilder.coalesce(
				product.<Integer>get("favoriteCount").as(Long.class), 0L));
		Expression<BigDecimal> ratingCountDecimal = criteriaBuilder.sum(criteriaBuilder.coalesce(
				product.<Integer>get("ratingCount").as(BigDecimal.class), BigDecimal.ZERO));
		Expression<BigDecimal> weightedRatingTotal = criteriaBuilder.sum(criteriaBuilder.prod(
				criteriaBuilder.coalesce(product.<BigDecimal>get("avgRating"), BigDecimal.ZERO),
				criteriaBuilder.coalesce(product.<Integer>get("ratingCount").as(BigDecimal.class), BigDecimal.ZERO)));
		Expression<BigDecimal> avgRating = criteriaBuilder.<BigDecimal>selectCase()
				.when(criteriaBuilder.greaterThan(ratingCountDecimal, BigDecimal.ZERO),
						criteriaBuilder.quot(weightedRatingTotal, ratingCountDecimal).as(BigDecimal.class))
				.otherwise(criteriaBuilder.nullLiteral(BigDecimal.class));
		Expression<String> imageUrl = criteriaBuilder.greatest(criteriaBuilder.<String>selectCase()
				.when(single, card.get("imageUrl"))
				.otherwise(criteriaBuilder.nullLiteral(String.class)));
		Expression<LocalDateTime> latestCreatedAt = criteriaBuilder.greatest(product.<LocalDateTime>get("createdAt"));

		Predicate predicate = specification.toPredicate(product, query, criteriaBuilder);
		query.multiselect(
				itemId.alias("itemId"),
				productId.alias("productId"),
				cardId.alias("cardId"),
				criteriaBuilder.max(criteriaBuilder.<Integer>selectCase().when(single, 1).otherwise(0))
						.alias("singleCard"),
				shopId.alias("shopId"),
				shopName.alias("shopName"),
				name.alias("name"),
				cardNumber.alias("cardNumber"),
				gameName.alias("gameName"),
				seriesName.alias("seriesName"),
				typeName.alias("type"),
				languageName.alias("language"),
				rarityName.alias("rarity"),
				price.alias("price"),
				sellerCount.alias("sellerCount"),
				soldCount.alias("soldCount"),
				avgRating.alias("avgRating"),
				ratingCount.alias("ratingCount"),
				favoriteCount.alias("favoriteCount"),
				imageUrl.alias("imageUrl"));
		query.where(predicate);
		query.groupBy(itemId);
		if (sort == SortBy.TOP_RATED) {
			query.having(criteriaBuilder.greaterThanOrEqualTo(ratingCount, (long) MIN_RATING_COUNT));
		}
		query.orderBy(buildOrders(criteriaBuilder, sort, price, latestCreatedAt, soldCount,
				avgRating, ratingCount, favoriteCount, itemId));

		List<PublicProductSearchProjection> content = entityManager.createQuery(query)
				.setFirstResult(Math.toIntExact(pageable.getOffset()))
				.setMaxResults(pageable.getPageSize())
				.getResultList()
				.stream()
				.map(this::toProjection)
				.toList();

		long total = topTwenty ? content.size() : countMatches(specification, criteriaBuilder);
		return new PageImpl<>(content, pageable, total);
	}

	private List<Order> buildOrders(
			CriteriaBuilder criteriaBuilder,
			SortBy sort,
			Expression<BigDecimal> price,
			Expression<?> latestCreatedAt,
			Expression<Long> soldCount,
			Expression<BigDecimal> avgRating,
			Expression<Long> ratingCount,
			Expression<Long> favoriteCount,
			Expression<Long> itemId) {
		List<Order> orders = new ArrayList<>();
		switch (sort) {
			case PRICE_ASC -> orders.add(criteriaBuilder.asc(price));
			case PRICE_DESC -> orders.add(criteriaBuilder.desc(price));
			case BEST_SELLING -> orders.add(criteriaBuilder.desc(soldCount));
			case TOP_RATED -> {
				orders.add(criteriaBuilder.desc(avgRating));
				orders.add(criteriaBuilder.desc(ratingCount));
			}
			case FAVORITES -> orders.add(criteriaBuilder.desc(favoriteCount));
			case NEWEST -> orders.add(criteriaBuilder.desc(latestCreatedAt));
		}
		orders.add(criteriaBuilder.asc(itemId));
		return orders;
	}

	private long countMatches(Specification<Product> specification, CriteriaBuilder criteriaBuilder) {
		CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
		Root<Product> product = countQuery.from(Product.class);
		Expression<Long> itemId = criteriaBuilder.<Long>selectCase()
				.when(criteriaBuilder.equal(product.get("type"), ProductType.SINGLE),
						product.get("card").get("id"))
				.otherwise(product.get("id"));
		Predicate predicate = specification.toPredicate(product, countQuery, criteriaBuilder);
		countQuery.select(criteriaBuilder.countDistinct(itemId)).where(predicate);
		return entityManager.createQuery(countQuery).getSingleResult();
	}

	private PublicProductSearchProjection toProjection(Tuple tuple) {
		String typeName = tuple.get("type", String.class);
		String languageName = tuple.get("language", String.class);
		String rarityName = tuple.get("rarity", String.class);
		Number rating = tuple.get("avgRating", Number.class);
		return new PublicProductSearchProjection(
				tuple.get("itemId", Long.class),
				tuple.get("productId", Long.class),
				tuple.get("cardId", Long.class),
				tuple.get("singleCard", Integer.class) == 1,
				tuple.get("shopId", Long.class),
				tuple.get("shopName", String.class),
				tuple.get("name", String.class),
				tuple.get("cardNumber", String.class),
				tuple.get("gameName", String.class),
				tuple.get("seriesName", String.class),
				typeName == null ? null : ProductType.valueOf(typeName),
				languageName == null ? null : com.tcgshop.entity.enums.CardLanguage.valueOf(languageName),
				rarityName == null ? null : com.tcgshop.entity.enums.Rarity.valueOf(rarityName),
				tuple.get("price", BigDecimal.class),
				tuple.get("sellerCount", Long.class),
				tuple.get("soldCount", Long.class),
				rating == null ? null : BigDecimal.valueOf(rating.doubleValue()),
				tuple.get("ratingCount", Long.class),
				tuple.get("favoriteCount", Long.class),
				tuple.get("imageUrl", String.class));
	}
}
