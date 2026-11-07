package com.darchive.ai;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/** Protects your API bill: 10 AI calls/minute and 150/day per user (in-memory, resets on restart). */
@Component
public class AiRateLimiter {

    private static final int PER_MINUTE = 10;
    private static final int PER_DAY = 150;

    private final ConcurrentHashMap<Long, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public void check(long userId) {
        Deque<Instant> q = hits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            Instant now = Instant.now();
            while (!q.isEmpty() && q.peekFirst().isBefore(now.minus(Duration.ofDays(1)))) q.pollFirst();
            long lastMinute = q.stream().filter(t -> t.isAfter(now.minusSeconds(60))).count();
            if (lastMinute >= PER_MINUTE)
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Slow down - try again in a minute");
            if (q.size() >= PER_DAY)
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily AI limit reached, try again tomorrow");
            q.addLast(now);
        }
    }
}
