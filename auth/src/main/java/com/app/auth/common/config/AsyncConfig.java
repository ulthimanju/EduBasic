package com.app.auth.common.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import io.micrometer.context.ContextSnapshot;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "authExecutor")
    public AsyncTaskExecutor authExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("edubasic-auth-");
        
        // Propagate tracing/MDC context
        executor.setTaskDecorator(new ContextPropagationTaskDecorator());
        
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return authExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // Prevents async exceptions from silently disappearing
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    private static class ContextPropagationTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            ContextSnapshot snapshot = ContextSnapshot.captureAll();
            return () -> {
                try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
                    runnable.run();
                } catch (Exception e) {
                    throw new RuntimeException("Exception in async task", e);
                }
            };
        }
    }
}
