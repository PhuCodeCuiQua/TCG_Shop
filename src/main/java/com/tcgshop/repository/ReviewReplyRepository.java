package com.tcgshop.repository;

import com.tcgshop.entity.ReviewReply;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
	@EntityGraph(attributePaths = "sender")
	List<ReviewReply> findByReviewIdInOrderByCreatedAtAscIdAsc(Collection<Long> reviewIds);
}
