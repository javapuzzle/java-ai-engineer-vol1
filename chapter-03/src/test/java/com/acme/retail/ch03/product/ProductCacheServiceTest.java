package com.acme.retail.ch03.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.acme.retail.ch03.category.Category;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit tests for the cache-aside pattern: no Spring context, no real
 * Redis server, no database. {@link RedisTemplate} and its
 * {@link ValueOperations} are mocked, which is enough to verify the two
 * behaviors that matter: read-through on a miss, and skipping the database
 * entirely on a hit.
 */
class ProductCacheServiceTest {

    private ProductRepository productRepository;

    private RedisTemplate<String, ProductView> redisTemplate;

    private ValueOperations<String, ProductView> valueOperations;

    private ProductCacheService cacheService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        productRepository = mock(ProductRepository.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService = new ProductCacheService(productRepository, redisTemplate);
    }

    @Test
    void cacheHitNeverTouchesTheDatabase() {
        ProductView cached = new ProductView(1L, "Trail Runner", "SKU-1", 12999, 42, "Footwear");
        when(valueOperations.get("product:1")).thenReturn(cached);

        Optional<ProductView> result = cacheService.getProductById(1L);

        assertThat(result).contains(cached);
        verifyNoInteractions(productRepository);
    }

    @Test
    void cacheMissLoadsFromTheDatabaseAndPopulatesTheCache() {
        when(valueOperations.get("product:2")).thenReturn(null);

        Category category = new Category("Footwear");
        Product product = new Product(
                "Trail Runner", "Lightweight trail running shoe", 12999, "SKU-2", 10, category);
        ReflectionTestUtils.setField(product, "id", 2L);

        when(productRepository.findByIdWithCategory(2L)).thenReturn(Optional.of(product));

        Optional<ProductView> result = cacheService.getProductById(2L);

        assertThat(result).isPresent();
        assertThat(result.get().categoryName()).isEqualTo("Footwear");
        verify(valueOperations).set(eq("product:2"), any(ProductView.class), eq(ProductCacheService.CACHE_TTL));
    }

    @Test
    void missingProductReturnsEmptyAndCachesNothing() {
        when(valueOperations.get("product:99")).thenReturn(null);
        when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

        Optional<ProductView> result = cacheService.getProductById(99L);

        assertThat(result).isEmpty();
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void evictDeletesTheCacheKeySoTheNextReadRepopulatesIt() {
        cacheService.evict(2L);

        verify(redisTemplate).delete("product:2");
    }
}
