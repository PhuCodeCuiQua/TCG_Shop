package com.tcgshop.repository;

import com.tcgshop.entity.Coupon;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CouponSuggestionRepository extends Repository<Coupon, Long> {
    @Query("select c from Coupon c join fetch c.shop s where s.id in :shopIds "
            + "and c.status = com.tcgshop.entity.enums.ActiveStatus.ACTIVE "
            + "and c.startDate <= :now and c.endDate >= :now "
            + "and (c.totalQuantity is null or c.usedQuantity < c.totalQuantity) "
            + "order by c.endDate, c.id")
    List<Coupon> findCandidates(@Param("shopIds") Collection<Long> shopIds,
            @Param("now") LocalDateTime now);
}
