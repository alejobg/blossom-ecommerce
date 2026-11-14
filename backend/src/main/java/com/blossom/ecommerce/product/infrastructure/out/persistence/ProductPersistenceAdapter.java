package com.blossom.ecommerce.product.infrastructure.out.persistence;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.product.domain.port.out.ProductRepositoryPort;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
class ProductPersistenceAdapter implements ProductRepositoryPort {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id"));

    private final ProductJpaRepository jpaRepository;

    ProductPersistenceAdapter(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity;
        if (product.getId() == null) {
            entity = ProductPersistenceMapper.toNewEntity(product);
        } else {
            entity = jpaRepository.findById(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", product.getId()));
            ProductPersistenceMapper.copyToEntity(product, entity);
        }
        return ProductPersistenceMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Product> findActiveById(Long id) {
        return jpaRepository.findByIdAndActiveTrue(id).map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Product> searchActive(ProductFilter filter, PageQuery pageQuery) {
        PageRequest pageRequest = PageRequest.of(pageQuery.page(), pageQuery.size(), DEFAULT_SORT);
        Page<ProductEntity> page = jpaRepository.findAll(ProductSpecifications.activeMatching(filter), pageRequest);
        return new PageResult<>(
                page.getContent().stream().map(ProductPersistenceMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    public List<Product> findActiveByIdsForUpdate(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findActiveByIdInForUpdate(ids).stream().map(ProductPersistenceMapper::toDomain).toList();
    }
}
