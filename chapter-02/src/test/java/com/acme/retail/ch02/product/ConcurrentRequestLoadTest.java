package com.acme.retail.ch02.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.retail.ch02.diagnostics.ThreadInfoController.ThreadInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * This is Sarah's "load test" turned into an automated regression test.
 *
 * <p>It fires many concurrent requests at the product endpoint, where
 * every request blocks for roughly 80ms inside {@link ProductRepository}
 * (see {@link ProductRepository#SIMULATED_DATABASE_LATENCY_MS}). It then
 * checks two things that map directly to the chapter's story:
 *
 * <ol>
 *   <li>All requests succeed and the whole batch finishes in roughly the
 *       time ONE request takes, not the sum of all of them — proving the
 *       blocking calls are running concurrently, not queued one behind
 *       another.</li>
 *   <li>The server actually served the request on a virtual thread, not
 *       a platform thread from a fixed-size pool — this is what makes
 *       (1) possible without an enormous thread pool.</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ConcurrentRequestLoadTest {

    private static final int CONCURRENT_REQUESTS = 150;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void manyConcurrentRequestsCompleteFasterThanRunningThemOneAtATime() throws Exception {
        long sequentialWorstCaseMs = CONCURRENT_REQUESTS * ProductRepository.SIMULATED_DATABASE_LATENCY_MS;

        try (ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            Instant start = Instant.now();

            List<Future<ResponseEntity<Product>>> futures = IntStream.range(0, CONCURRENT_REQUESTS)
                    .mapToObj(i -> clientExecutor.submit(() ->
                            restTemplate.getForEntity("/api/products/1", Product.class)))
                    .toList();

            for (Future<ResponseEntity<Product>> future : futures) {
                ResponseEntity<Product> response = future.get();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }

            Duration elapsed = Duration.between(start, Instant.now());

            // Illustrative bound, not a strict benchmark: well under the
            // "one request after another" worst case, which is the whole
            // point of this test.
            assertThat(elapsed.toMillis()).isLessThan(sequentialWorstCaseMs / 2);
        }
    }

    @Test
    void requestsAreServedByVirtualThreadsWhenTheFeatureIsEnabled() {
        ResponseEntity<ThreadInfo> response =
                restTemplate.getForEntity("/api/diagnostics/thread-info", ThreadInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // application.yml sets spring.threads.virtual.enabled: true, so
        // Tomcat hands each request to a virtual thread instead of a
        // platform thread from its fixed-size pool.
        assertThat(response.getBody().virtual()).isTrue();
    }
}
