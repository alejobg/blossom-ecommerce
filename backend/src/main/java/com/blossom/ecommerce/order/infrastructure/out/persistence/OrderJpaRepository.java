package com.blossom.ecommerce.order.infrastructure.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    /** Step 1 of the history: page over ids only (paging a collection fetch join would happen in memory). */
    @Query(value = "select o.id from OrderEntity o where o.customerId = :customerId",
            countQuery = "select count(o) from OrderEntity o where o.customerId = :customerId")
    Page<Long> findIdsByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    /** Step 2: load that page's orders together with their items in a single query. */
    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findByIdIn(Collection<Long> ids, Sort sort);

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findByIdAndCustomerId(Long id, Long customerId);
}
