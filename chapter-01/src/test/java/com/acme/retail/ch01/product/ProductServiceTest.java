package com.acme.retail.ch01.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for the service layer.
 *
 * <p>This test never starts Spring, never touches a database, and never
 * makes an HTTP call. {@link ProductRepository} is replaced with a Mockito
 * mock, so the test is only exercising {@link ProductService}'s own logic
 * -- exactly the kind of fast, isolated test a layered architecture makes
 * possible.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProduct_returnsProduct_whenItExists() {
        Product trailShoe = new Product(
                "Trail Running Shoe", "Grippy trail shoe for wet terrain",
                8999, "SKU-1001", 42, "Footwear");
        when(productRepository.findById(1L)).thenReturn(Optional.of(trailShoe));

        Product result = productService.getProduct(1L);

        assertThat(result.getName()).isEqualTo("Trail Running Shoe");
        assertThat(result.getSku()).isEqualTo("SKU-1001");
    }

    @Test
    void getProduct_throwsProductNotFoundException_whenIdDoesNotExist() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void createProduct_delegatesToRepositorySave() {
        Product newProduct = new Product(
                "Insulated Water Bottle", "Keeps drinks cold for 24 hours",
                2499, "SKU-2002", 100, "Outdoor");
        when(productRepository.save(newProduct)).thenReturn(newProduct);

        Product saved = productService.createProduct(newProduct);

        assertThat(saved.getSku()).isEqualTo("SKU-2002");
        verify(productRepository).save(newProduct);
    }

    @Test
    void listProducts_returnsEveryProductFromRepository() {
        Product productA = new Product("A", "desc", 100, "SKU-A", 1, "Cat");
        Product productB = new Product("B", "desc", 200, "SKU-B", 2, "Cat");
        when(productRepository.findAll()).thenReturn(List.of(productA, productB));

        List<Product> result = productService.listProducts();

        assertThat(result).hasSize(2);
    }
}
