package com.acme.retail.ch01.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer test using {@code @WebMvcTest}.
 *
 * <p>{@code @WebMvcTest} starts only the web layer of Spring (controllers,
 * JSON serialization, exception handlers) -- it does not start a real
 * database. {@link ProductService} is replaced with a {@code @MockBean}, so
 * this test proves that HTTP requests are routed correctly and that
 * {@link Product} objects are serialized to the JSON shape callers expect,
 * without depending on the database being up.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void getProduct_returns200WithProductJson_whenProductExists() throws Exception {
        Product trailShoe = new Product(
                "Trail Running Shoe", "Grippy trail shoe for wet terrain",
                8999, "SKU-1001", 42, "Footwear");
        when(productService.getProduct(1L)).thenReturn(trailShoe);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Trail Running Shoe"))
                .andExpect(jsonPath("$.sku").value("SKU-1001"))
                .andExpect(jsonPath("$.priceCents").value(8999));
    }

    @Test
    void getProduct_returns404_whenProductDoesNotExist() throws Exception {
        when(productService.getProduct(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found: 999"));
    }

    @Test
    void listProducts_returns200WithJsonArray() throws Exception {
        Product productA = new Product("A", "desc", 100, "SKU-A", 1, "Cat");
        when(productService.listProducts()).thenReturn(List.of(productA));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-A"));
    }

    @Test
    void createProduct_returns201_whenRequestBodyIsValid() throws Exception {
        Product newProduct = new Product(
                "Insulated Water Bottle", "Keeps drinks cold for 24 hours",
                2499, "SKU-2002", 100, "Outdoor");
        when(productService.createProduct(org.mockito.ArgumentMatchers.any(Product.class)))
                .thenReturn(newProduct);

        String requestBody = """
                {
                  "name": "Insulated Water Bottle",
                  "description": "Keeps drinks cold for 24 hours",
                  "priceCents": 2499,
                  "sku": "SKU-2002",
                  "stockQuantity": 100,
                  "category": "Outdoor"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-2002"));
    }
}
