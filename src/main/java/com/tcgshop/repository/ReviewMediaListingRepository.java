package com.tcgshop.repository;

import com.tcgshop.entity.ReviewMedia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReviewMediaListingRepository extends Repository<ReviewMedia, Long> {
	@Query("select media.review.id as reviewId, media.mediaUrl as mediaUrl, media.mediaType as mediaType "
			+ "from ReviewMedia media where media.review.id in :reviewIds order by media.id")
	List<ReviewMediaListingRow> findForReviews(@Param("reviewIds") Collection<Long> reviewIds);
}
