package com.tcgshop.repository;

import com.tcgshop.dto.request.PublicProductSearchRequest.SortBy;
import com.tcgshop.entity.Product;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface PublicProductSearchRepositoryCustom {

	Page<PublicProductSearchProjection> searchPublic(
			Specification<Product> specification,
			Pageable pageable,
			SortBy sort,
			boolean topTwenty);

	List<PublicProductPriceProjection> findPublicPrices(
			Specification<Product> specification,
			Collection<Long> itemIds);
}
