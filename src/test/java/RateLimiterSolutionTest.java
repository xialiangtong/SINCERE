import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimiterSolution Tests")
public class RateLimiterSolutionTest {

    // ==================== CheckStatus Tests ====================

    @Nested
    @DisplayName("CheckStatus Tests")
    class CheckStatusTests {

        @Test
        @DisplayName("allow() creates success status with remaining tokens")
        void testAllowCreatesSuccessStatus() {
            RateLimiterSolution.CheckStatus status = RateLimiterSolution.CheckStatus.allow(5);
            assertTrue(status.isSuccess());
            assertEquals(5, status.getRemainingTokens());
            assertEquals(0, status.getRetryAfterMs());
        }

        @Test
        @DisplayName("allow() with zero remaining tokens")
        void testAllowWithZeroRemaining() {
            RateLimiterSolution.CheckStatus status = RateLimiterSolution.CheckStatus.allow(0);
            assertTrue(status.isSuccess());
            assertEquals(0, status.getRemainingTokens());
            assertEquals(0, status.getRetryAfterMs());
        }

        @Test
        @DisplayName("reject() creates failure status with retryAfterMs")
        void testRejectCreatesFailureStatus() {
            RateLimiterSolution.CheckStatus status = RateLimiterSolution.CheckStatus.reject(1000);
            assertFalse(status.isSuccess());
            assertEquals(0, status.getRemainingTokens());
            assertEquals(1000, status.getRetryAfterMs());
        }

        @Test
        @DisplayName("toString() for allowed status")
        void testToStringAllowed() {
            RateLimiterSolution.CheckStatus status = RateLimiterSolution.CheckStatus.allow(3);
            assertEquals("CheckStatus{ALLOWED, remaining=3}", status.toString());
        }

        @Test
        @DisplayName("toString() for rejected status")
        void testToStringRejected() {
            RateLimiterSolution.CheckStatus status = RateLimiterSolution.CheckStatus.reject(500);
            assertEquals("CheckStatus{REJECTED, retryAfterMs=500}", status.toString());
        }
    }

    // ==================== BucketState Tests ====================

    @Nested
    @DisplayName("BucketState Tests")
    class BucketStateTests {

        @Test
        @DisplayName("constructor sets initial values")
        void testConstructorSetsValues() {
            RateLimiterSolution.BucketTokenStrategy.BucketState state =
                new RateLimiterSolution.BucketTokenStrategy.BucketState(10, 12345L);
            assertEquals(10, state.getRemainTokens());
            assertEquals(12345L, state.getLastTimestamp());
        }

        @Test
        @DisplayName("setRemainTokens updates tokens")
        void testSetRemainTokens() {
            RateLimiterSolution.BucketTokenStrategy.BucketState state =
                new RateLimiterSolution.BucketTokenStrategy.BucketState(10, 0L);
            state.setRemainTokens(5);
            assertEquals(5, state.getRemainTokens());
        }

        @Test
        @DisplayName("setLastTimestamp updates timestamp")
        void testSetLastTimestamp() {
            RateLimiterSolution.BucketTokenStrategy.BucketState state =
                new RateLimiterSolution.BucketTokenStrategy.BucketState(10, 0L);
            state.setLastTimestamp(99999L);
            assertEquals(99999L, state.getLastTimestamp());
        }
    }

    // ==================== BucketTokenStrategy Tests ====================

    @Nested
    @DisplayName("BucketTokenStrategy Tests")
    class BucketTokenStrategyTests {

        // capacity=10, tokensPerSecond=10, alivePeriod=5000ms
        private RateLimiterSolution.BucketTokenStrategy createStrategy() {
            return new RateLimiterSolution.BucketTokenStrategy(10, 10, 5000);
        }

