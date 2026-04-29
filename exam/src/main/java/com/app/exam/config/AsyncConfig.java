package com.app.exam.config;

import io.micrometer.context.ContextSnapshot;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "geminiExecutor")
    public AsyncTaskExecutor geminiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Core/max pool sizes tuned for burst traffic (Gemini AI calls)
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("edubasic-gemini-");
        
        executor.setTaskDecorator(new ContextPropagationTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return geminiExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
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
