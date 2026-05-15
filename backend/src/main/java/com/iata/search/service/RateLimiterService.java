package com.iata.search.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final Map<String, UserBucket> buckets = new ConcurrentHashMap<>();
    private final int maxRequests = 10;
    private final Duration window = Duration.ofMinutes(1);

    public boolean isAllowed(String userId) {
        UserBucket bucket = buckets.computeIfAbsent(userId, k -> new UserBucket());
        return bucket.tryConsume();
    }

    private class UserBucket {
        private int count;
        private Instant windowStart;

        UserBucket() {
            this.count = 0;
            this.windowStart = Instant.now();
        }

        synchronized boolean tryConsume() {
            Instant now = Instant.now();
            if (Duration.between(windowStart, now).compareTo(window) >= 0) {
                count = 0;
                windowStart = now;
            }
            if (count >= maxRequests) {
                log.warn("Rate limit exceeded for user");
                return false;
            }
            count++;
            return true;
        }
    }
}
