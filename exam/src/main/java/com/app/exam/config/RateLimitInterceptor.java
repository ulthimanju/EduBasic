package com.app.exam.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Simple in-memory rate limiting: userId -> (timestamp -> count)
    // In production, use Redis-based bucket4j or similar.
    private final Map<String, UserRateInfo> userLimits = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : request.getRemoteAddr();
        
        long now = System.currentTimeMillis() / 1000 / 60; // Current minute
        
        UserRateInfo info = userLimits.computeIfAbsent(userId, k -> new UserRateInfo(now));
        
        if (info.minute == now) {
            if (info.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(429);
                response.getWriter().write("Too many requests");
                return false;
            }
        } else {
            info.minute = now;
            info.count.set(1);
        }
        
        return true;
    }

    private static class UserRateInfo {
        long minute;
        AtomicLong count;

        UserRateInfo(long minute) {
            this.minute = minute;
            this.count = new AtomicLong(0);
        }
    }
}
