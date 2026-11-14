package com.blossom.ecommerce.product.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findByIdAndActiveTrue(Long id);
}
