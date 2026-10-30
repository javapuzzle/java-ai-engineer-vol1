package com.acme.retail.ch02.diagnostics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tiny stand-in for the thread dump / profiler Sarah studies in this
 * chapter.
 *
 * <p>A real investigation would use {@code jstack}, Java Flight Recorder,
 * or an APM tool. This endpoint exists so the tests in this module can
 * demonstrate, in a few lines of code, the exact fact that matters for
 * the story: which kind of thread served the request, and whether it was
 * a cheap virtual thread or an expensive platform thread from the fixed
 * Tomcat pool.
 */
@RestController
public class ThreadInfoController {

    @GetMapping("/api/diagnostics/thread-info")
    public ThreadInfo threadInfo() {
        Thread current = Thread.currentThread();
        return new ThreadInfo(current.getName(), current.isVirtual(), current.threadId());
    }

    public record ThreadInfo(String threadName, boolean virtual, long threadId) {
    }
}
