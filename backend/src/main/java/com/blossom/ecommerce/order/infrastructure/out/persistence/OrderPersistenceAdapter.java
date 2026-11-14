package com.blossom.ecommerce.order.infrastructure.out.persistence;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.port.out.OrderRepositoryPort;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * The history page is read with two queries whatever its size (ids, then orders with their items)
 * instead of one extra query per order to load its items (N+1).
 */
@Component
class OrderPersistenceAdapter implements OrderRepositoryPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final OrderJpaRepository jpaRepository;

    OrderPersistenceAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        if (order.getId() != null) {
            throw new IllegalArgumentException("Orders are immutable: only new orders can be saved");
        }
        return OrderPersistenceMapper.toDomain(jpaRepository.save(OrderPersistenceMapper.toNewEntity(order)));
    }

    @Override
    public PageResult<Order> findByCustomerId(Long customerId, PageQuery pageQuery) {
        Page<Long> ids = jpaRepository.findIdsByCustomerId(customerId,
                PageRequest.of(pageQuery.page(), pageQuery.size(), NEWEST_FIRST));
        List<Order> orders = ids.isEmpty()
                ? List.of()
                : jpaRepository.findByIdIn(ids.getContent(), NEWEST_FIRST).stream()
                        .map(OrderPersistenceMapper::toDomain)
                        .toList();
        return new PageResult<>(orders, ids.getNumber(), ids.getSize(), ids.getTotalElements(), ids.getTotalPages());
    }

    @Override
    public Optional<Order> findByIdAndCustomerId(Long id, Long customerId) {
        return jpaRepository.findByIdAndCustomerId(id, customerId).map(OrderPersistenceMapper::toDomain);
    }
}
