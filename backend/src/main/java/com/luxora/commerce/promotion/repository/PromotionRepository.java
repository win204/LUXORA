package com.luxora.commerce.promotion.repository;
import com.luxora.commerce.promotion.model.Promotion;
import jakarta.persistence.LockModeType;
import java.util.Optional; import java.util.UUID;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface PromotionRepository extends JpaRepository<Promotion,UUID>{
 boolean existsByCode(String code); boolean existsByCodeAndIdNot(String code,UUID id);
 Optional<Promotion> findByCode(String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Promotion p where p.code=:code") Optional<Promotion> findByCodeForUpdate(@Param("code") String code);
 @Query("select p from Promotion p where (:active is null or p.active=:active) and (:search is null or lower(p.code) like lower(concat('%',:search,'%')) or lower(p.name) like lower(concat('%',:search,'%')))") Page<Promotion> findAdminPage(@Param("active") Boolean active,@Param("search") String search,Pageable pageable);
}