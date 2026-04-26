package com.app.exam.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class RedisExpirationListener extends KeyExpirationEventMessageListener {

    private final AttemptService attemptService;
    private static final String REDIS_PREFIX = "exam:session:";

    public RedisExpirationListener(RedisMessageListenerContainer listenerContainer, AttemptService attemptService) {
        super(listenerContainer);
        this.attemptService = attemptService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("Redis key expired: {}", expiredKey);

        if (expiredKey.startsWith(REDIS_PREFIX)) {
            try {
                String attemptIdStr = expiredKey.substring(REDIS_PREFIX.length());
                UUID attemptId = UUID.fromString(attemptIdStr);
                log.info("Auto-submitting attempt: {}", attemptId);
                attemptService.autoSubmitAttempt(attemptId);
            } catch (Exception e) {
                log.error("Error auto-submitting attempt for key: {}", expiredKey, e);
            }
        }
    }
}
