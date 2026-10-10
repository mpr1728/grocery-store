package com.grocerystore.api.catalog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HubProductRepository extends JpaRepository<HubProduct, Long> {

    Optional<HubProduct> findByHubIdAndProductIdAndActiveTrue(Long hubId, Long productId);

    // Available stock leaves out batches that are past their use-by date.
    @Query("""
            select new com.grocerystore.api.catalog.CatalogItem(
                p.id, p.sku, p.name, p.category, p.unitLabel, p.diet, p.privateLabel, p.description, hp.pricePaise,
                coalesce((select sum(b.quantity) from StockBatch b
                          where b.hubId = hp.hubId and b.productId = p.id
                            and (b.useBy is null or b.useBy >= :today)), 0L),
                p.brand, p.imageUrl, p.growingMethod,
                (select count(t) from StockBatch t
                 where t.hubId = hp.hubId and t.productId = p.id and t.quantity > 0
                   and (t.useBy is null or t.useBy >= :today) and t.testResult is not null),
                p.shelfLifeDays)
            from HubProduct hp join Product p on p.id = hp.productId
            where hp.hubId = :hubId and hp.active = true
              and (:category is null or p.category = :category)
              and (:namePattern is null or lower(p.name) like :namePattern)
            order by p.category, p.name
            """)
    List<CatalogItem> findCatalog(@Param("hubId") Long hubId, @Param("today") LocalDate today,
            @Param("category") String category, @Param("namePattern") String namePattern);
}
