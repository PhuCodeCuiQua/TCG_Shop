package com.tcgshop.repository;

import com.tcgshop.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

	@Query("""
			select image from ProductImage image
			where image.product.id in :productIds
			and (image.isMain = true or image.isMain is null)
			and not exists (
				select preferred.id from ProductImage preferred
				where preferred.product.id = image.product.id and preferred.isMain = true
			)
			order by image.product.id, image.displayOrder asc, image.id asc
			""")
	List<ProductImage> findPrimaryOrFirstImages(@Param("productIds") Collection<Long> productIds);
}
