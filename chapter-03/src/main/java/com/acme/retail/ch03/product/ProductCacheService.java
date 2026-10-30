package com.acme.retail.ch03.product;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * A cache-aside layer for single-product reads.
 *
 * <p>The catalog problem was too many queries for one request. This is a
 * different problem: the same handful of products get requested thousands
 * of times a minute (best sellers, flash-sale items). Fixing the query
 * shape does not remove that repeated read pressure — a cache does.
 *
 * <p>The pattern here is "cache-aside": the application, not the database
 * or a framework annotation, is responsible for checking the cache, and
 * for filling it on a miss.
 * <pre>
 *   read:
 *     1. ask Redis for product:{id}
 *     2. hit  -&gt; return it, database untouched
 *     3. miss -&gt; load from Postgres, write it into Redis, return it
 *
 *   write (see {@link #evict(Long)}):
 *     1. update the product row in Postgres
 *     2. delete product:{id} from Redis so the next read repopulates it
 * </pre>
 */
@Service
public class ProductCacheService {

    /** How long a cached product is trusted before it must be reloaded. */
    static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private static final String KEY_PREFIX = "product:";

    private final ProductRepository productRepository;

    private final RedisTemplate<String, ProductView> redisTemplate;

    public ProductCacheService(ProductRepository productRepository,
                                RedisTemplate<String, ProductView> redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    public Optional<ProductView> getProductById(Long id) {
        String key = KEY_PREFIX + id;

        ProductView cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<Product> product = productRepository.findByIdWithCategory(id);
        if (product.isEmpty()) {
            return Optional.empty();
        }

        ProductView view = ProductView.from(product.get());
        redisTemplate.opsForValue().set(key, view, CACHE_TTL);
        return Optional.of(view);
    }

    /**
     * Call this after a product is updated (price change, restock, and so
     * on). Without it, Redis would keep serving the old value for up to
     * {@link #CACHE_TTL} even after the database changes — the classic
     * cache-aside staleness trade-off, discussed in Production Notes.
     */
    public void evict(Long id) {
        redisTemplate.delete(KEY_PREFIX + id);
    }
}
