package com.blossom.ecommerce.product.application;

import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductCategory;
import com.blossom.ecommerce.product.domain.ProductFilter;
import com.blossom.ecommerce.product.domain.port.in.ProductCommand;
import com.blossom.ecommerce.product.domain.port.out.ProductRepositoryPort;
import com.blossom.ecommerce.shared.domain.DomainValidationException;
import com.blossom.ecommerce.shared.domain.PageQuery;
import com.blossom.ecommerce.shared.domain.PageResult;
import com.blossom.ecommerce.shared.domain.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.blossom.ecommerce.product.ProductFixtures.existingProduct;
import static com.blossom.ecommerce.product.ProductFixtures.validCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createBuildsActiveProductAndSavesIt() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product toSave = inv.getArgument(0);
            return Product.restore(1L, toSave.getName(), toSave.getDescription(), toSave.getPrice(),
                    toSave.getStock(), toSave.getCategory(), toSave.isActive());
        });

        Product created = productService.create(validCommand());

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getName()).isEqualTo("Desk Lamp");
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void createDoesNotSaveWhenCommandBreaksInvariants() {
        ProductCommand invalid = new ProductCommand("Lamp", null, BigDecimal.ZERO, 1, ProductCategory.HOME);

        assertThatThrownBy(() -> productService.create(invalid)).isInstanceOf(DomainValidationException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsActiveProduct() {
        when(productRepository.findActiveById(1L)).thenReturn(Optional.of(existingProduct(1L)));

        assertThat(productService.getById(1L).getId()).isEqualTo(1L);
    }

    @Test
    void getByIdFailsWhenProductDoesNotExist() {
        when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id 99 was not found");
    }

    @Test
    void updateAppliesChangesAndSaves() {
        when(productRepository.findActiveById(1L)).thenReturn(Optional.of(existingProduct(1L)));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductCommand command = new ProductCommand("New name", null, new BigDecimal("99.90"), 3,
                ProductCategory.ELECTRONICS);

        Product updated = productService.update(1L, command);

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getPrice()).isEqualByComparingTo("99.90");
        assertThat(updated.getCategory()).isEqualTo(ProductCategory.ELECTRONICS);
    }

    @Test
    void updateFailsWhenProductDoesNotExist() {
        when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(99L, validCommand()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteDeactivatesInsteadOfRemoving() {
        when(productRepository.findActiveById(1L)).thenReturn(Optional.of(existingProduct(1L)));
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

        productService.delete(1L);

        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void deleteFailsWhenProductDoesNotExist() {
        when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchDelegatesToRepository() {
        ProductFilter filter = new ProductFilter("lamp", ProductCategory.HOME, null, null);
        PageQuery pageQuery = new PageQuery(0, 10);
        PageResult<Product> page = new PageResult<>(List.of(existingProduct(1L)), 0, 10, 1, 1);
        when(productRepository.searchActive(filter, pageQuery)).thenReturn(page);

        assertThat(productService.search(filter, pageQuery)).isSameAs(page);
    }
}
