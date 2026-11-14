package com.blossom.ecommerce.product.infrastructure.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByIdAndActiveTrue(Long id);

    /** SELECT ... ORDER BY id FOR UPDATE: PostgreSQL locks the rows in the sorted order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id in :ids and p.active = true order by p.id")
    List<ProductEntity> findActiveByIdInForUpdate(@Param("ids") Collection<Long> ids);
}
