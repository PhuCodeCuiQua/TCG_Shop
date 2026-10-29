package com.tcgshop.repository;

import com.tcgshop.entity.*;
import com.tcgshop.entity.enums.ShopStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CatalogImportRepository extends Repository<Product, Long> {
    @Query("select s.id from Shop s where s.status = :status and (s.deleted is null or s.deleted = false) order by s.id")
    List<Long> findShopIdsByStatus(@Param("status") ShopStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shop s where s.id = :shopId and s.status = :status and (s.deleted is null or s.deleted = false)")
    Shop lockApprovedShop(@Param("shopId") Long shopId, @Param("status") ShopStatus status);

    @Query("select distinct p from Product p left join fetch p.card c left join fetch c.series "
            + "left join fetch p.shop left join fetch p.images "
            + "where lower(p.name) = lower(:name) or lower(c.name) = lower(:name) order by p.id")
    List<Product> findProducts(@Param("name") String name);

    @Query("select g from Game g where lower(g.code) = lower(:code) or lower(g.name) = lower(:name) order by g.id")
    List<Game> findGames(@Param("code") String code, @Param("name") String name);

    @Query("select s from Series s where s.game.id = :gameId and lower(s.code) = lower(:code)")
    List<Series> findSeries(@Param("gameId") Long gameId, @Param("code") String code);

    @Query("select c from Card c where c.series.id = :seriesId and lower(c.cardNumber) = lower(:number) and c.language = :language")
    List<Card> findCards(@Param("seriesId") Long seriesId, @Param("number") String number,
            @Param("language") com.tcgshop.entity.enums.CardLanguage language);
}