        @Test
        @DisplayName("new user gets full capacity - single token")
        void testNewUserGetsFullCapacity() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 1);
            assertTrue(status.isSuccess());
            assertEquals(9, status.getRemainingTokens());
        }

        @Test
        @DisplayName("new user gets full capacity - multiple tokens")
        void testNewUserMultipleTokens() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 5);
            assertTrue(status.isSuccess());
            assertEquals(5, status.getRemainingTokens());
        }

        @Test
        @DisplayName("new user requesting all tokens succeeds with 0 remaining")
        void testNewUserRequestAllTokens() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 10);
            assertTrue(status.isSuccess());
            assertEquals(0, status.getRemainingTokens());
        }

        @Test
        @DisplayName("new user requesting more than capacity is rejected")
        void testNewUserExceedsCapacity() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 11);
            assertFalse(status.isSuccess());
            assertTrue(status.getRetryAfterMs() > 0);
        }

        @Test
        @DisplayName("existing user - enough tokens available")
        void testExistingUserEnoughTokens() {
            var strategy = createStrategy();
            // First request: 10 - 3 = 7 remaining
            strategy.tryAcquire("user1", 3);
            // Second request immediately: at least 7 tokens remain (plus tiny refill)
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 5);
            assertTrue(status.isSuccess());
        }

        @Test
        @DisplayName("existing user - not enough tokens, gets rejected")
        void testExistingUserNotEnoughTokens() {
            var strategy = createStrategy();
            // Drain all tokens
            strategy.tryAcquire("user1", 10);
            // Immediately request more - no time to refill
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 5);
            assertFalse(status.isSuccess());
            assertTrue(status.getRetryAfterMs() > 0);
        }

        @Test
        @DisplayName("retryAfterMs is correct when rejected")
        void testRetryAfterMsCalculation() {
            // capacity=5, tokensPerSecond=5, alivePeriod=10000ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 5, 10000);
            // Drain all 5 tokens
            strategy.tryAcquire("user1", 5);
            // Request 3 tokens immediately - need 3 tokens, rate=0.005/ms
            // retryAfterMs = ceil(3 / 0.005) = 600ms
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 3);
            assertFalse(status.isSuccess());
            assertEquals(600, status.getRetryAfterMs());
        }

        @Test
        @DisplayName("different users have independent buckets")
        void testDifferentUsersIndependentBuckets() {
            var strategy = createStrategy();
            // Drain user1
            strategy.tryAcquire("user1", 10);
            // user2 should still have full capacity
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user2", 5);
            assertTrue(status.isSuccess());
            assertEquals(5, status.getRemainingTokens());
        }

        @Test
        @DisplayName("tokens refill after waiting")
        void testTokensRefillAfterWaiting() throws InterruptedException {
            // capacity=5, tokensPerSecond=10, alivePeriod=10000ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 10, 10000);
            // Drain all 5 tokens
            strategy.tryAcquire("user1", 5);
            // Wait 300ms → refill = (int)(300 * 0.01) = 3 tokens
            Thread.sleep(300);
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 2);
            assertTrue(status.isSuccess());
        }

        @Test
        @DisplayName("refill does not exceed capacity")
        void testRefillCappedAtCapacity() throws InterruptedException {
            // capacity=5, tokensPerSecond=10, alivePeriod=10000ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 10, 10000);
            // Use 2 tokens: 5 - 2 = 3 remaining
            strategy.tryAcquire("user1", 2);
            // Wait 500ms → refill = (int)(500 * 0.01) = 5
            // newTokens = min(5, 3 + 5) = 5 (capped at capacity)
            Thread.sleep(500);
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 1);
            assertTrue(status.isSuccess());
            assertEquals(4, status.getRemainingTokens()); // 5 - 1 = 4
        }

        @Test
        @DisplayName("tryAcquire(userId, 1) acquires 1 token")
        void testDefaultTryAcquireSingleToken() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 1);
            assertTrue(status.isSuccess());
            assertEquals(9, status.getRemainingTokens()); // 10 - 1 = 9
        }
    }

    // ==================== Token Expiration Tests ====================

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("expired user is treated as new - gets full capacity")
        void testExpiredUserGetsFreshBucket() throws InterruptedException {
            // capacity=5, tokensPerSecond=10, alivePeriod=200ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 10, 200);
            // Drain all tokens
            strategy.tryAcquire("user1", 5);
            // Wait beyond alive period
            Thread.sleep(250);
            // User should be treated as new with full capacity
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 5);
            assertTrue(status.isSuccess());
            assertEquals(0, status.getRemainingTokens());
        }

        @Test
        @DisplayName("not expired user retains state")
        void testNotExpiredUserRetainsState() throws InterruptedException {
            // capacity=5, tokensPerSecond=1, alivePeriod=500ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 1, 500);
            // Drain all tokens
            strategy.tryAcquire("user1", 5);
            // Wait less than alive period
            Thread.sleep(100);
            // refill = (int)(100 * 0.001) = 0, still no tokens
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 5);
            assertFalse(status.isSuccess());
        }

        @Test
        @DisplayName("expiration resets even partially consumed bucket")
        void testExpirationResetsPartialBucket() throws InterruptedException {
            // capacity=10, tokensPerSecond=1, alivePeriod=200ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(10, 1, 200);
            // Use 3 tokens
            strategy.tryAcquire("user1", 3);
            // Wait beyond alive period
            Thread.sleep(250);
            // Should reset to full capacity=10
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 10);
            assertTrue(status.isSuccess());
            assertEquals(0, status.getRemainingTokens());
        }
    }

    // ==================== Retry After Tests ====================

    @Nested
    @DisplayName("Retry After Tests")
    class RetryAfterTests {

        @Test
        @DisplayName("retry after retryAfterMs time succeeds")
        void testRetryAfterWaitingSucceeds() throws InterruptedException {
            // capacity=5, tokensPerSecond=10, alivePeriod=10000ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 10, 10000);
            // Drain all tokens
            strategy.tryAcquire("user1", 5);
            // Request 2 tokens - should be rejected
            RateLimiterSolution.CheckStatus rejected = strategy.tryAcquire("user1", 2);
            assertFalse(rejected.isSuccess());
            long retryAfter = rejected.getRetryAfterMs();
            assertTrue(retryAfter > 0);
            // Wait for retryAfterMs + small buffer
            Thread.sleep(retryAfter + 50);
            // Now should succeed
            RateLimiterSolution.CheckStatus allowed = strategy.tryAcquire("user1", 2);
            assertTrue(allowed.isSuccess());
        }

        @Test
        @DisplayName("retry before retryAfterMs time still fails")
        void testRetryTooEarlyStillFails() throws InterruptedException {
            // capacity=5, tokensPerSecond=1, alivePeriod=60000ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 1, 60000);
            // Drain all tokens
            strategy.tryAcquire("user1", 5);
            // Request 5 tokens - rejected, retryAfter = ceil(5/0.001) = 5000ms
            RateLimiterSolution.CheckStatus rejected = strategy.tryAcquire("user1", 5);
            assertFalse(rejected.isSuccess());
            assertEquals(5000, rejected.getRetryAfterMs());
            // Wait only 100ms - not enough
            Thread.sleep(100);
            // refill = (int)(100 * 0.001) = 0, still not enough
            RateLimiterSolution.CheckStatus stillRejected = strategy.tryAcquire("user1", 5);
            assertFalse(stillRejected.isSuccess());
        }
    }

    // ==================== Top-Level RateLimiterSolution Tests ====================

    @Nested
    @DisplayName("RateLimiterSolution Top-Level Tests")
    class TopLevelTests {

        @Test
        @DisplayName("tryAcquire(userId) delegates to strategy for 1 token")
        void testTryAcquireSingleToken() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(10, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);
            RateLimiterSolution.CheckStatus status = limiter.tryAcquire("user1");
            assertTrue(status.isSuccess());
            assertEquals(9, status.getRemainingTokens());
        }

        @Test
        @DisplayName("tryAcquire(userId, tokens) delegates to strategy")
        void testTryAcquireMultipleTokens() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(10, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);
            RateLimiterSolution.CheckStatus status = limiter.tryAcquire("user1", 7);
            assertTrue(status.isSuccess());
            assertEquals(3, status.getRemainingTokens());
        }

        @Test
        @DisplayName("new user gets allowed, exhausted user gets rejected")
        void testNewUserAllowedExhaustedUserRejected() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);

            // New user: allowed
            RateLimiterSolution.CheckStatus first = limiter.tryAcquire("user1", 5);
            assertTrue(first.isSuccess());
            assertEquals(0, first.getRemainingTokens());

            // Same user, no tokens left: rejected
            RateLimiterSolution.CheckStatus second = limiter.tryAcquire("user1", 1);
            assertFalse(second.isSuccess());
            assertTrue(second.getRetryAfterMs() > 0);
        }

        @Test
        @DisplayName("new user comes in while existing user is exhausted")
        void testNewUserWhileExistingExhausted() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);

            // Exhaust user1
            limiter.tryAcquire("user1", 5);
            RateLimiterSolution.CheckStatus user1Status = limiter.tryAcquire("user1", 1);
            assertFalse(user1Status.isSuccess());

            // New user2 is independent and gets full capacity
            RateLimiterSolution.CheckStatus user2Status = limiter.tryAcquire("user2", 3);
            assertTrue(user2Status.isSuccess());
            assertEquals(2, user2Status.getRemainingTokens());
        }

        @Test
        @DisplayName("reject then retry after retryAfterMs - end to end")
        void testRejectThenRetryEndToEnd() throws InterruptedException {
            // capacity=3, tokensPerSecond=10, alivePeriod=10000ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(3, 10, 10000);
            var limiter = new RateLimiterSolution(strategy);

            // Drain all 3 tokens
            limiter.tryAcquire("user1", 3);

            // Request 1 token - rejected
            RateLimiterSolution.CheckStatus rejected = limiter.tryAcquire("user1", 1);
            assertFalse(rejected.isSuccess());
            long retryAfter = rejected.getRetryAfterMs();
            assertTrue(retryAfter > 0);

            // Wait the suggested retryAfter time + buffer
            Thread.sleep(retryAfter + 50);

            // Now should succeed
            RateLimiterSolution.CheckStatus allowed = limiter.tryAcquire("user1", 1);
            assertTrue(allowed.isSuccess());
        }

        @Test
        @DisplayName("token expiration resets through top-level API")
        void testTokenExpirationThroughTopLevel() throws InterruptedException {
            // capacity=5, tokensPerSecond=1, alivePeriod=200ms
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 1, 200);
            var limiter = new RateLimiterSolution(strategy);

            // Drain all tokens
            limiter.tryAcquire("user1", 5);
            // Confirm rejected
            RateLimiterSolution.CheckStatus rejected = limiter.tryAcquire("user1", 1);
            assertFalse(rejected.isSuccess());

            // Wait beyond alive period so bucket expires
            Thread.sleep(250);

            // User gets fresh bucket with full capacity
            RateLimiterSolution.CheckStatus allowed = limiter.tryAcquire("user1", 5);
            assertTrue(allowed.isSuccess());
            assertEquals(0, allowed.getRemainingTokens());
        }

        @Test
        @DisplayName("consecutive requests decrement tokens correctly")
        void testConsecutiveRequestsDecrementTokens() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(5, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);

            RateLimiterSolution.CheckStatus s1 = limiter.tryAcquire("user1"); // 5 - 1 = 4
            assertTrue(s1.isSuccess());
            assertEquals(4, s1.getRemainingTokens());

            RateLimiterSolution.CheckStatus s2 = limiter.tryAcquire("user1"); // ~4 - 1 = ~3
            assertTrue(s2.isSuccess());

            RateLimiterSolution.CheckStatus s3 = limiter.tryAcquire("user1");
            assertTrue(s3.isSuccess());

            RateLimiterSolution.CheckStatus s4 = limiter.tryAcquire("user1");
            assertTrue(s4.isSuccess());

            RateLimiterSolution.CheckStatus s5 = limiter.tryAcquire("user1");
            assertTrue(s5.isSuccess());

            // 6th request without enough time to refill - should be rejected or have 0 remaining
            // (tiny refill might give 0 tokens due to int cast)
            RateLimiterSolution.CheckStatus s6 = limiter.tryAcquire("user1");
            // With tiny elapsed ms, refill = (int)(~0ms * 0.01) = 0
            // So this should be rejected
            assertFalse(s6.isSuccess());
        }
    }

    // ==================== Strategy Interface Tests ====================

    @Nested
    @DisplayName("RateLimitCheckStrategy Interface Tests")
    class StrategyInterfaceTests {

        @Test
        @DisplayName("custom strategy implementation works with RateLimiterSolution")
        void testCustomStrategyImplementation() {
            // Create a custom implementation
            RateLimiterSolution.RateLimitCheckStrategy customStrategy =
                new RateLimiterSolution.RateLimitCheckStrategy() {
                    @Override
                    public RateLimiterSolution.CheckStatus tryAcquire(String userId, int tokens) {
                        // Return tokens requested as remaining to verify what was passed
                        return RateLimiterSolution.CheckStatus.allow(tokens);
                    }
                };
            RateLimiterSolution.CheckStatus status = customStrategy.tryAcquire("user1", 1);
            assertTrue(status.isSuccess());
            assertEquals(1, status.getRemainingTokens());
        }

        @Test
        @DisplayName("BucketTokenStrategy implements RateLimitCheckStrategy")
        void testBucketTokenStrategyImplementsInterface() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(10, 10, 5000);
            assertInstanceOf(RateLimiterSolution.RateLimitCheckStrategy.class, strategy);
        }
    }

    // ==================== Input Validation Tests ====================

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("tryAcquire rejects tokenRequired = 0")
        void testTryAcquireRejectsZeroTokens() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(10, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);
            assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire("user1", 0));
        }

        @Test
        @DisplayName("tryAcquire rejects negative tokenRequired")
        void testTryAcquireRejectsNegativeTokens() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(10, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);
            assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire("user1", -1));
        }

        @Test
        @DisplayName("BucketTokenStrategy rejects capacity = 0")
        void testConstructorRejectsZeroCapacity() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.BucketTokenStrategy(0, 10, 5000));
        }

        @Test
        @DisplayName("BucketTokenStrategy rejects negative capacity")
        void testConstructorRejectsNegativeCapacity() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.BucketTokenStrategy(-1, 10, 5000));
        }

        @Test
        @DisplayName("BucketTokenStrategy rejects tokensPerSecond = 0")
        void testConstructorRejectsZeroRefillRate() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.BucketTokenStrategy(10, 0, 5000));
        }

        @Test
        @DisplayName("BucketTokenStrategy rejects negative tokensPerSecond")
        void testConstructorRejectsNegativeRefillRate() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.BucketTokenStrategy(10, -10, 5000));
        }

        @Test
        @DisplayName("BucketTokenStrategy rejects tokenAlivePeriod = 0")
        void testConstructorRejectsZeroAlivePeriod() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.BucketTokenStrategy(10, 10, 0));
        }

        @Test
        @DisplayName("BucketTokenStrategy rejects negative tokenAlivePeriod")
        void testConstructorRejectsNegativeAlivePeriod() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.BucketTokenStrategy(10, 10, -1));
        }

        @Test
        @DisplayName("tryAcquire rejects null userId")
        void testTryAcquireRejectsNullUserId() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(10, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);
            assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(null));
        }

        @Test
        @DisplayName("tryAcquire with tokens rejects null userId")
        void testTryAcquireWithTokensRejectsNullUserId() {
            var strategy = new RateLimiterSolution.BucketTokenStrategy(10, 10, 5000);
            var limiter = new RateLimiterSolution(strategy);
            assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(null, 5));
        }
    }

    // ==================== CheckStatus Equals/HashCode Tests ====================

    @Nested
    @DisplayName("CheckStatus Equals and HashCode Tests")
    class CheckStatusEqualsHashCodeTests {

        @Test
        @DisplayName("equal allow statuses are equal")
        void testEqualAllowStatuses() {
            var a = RateLimiterSolution.CheckStatus.allow(5);
            var b = RateLimiterSolution.CheckStatus.allow(5);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("equal reject statuses are equal")
        void testEqualRejectStatuses() {
            var a = RateLimiterSolution.CheckStatus.reject(1000);
            var b = RateLimiterSolution.CheckStatus.reject(1000);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("allow and reject are not equal")
        void testAllowNotEqualToReject() {
            var allow = RateLimiterSolution.CheckStatus.allow(0);
            var reject = RateLimiterSolution.CheckStatus.reject(0);
            assertNotEquals(allow, reject);
        }

        @Test
        @DisplayName("different remaining tokens are not equal")
        void testDifferentRemainingTokensNotEqual() {
            var a = RateLimiterSolution.CheckStatus.allow(5);
            var b = RateLimiterSolution.CheckStatus.allow(3);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("different retryAfterMs are not equal")
        void testDifferentRetryAfterNotEqual() {
            var a = RateLimiterSolution.CheckStatus.reject(500);
            var b = RateLimiterSolution.CheckStatus.reject(1000);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("equals with same instance returns true")
        void testEqualsSameInstance() {
            var a = RateLimiterSolution.CheckStatus.allow(5);
            assertEquals(a, a);
        }

        @Test
        @DisplayName("equals with null returns false")
        void testEqualsNull() {
            var a = RateLimiterSolution.CheckStatus.allow(5);
            assertNotEquals(null, a);
        }

        @Test
        @DisplayName("equals with different type returns false")
        void testEqualsDifferentType() {
            var a = RateLimiterSolution.CheckStatus.allow(5);
            assertNotEquals("not a CheckStatus", a);
        }
    }

    // ==================== WindowState Tests ====================

    @Nested
    @DisplayName("WindowState Tests")
    class WindowStateTests {

        @Test
        @DisplayName("constructor sets initial values")
        void testConstructorSetsValues() {
            var state = new RateLimiterSolution.FixedWindowStrategy.WindowState(12345L, 3);
            assertEquals(12345L, state.getStartTime());
            assertEquals(3, state.getUsedTokens());
        }

        @Test
        @DisplayName("setStartTime updates start time")
        void testSetStartTime() {
            var state = new RateLimiterSolution.FixedWindowStrategy.WindowState(0L, 0);
            state.setStartTime(99999L);
            assertEquals(99999L, state.getStartTime());
        }

        @Test
        @DisplayName("setUsedTokens updates used tokens")
        void testSetUsedTokens() {
            var state = new RateLimiterSolution.FixedWindowStrategy.WindowState(0L, 0);
            state.setUsedTokens(7);
            assertEquals(7, state.getUsedTokens());
        }
    }

    // ==================== FixedWindowStrategy Tests ====================

    @Nested
    @DisplayName("FixedWindowStrategy Tests")
    class FixedWindowStrategyTests {

        // maxTokens=10, windowWidth=1000ms
        private RateLimiterSolution.FixedWindowStrategy createStrategy() {
            return new RateLimiterSolution.FixedWindowStrategy(10, 1000);
        }

        @Test
        @DisplayName("new user gets allowed - single token")
        void testNewUserSingleToken() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 1);
            assertTrue(status.isSuccess());
            assertEquals(9, status.getRemainingTokens());
        }

        @Test
        @DisplayName("new user gets allowed - multiple tokens")
        void testNewUserMultipleTokens() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 5);
            assertTrue(status.isSuccess());
            assertEquals(5, status.getRemainingTokens());
        }

        @Test
        @DisplayName("new user requesting all tokens succeeds with 0 remaining")
        void testNewUserRequestAllTokens() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 10);
            assertTrue(status.isSuccess());
            assertEquals(0, status.getRemainingTokens());
        }

        @Test
        @DisplayName("new user requesting more than max is rejected")
        void testNewUserExceedsMax() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 11);
            assertFalse(status.isSuccess());
            assertTrue(status.getRetryAfterMs() > 0);
        }

        @Test
        @DisplayName("existing user - enough tokens in window")
        void testExistingUserEnoughTokens() {
            var strategy = createStrategy();
            strategy.tryAcquire("user1", 3);
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 5);
            assertTrue(status.isSuccess());
            assertEquals(2, status.getRemainingTokens()); // 10 - 3 - 5 = 2
        }

        @Test
        @DisplayName("existing user - not enough tokens in window, gets rejected")
        void testExistingUserNotEnoughTokens() {
            var strategy = createStrategy();
            strategy.tryAcquire("user1", 10);
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 1);
            assertFalse(status.isSuccess());
            assertTrue(status.getRetryAfterMs() > 0);
        }

        @Test
        @DisplayName("different users have independent windows")
        void testDifferentUsersIndependentWindows() {
            var strategy = createStrategy();
            strategy.tryAcquire("user1", 10);
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user2", 5);
            assertTrue(status.isSuccess());
            assertEquals(5, status.getRemainingTokens());
        }

        @Test
        @DisplayName("window expires and resets count")
        void testWindowExpiresAndResets() throws InterruptedException {
            // maxTokens=5, windowWidth=200ms
            var strategy = new RateLimiterSolution.FixedWindowStrategy(5, 200);
            strategy.tryAcquire("user1", 5);
            // Confirm rejected
            RateLimiterSolution.CheckStatus rejected = strategy.tryAcquire("user1", 1);
            assertFalse(rejected.isSuccess());
            // Wait beyond window
            Thread.sleep(250);
            // New window: should succeed with full capacity
            RateLimiterSolution.CheckStatus allowed = strategy.tryAcquire("user1", 5);
            assertTrue(allowed.isSuccess());
            assertEquals(0, allowed.getRemainingTokens());
        }

        @Test
        @DisplayName("within window, count is not reset")
        void testWithinWindowCountRetained() throws InterruptedException {
            // maxTokens=5, windowWidth=500ms
            var strategy = new RateLimiterSolution.FixedWindowStrategy(5, 500);
            strategy.tryAcquire("user1", 5);
            Thread.sleep(100);
            // Still within window
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 1);
            assertFalse(status.isSuccess());
        }

        @Test
        @DisplayName("retryAfterMs is time remaining in current window")
        void testRetryAfterMsIsTimeRemainingInWindow() throws InterruptedException {
            // maxTokens=5, windowWidth=500ms
            var strategy = new RateLimiterSolution.FixedWindowStrategy(5, 500);
            strategy.tryAcquire("user1", 5);
            Thread.sleep(100);
            RateLimiterSolution.CheckStatus status = strategy.tryAcquire("user1", 1);
            assertFalse(status.isSuccess());
            // retryAfterMs should be roughly 400ms (500 - ~100 elapsed)
            assertTrue(status.getRetryAfterMs() > 0);
            assertTrue(status.getRetryAfterMs() <= 500);
        }

        @Test
        @DisplayName("retry after window expires succeeds")
        void testRetryAfterWindowExpiresSucceeds() throws InterruptedException {
            // maxTokens=3, windowWidth=200ms
            var strategy = new RateLimiterSolution.FixedWindowStrategy(3, 200);
            strategy.tryAcquire("user1", 3);
            RateLimiterSolution.CheckStatus rejected = strategy.tryAcquire("user1", 1);
            assertFalse(rejected.isSuccess());
            long retryAfter = rejected.getRetryAfterMs();
            assertTrue(retryAfter > 0);
            // Wait for window to expire
            Thread.sleep(retryAfter + 50);
            RateLimiterSolution.CheckStatus allowed = strategy.tryAcquire("user1", 1);
            assertTrue(allowed.isSuccess());
        }

        @Test
        @DisplayName("consecutive requests decrement remaining correctly")
        void testConsecutiveRequestsDecrement() {
            var strategy = createStrategy();
            RateLimiterSolution.CheckStatus s1 = strategy.tryAcquire("user1", 1);
            assertEquals(9, s1.getRemainingTokens());
            RateLimiterSolution.CheckStatus s2 = strategy.tryAcquire("user1", 1);
            assertEquals(8, s2.getRemainingTokens());
            RateLimiterSolution.CheckStatus s3 = strategy.tryAcquire("user1", 3);
            assertEquals(5, s3.getRemainingTokens());
        }
    }

    // ==================== FixedWindowStrategy Validation Tests ====================

    @Nested
    @DisplayName("FixedWindowStrategy Validation Tests")
    class FixedWindowValidationTests {

        @Test
        @DisplayName("rejects maxTokens = 0")
        void testRejectsZeroMaxTokens() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.FixedWindowStrategy(0, 1000));
        }

        @Test
        @DisplayName("rejects negative maxTokens")
        void testRejectsNegativeMaxTokens() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.FixedWindowStrategy(-1, 1000));
        }

        @Test
        @DisplayName("rejects windowWidth = 0")
        void testRejectsZeroWindowWidth() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.FixedWindowStrategy(10, 0));
        }

        @Test
        @DisplayName("rejects negative windowWidth")
        void testRejectsNegativeWindowWidth() {
            assertThrows(IllegalArgumentException.class,
                () -> new RateLimiterSolution.FixedWindowStrategy(10, -1));
        }

        @Test
        @DisplayName("FixedWindowStrategy implements RateLimitCheckStrategy")
        void testImplementsInterface() {
            var strategy = new RateLimiterSolution.FixedWindowStrategy(10, 1000);
            assertInstanceOf(RateLimiterSolution.RateLimitCheckStrategy.class, strategy);
        }
    }

    // ==================== Top-Level with FixedWindowStrategy Tests ====================

    @Nested
    @DisplayName("RateLimiterSolution with FixedWindowStrategy Tests")
    class TopLevelFixedWindowTests {

        @Test
        @DisplayName("tryAcquire(userId) delegates to FixedWindowStrategy for 1 token")
        void testTryAcquireSingleToken() {
            var strategy = new RateLimiterSolution.FixedWindowStrategy(10, 5000);
            var limiter = new RateLimiterSolution(strategy);
            RateLimiterSolution.CheckStatus status = limiter.tryAcquire("user1");
            assertTrue(status.isSuccess());
            assertEquals(9, status.getRemainingTokens());
        }

        @Test
        @DisplayName("tryAcquire(userId, tokens) delegates to FixedWindowStrategy")
        void testTryAcquireMultipleTokens() {
            var strategy = new RateLimiterSolution.FixedWindowStrategy(10, 5000);
            var limiter = new RateLimiterSolution(strategy);
            RateLimiterSolution.CheckStatus status = limiter.tryAcquire("user1", 7);
            assertTrue(status.isSuccess());
            assertEquals(3, status.getRemainingTokens());
        }

        @Test
        @DisplayName("new user allowed, exhausted user rejected")
        void testNewUserAllowedExhaustedRejected() {
            var strategy = new RateLimiterSolution.FixedWindowStrategy(5, 5000);
            var limiter = new RateLimiterSolution(strategy);

            RateLimiterSolution.CheckStatus first = limiter.tryAcquire("user1", 5);
            assertTrue(first.isSuccess());
            assertEquals(0, first.getRemainingTokens());

            RateLimiterSolution.CheckStatus second = limiter.tryAcquire("user1", 1);
            assertFalse(second.isSuccess());
            assertTrue(second.getRetryAfterMs() > 0);
        }

        @Test
        @DisplayName("new user comes in while existing user is exhausted")
        void testNewUserWhileExistingExhausted() {
            var strategy = new RateLimiterSolution.FixedWindowStrategy(5, 5000);
            var limiter = new RateLimiterSolution(strategy);

            limiter.tryAcquire("user1", 5);
            assertFalse(limiter.tryAcquire("user1", 1).isSuccess());

            RateLimiterSolution.CheckStatus user2Status = limiter.tryAcquire("user2", 3);
            assertTrue(user2Status.isSuccess());
            assertEquals(2, user2Status.getRemainingTokens());
        }

        @Test
        @DisplayName("reject then retry after window expires - end to end")
        void testRejectThenRetryEndToEnd() throws InterruptedException {
            var strategy = new RateLimiterSolution.FixedWindowStrategy(3, 200);
            var limiter = new RateLimiterSolution(strategy);

            limiter.tryAcquire("user1", 3);
            RateLimiterSolution.CheckStatus rejected = limiter.tryAcquire("user1", 1);
            assertFalse(rejected.isSuccess());

            Thread.sleep(250);

            RateLimiterSolution.CheckStatus allowed = limiter.tryAcquire("user1", 1);
            assertTrue(allowed.isSuccess());
        }

        @Test
        @DisplayName("window expiration resets through top-level API")
        void testWindowExpirationThroughTopLevel() throws InterruptedException {
            var strategy = new RateLimiterSolution.FixedWindowStrategy(5, 200);
            var limiter = new RateLimiterSolution(strategy);

            limiter.tryAcquire("user1", 5);
            assertFalse(limiter.tryAcquire("user1", 1).isSuccess());

            Thread.sleep(250);

            RateLimiterSolution.CheckStatus allowed = limiter.tryAcquire("user1", 5);
            assertTrue(allowed.isSuccess());
            assertEquals(0, allowed.getRemainingTokens());
        }
    }
}
