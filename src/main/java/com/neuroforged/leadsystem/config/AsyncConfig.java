package com.neuroforged.leadsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Bounded executor for {@code @Async} work (outbound webhooks, KB fetch, notifications).
 *
 * <p>Without an explicitly-named {@link TaskExecutor}, {@code @EnableAsync} falls back to
 * {@code SimpleAsyncTaskExecutor}, which spawns a brand-new unpooled thread per invocation
 * with no upper bound — a burst of leads would create a thread per webhook delivery. This
 * pool caps concurrency and applies backpressure via a bounded queue, falling back to
 * running the task on the caller thread when saturated rather than dropping it.
 */
@Configuration
public class AsyncConfig {

    @Bean("backgroundTaskExecutor")
    public TaskExecutor backgroundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-bg-");
        // When the pool + queue are saturated, run on the calling thread so the work
        // is not silently lost (still bounds total in-flight work via backpressure).
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
