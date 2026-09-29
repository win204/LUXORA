package com.luxora.commerce.catalog.repository;

import com.luxora.commerce.catalog.model.Product;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("select p from Product p")
    Page<Product> findAdminPage(Pageable pageable);


    @Query("""
            select new com.luxora.commerce.catalog.repository.AdminProductListMeta(
                p.id,
                count(v.id),
                min(case when v.active = true then v.price else null end),
                sum(case when v.active = true and i.quantityAvailable > 0 then 1 else 0 end)
            )
            from Product p
            left join p.variants v
            left join v.inventoryItem i
            where p.id in :productIds
            group by p.id
            """)
    List<AdminProductListMeta> findAdminListMeta(@Param("productIds") Collection<UUID> productIds);
    @EntityGraph(attributePaths = {"brand", "category", "images", "specifications", "variants", "variants.inventoryItem"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findAdminDetailById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("""
            select p
            from Product p
            where p.active = true
              and (:hasSearch = false or lower(p.name) like :searchPattern
                   or lower(p.subtitle) like :searchPattern)
              and (:category is null or p.category.slug = :category)
              and (:brand is null or p.brand.slug = :brand)
              and exists (
                  select 1
                  from ProductVariant v
                  where v.product = p
                    and v.active = true
                    and (:minPrice is null or v.price >= :minPrice)
                    and (:maxPrice is null or v.price <= :maxPrice)
              )
            order by
              case when :sort = 'nameAsc' then p.name end asc,
              case when :sort = 'priceAsc' then (
                  select min(vpa.price) from ProductVariant vpa where vpa.product = p and vpa.active = true
              ) end asc,
              case when :sort = 'priceDesc' then (
                  select min(vpd.price) from ProductVariant vpd where vpd.product = p and vpd.active = true
              ) end desc,
              p.id desc
            """)
    Page<Product> findProducts(
            @Param("hasSearch") boolean hasSearch,
            @Param("searchPattern") String searchPattern,
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("sort") String sort,
            Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "category", "images", "specifications", "variants", "variants.inventoryItem"})
    Optional<Product> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @Query("""
            select new com.luxora.commerce.catalog.repository.ProductListMeta(
                p.id,
                min(v.price),
                sum(case when i.quantityAvailable > 0 then 1 else 0 end)
            )
            from Product p
            join p.variants v
            left join v.inventoryItem i
            where p.id in :productIds
              and v.active = true
            group by p.id
            """)
    List<ProductListMeta> findListMeta(@Param("productIds") Collection<UUID> productIds);
}