package com.blossom.ecommerce.product.application;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.product.domain.port.in.ProductCommand;
import com.blossom.ecommerce.product.domain.port.in.ProductUseCase;
import com.blossom.ecommerce.product.domain.port.in.ReservedStock;
import com.blossom.ecommerce.product.domain.port.in.StockRequest;
import com.blossom.ecommerce.product.domain.port.out.ProductRepositoryPort;
import com.blossom.ecommerce.shared.domain.DomainValidationException;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * MANDATORY: the row locks only make sense inside the caller's transaction (e.g. placing an order),
     * which also rolls back every stock change if anything later fails.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ReservedStock> reserveStock(List<StockRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new DomainValidationException("at least one product is required");
        }
        List<Long> ids = requests.stream().map(StockRequest::productId).toList();
        if (ids.stream().distinct().count() != ids.size()) {
            throw new DomainValidationException("each product can be requested only once");
        }

        Map<Long, Product> lockedById = productRepository.findActiveByIdsForUpdate(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Check and apply every change in memory first, so a failure never leaves a partial reservation
        List<ReservedStock> reservations = requests.stream().map(request -> {
            Product product = lockedById.get(request.productId());
            if (product == null) {
                throw new ResourceNotFoundException(RESOURCE, request.productId());
            }
            product.decreaseStock(request.quantity());
            return new ReservedStock(product.getId(), product.getName(), product.getPrice(), request.quantity());
        }).toList();

        lockedById.values().forEach(productRepository::save);
        return reservations;
    }

    private Product findOrThrow(Long id) {
        return productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }
}
