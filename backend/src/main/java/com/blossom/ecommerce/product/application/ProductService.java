package com.blossom.ecommerce.product.application;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.product.domain.port.in.ProductCommand;
import com.blossom.ecommerce.product.domain.port.in.ProductUseCase;
import com.blossom.ecommerce.product.domain.port.out.ProductRepositoryPort;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService implements ProductUseCase {

    private static final String RESOURCE = "Product";

    private final ProductRepositoryPort productRepository;

    public ProductService(ProductRepositoryPort productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product create(ProductCommand command) {
        Product product = Product.create(command.name(), command.description(), command.price(),
                command.stock(), command.category());
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return findOrThrow(id);
    }

    @Override
    public Product update(Long id, ProductCommand command) {
        Product product = findOrThrow(id);
        product.update(command.name(), command.description(), command.price(),
                command.stock(), command.category());
        return productRepository.save(product);
    }

    @Override
    public void delete(Long id) {
        Product product = findOrThrow(id);
        product.deactivate();
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Product> search(ProductFilter filter, PageQuery pageQuery) {
        return productRepository.searchActive(filter, pageQuery);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
