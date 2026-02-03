import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiter Implementation
 * 
 * Provides both Fixed Window and Token Bucket rate limiting strategies.
 */
public class RateLimiter {

    /**
     * Decision result from a rate limit check.
     */
    public static class Decision {
        private final boolean allowed;
        private final long retryAfterMs;
        private final int remaining;

        private Decision(boolean allowed, long retryAfterMs, int remaining) {
            this.allowed = allowed;
            this.retryAfterMs = retryAfterMs;
            this.remaining = remaining;
        }

        public static Decision allow(int remaining) {
            return new Decision(true, 0, remaining);
        }

        public static Decision reject(long retryAfterMs) {
            return new Decision(false, retryAfterMs, 0);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRetryAfterMs() {
            return retryAfterMs;
        }

        public int getRemaining() {
            return remaining;
        }

        @Override
        public String toString() {
            if (allowed) {
                return "Decision{allowed=true, remaining=" + remaining + "}";
            } else {
                return "Decision{allowed=false, retryAfterMs=" + retryAfterMs + "}";
            }
        }
    }

    /**
     * Rate limiting strategy interface.
     */
    public interface Strategy {
        Decision allow(String key);
        Decision tryAcquire(String key, int permits);
    }

    /**
     * Fixed Window Rate Limiter Strategy.
     * Counts requests in discrete time windows.
     */
    public static class FixedWindowStrategy implements Strategy {
        private final int maxRequests;
        private final long windowSizeMs;
        private final Map<String, WindowState> states = new ConcurrentHashMap<>();

        private static class WindowState {
            long windowStart;
            int count;

            WindowState(long windowStart) {
                this.windowStart = windowStart;
                this.count = 0;
            }
        }

        public FixedWindowStrategy(int maxRequests, long windowSizeMs) {
            this.maxRequests = maxRequests;
            this.windowSizeMs = windowSizeMs;
        }

        @Override
        public Decision allow(String key) {
            return tryAcquire(key, 1);
        }

        @Override
        public synchronized Decision tryAcquire(String key, int permits) {
            long now = System.currentTimeMillis();
            
            WindowState state = states.computeIfAbsent(key, k -> new WindowState(now));

            // Check if we need to reset the window
            if (now >= state.windowStart + windowSizeMs) {
                state.windowStart = (now / windowSizeMs) * windowSizeMs;
                state.count = 0;
            }

            // Check if request is allowed
            if (state.count + permits <= maxRequests) {
                state.count += permits;
                return Decision.allow(maxRequests - state.count);
            } else {
                long retryAfter = (state.windowStart + windowSizeMs) - now;
                return Decision.reject(retryAfter);
            }
        }
    }

    /**
     * Token Bucket Rate Limiter Strategy.
     * Tokens accumulate over time up to a capacity.
     */
    public static class TokenBucketStrategy implements Strategy {
        private final int capacity;
        private final double refillRatePerMs;
        private final Map<String, BucketState> states = new ConcurrentHashMap<>();

        private static class BucketState {
            double tokens;
            long lastRefillMs;

            BucketState(double tokens, long lastRefillMs) {
                this.tokens = tokens;
                this.lastRefillMs = lastRefillMs;
            }
        }

        /**
         * Creates a Token Bucket strategy.
         * @param capacity Maximum tokens in the bucket
         * @param refillTokens Number of tokens to refill
         * @param refillPeriodMs Time period for refill in milliseconds
         */
        public TokenBucketStrategy(int capacity, int refillTokens, long refillPeriodMs) {
            this.capacity = capacity;
            this.refillRatePerMs = (double) refillTokens / refillPeriodMs;
        }

        @Override
        public Decision allow(String key) {
            return tryAcquire(key, 1);
        }

        @Override
        public synchronized Decision tryAcquire(String key, int permits) {
            long now = System.currentTimeMillis();
            
            BucketState state = states.computeIfAbsent(key, 
                k -> new BucketState(capacity, now));

            // Refill tokens based on elapsed time
            long elapsed = now - state.lastRefillMs;
            state.tokens = Math.min(capacity, state.tokens + elapsed * refillRatePerMs);
            state.lastRefillMs = now;

            // Check if request is allowed
            if (state.tokens >= permits) {
                state.tokens -= permits;
                return Decision.allow((int) state.tokens);
            } else {
                double needed = permits - state.tokens;
                long retryAfter = (long) Math.ceil(needed / refillRatePerMs);
                return Decision.reject(retryAfter);
            }
        }
    }

    // Instance fields
    private final Strategy strategy;

    public RateLimiter(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Check if a request is allowed for the given key.
     */
    public Decision allow(String key) {
        return strategy.allow(key);
    }

    /**
     * Try to acquire a number of permits for the given key.
     */
    public Decision tryAcquire(String key, int permits) {
        return strategy.tryAcquire(key, permits);
    }

    // Factory methods for convenience
    public static RateLimiter fixedWindow(int maxRequests, long windowSizeMs) {
        return new RateLimiter(new FixedWindowStrategy(maxRequests, windowSizeMs));
    }

    public static RateLimiter tokenBucket(int capacity, int refillTokens, long refillPeriodMs) {
        return new RateLimiter(new TokenBucketStrategy(capacity, refillTokens, refillPeriodMs));
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Fixed Window Rate Limiter Demo ===");
        // 5 requests per 2 seconds
        RateLimiter fixedLimiter = RateLimiter.fixedWindow(5, 2000);

        for (int i = 1; i <= 7; i++) {
            Decision decision = fixedLimiter.allow("user1");
            System.out.println("Request " + i + ": " + decision);
        }

        System.out.println("\n=== Token Bucket Rate Limiter Demo ===");
        // Capacity 5, refill 5 tokens per 2 seconds
        RateLimiter tokenLimiter = RateLimiter.tokenBucket(5, 5, 2000);

        for (int i = 1; i <= 7; i++) {
            Decision decision = tokenLimiter.allow("user2");
            System.out.println("Request " + i + ": " + decision);
        }

        System.out.println("\nWaiting 1 second for token refill...");
        Thread.sleep(1000);

        Decision afterWait = tokenLimiter.allow("user2");
        System.out.println("After 1s wait: " + afterWait);
    }
}
