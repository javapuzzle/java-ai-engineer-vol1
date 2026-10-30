package com.acme.retail.ch03.product;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCatalogService catalogService;

    private final ProductCacheService cacheService;

    public ProductController(ProductCatalogService catalogService, ProductCacheService cacheService) {
        this.catalogService = catalogService;
        this.cacheService = cacheService;
    }

    /** The catalog page. Uses the JOIN FETCH query — one SQL statement, not N+1. */
    @GetMapping
    public List<ProductView> list() {
        return catalogService.catalog();
    }

    /** A single product page. Cache-aside: Redis first, Postgres on a miss. */
    @GetMapping("/{id}")
    public ResponseEntity<ProductView> getById(@PathVariable Long id) {
        Optional<ProductView> product = cacheService.getProductById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
