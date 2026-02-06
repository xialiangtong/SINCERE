import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiter Solution
 * 
 * A reusable utility that enforces request limits per key
 * (e.g., per user, IP, API key).
 */
public class RateLimiterSolution {

    private final RateLimitCheckStrategy strategy;

    public RateLimiterSolution(RateLimitCheckStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Try to acquire 1 token for the given userId.
     */
    public CheckStatus tryAcquire(String userId) {
        return tryAcquire(userId, 1);
    }

    /**
     * Try to acquire the specified number of tokens for the given userId.
     */
    public CheckStatus tryAcquire(String userId, int tokenRequired) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (tokenRequired <= 0) {
            throw new IllegalArgumentException("tokenRequired must be > 0");
        }
        return strategy.tryAcquire(userId, tokenRequired);
    }

    // ==================== CheckStatus ====================

    /**
     * Result of a rate limit check.
     */
    public static class CheckStatus {
        private final boolean success;
        private final int remainingTokens;
        private final long retryAfterMs;

        private CheckStatus(boolean success, int remainingTokens, long retryAfterMs) {
            this.success = success;
            this.remainingTokens = remainingTokens;
            this.retryAfterMs = retryAfterMs;
        }

        public static CheckStatus allow(int remainingTokens) {
            return new CheckStatus(true, remainingTokens, 0);
        }

        public static CheckStatus reject(long retryAfterMs) {
            return new CheckStatus(false, 0, retryAfterMs);
        }

        public boolean isSuccess() {
            return success;
        }

        public int getRemainingTokens() {
            return remainingTokens;
        }

        public long getRetryAfterMs() {
            return retryAfterMs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CheckStatus)) return false;
            CheckStatus that = (CheckStatus) o;
            return success == that.success
                && remainingTokens == that.remainingTokens
                && retryAfterMs == that.retryAfterMs;
        }

        @Override
        public int hashCode() {
            return Objects.hash(success, remainingTokens, retryAfterMs);
        }

        @Override
        public String toString() {
            return success
                ? "CheckStatus{ALLOWED, remaining=" + remainingTokens + "}"
                : "CheckStatus{REJECTED, retryAfterMs=" + retryAfterMs + "}";
        }
    }

    // ==================== Strategy Interface ====================

    /**
     * Strategy interface for rate limit checking.
     */
    public interface RateLimitCheckStrategy {
        CheckStatus tryAcquire(String userId, int tokens);
    }

    // ==================== Token Bucket Strategy ====================

    /**
     * Token Bucket rate limiting strategy.
     * Tokens accumulate over time at a refill rate, up to a capacity.
     */
    public static class BucketTokenStrategy implements RateLimitCheckStrategy {

        private final int capacity;
        private final double tokenRefillRate; // tokens per millisecond (internal)
        private final long tokenAlivePeriod;
        private final ConcurrentHashMap<String, BucketState> map;

        public BucketTokenStrategy(int capacity, double tokensPerSecond, long tokenAlivePeriod) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be > 0");
            }
            if (tokensPerSecond <= 0) {
                throw new IllegalArgumentException("tokensPerSecond must be > 0");
            }
            if (tokenAlivePeriod <= 0) {
                throw new IllegalArgumentException("tokenAlivePeriod must be > 0");
            }
            this.capacity = capacity;
            this.tokenRefillRate = tokensPerSecond / 1000.0;
            this.tokenAlivePeriod = tokenAlivePeriod;
            this.map = new ConcurrentHashMap<>();
        }

        @Override
        public CheckStatus tryAcquire(String userId, int tokens) {
            long now = System.currentTimeMillis();
            CheckStatus[] result = new CheckStatus[1];

            map.compute(userId, (key, state) -> {
                if (state == null || (now - state.getLastTimestamp()) >= tokenAlivePeriod) {
                    // New user or expired: start with full capacity
                    state = new BucketState(capacity, now);
                } else {
                    // Refill tokens based on elapsed time
                    long elapsed = now - state.getLastTimestamp();
                    int refilled = (int) (elapsed * tokenRefillRate);
                    int newTokens = Math.min(capacity, state.getRemainTokens() + refilled);
                    state.setRemainTokens(newTokens);
                    state.setLastTimestamp(now);
                }

                // Check if enough tokens available
                if (state.getRemainTokens() >= tokens) {
                    state.setRemainTokens(state.getRemainTokens() - tokens);
                    result[0] = CheckStatus.allow(state.getRemainTokens());
                } else {
                    int needed = tokens - state.getRemainTokens();
                    long retryAfterMs = (long) Math.ceil(needed / tokenRefillRate);
                    result[0] = CheckStatus.reject(retryAfterMs);
                }

                return state;
            });

            return result[0];
        }

        /**
         * Tracks the bucket state for a single key.
         */
        public static class BucketState {
            private int remainTokens;
            private long lastTimestamp;

            public BucketState(int remainTokens, long lastTimestamp) {
                this.remainTokens = remainTokens;
                this.lastTimestamp = lastTimestamp;
            }

            public int getRemainTokens() {
                return remainTokens;
            }

            public void setRemainTokens(int remainTokens) {
                this.remainTokens = remainTokens;
            }

            public long getLastTimestamp() {
                return lastTimestamp;
            }

            public void setLastTimestamp(long lastTimestamp) {
                this.lastTimestamp = lastTimestamp;
            }
        }
    }

    // ==================== Fixed Window Strategy ====================

    /**
     * Fixed Window rate limiting strategy.
     * Requests are counted within fixed time windows of a given width.
     * Once the window expires, the count resets.
     */
    public static class FixedWindowStrategy implements RateLimitCheckStrategy {

        private final int maxTokens;
        private final long windowWidth;
        private final ConcurrentHashMap<String, WindowState> map;

        public FixedWindowStrategy(int maxTokens, long windowWidth) {
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("maxTokens must be > 0");
            }
            if (windowWidth <= 0) {
                throw new IllegalArgumentException("windowWidth must be > 0");
            }
            this.maxTokens = maxTokens;
            this.windowWidth = windowWidth;
            this.map = new ConcurrentHashMap<>();
        }

        @Override
        public CheckStatus tryAcquire(String userId, int tokens) {
            long now = System.currentTimeMillis();
            CheckStatus[] result = new CheckStatus[1];

            map.compute(userId, (key, state) -> {
                if (state == null || (now - state.getStartTime()) >= windowWidth) {
                    // New user or window expired: start a new window
                    state = new WindowState(now, 0);
                }

                if (state.getUsedTokens() + tokens <= maxTokens) {
                    state.setUsedTokens(state.getUsedTokens() + tokens);
                    int remaining = maxTokens - (int) state.getUsedTokens();
                    result[0] = CheckStatus.allow(remaining);
                } else {
                    long retryAfterMs = windowWidth - (now - state.getStartTime());
                    result[0] = CheckStatus.reject(retryAfterMs);
                }

                return state;
            });

            return result[0];
        }

        /**
         * Tracks the window state for a single key.
         */
        public static class WindowState {
            private long startTime;
            private long usedTokens;

            public WindowState(long startTime, long usedTokens) {
                this.startTime = startTime;
                this.usedTokens = usedTokens;
            }

            public long getStartTime() {
                return startTime;
            }

            public void setStartTime(long startTime) {
                this.startTime = startTime;
            }

            public long getUsedTokens() {
                return usedTokens;
            }

            public void setUsedTokens(long usedTokens) {
                this.usedTokens = usedTokens;
            }
        }
    }
}
