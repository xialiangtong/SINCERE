import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TopKProductsSolution Tests")
public class TopKProductsSolutionTest {

    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    // ==================== Construction Tests ====================

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("valid window days creates instance")
        void testValidConstruction() {
            assertDoesNotThrow(() -> new TopKProductsSolution(30));
        }

        @Test
        @DisplayName("zero window days throws")
        void testZeroWindowDays() {
            assertThrows(IllegalArgumentException.class, () -> new TopKProductsSolution(0));
        }

        @Test
        @DisplayName("negative window days throws")
        void testNegativeWindowDays() {
            assertThrows(IllegalArgumentException.class, () -> new TopKProductsSolution(-1));
        }
    }

    // ==================== RecordSale Tests ====================

    @Nested
    @DisplayName("RecordSale Tests")
    class RecordSaleTests {

        @Test
        @DisplayName("null productId throws")
        void testNullProductId() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            assertThrows(IllegalArgumentException.class, () -> sol.recordSale(null, 1000L));
        }

        @Test
        @DisplayName("empty productId throws")
        void testEmptyProductId() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            assertThrows(IllegalArgumentException.class, () -> sol.recordSale("", 1000L));
        }

        @Test
        @DisplayName("zero quantity throws")
        void testZeroQuantity() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            assertThrows(IllegalArgumentException.class, () -> sol.recordSale("A", 1000L, 0));
        }

        @Test
        @DisplayName("negative quantity throws")
        void testNegativeQuantity() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            assertThrows(IllegalArgumentException.class, () -> sol.recordSale("A", 1000L, -1));
        }
    }

    // ==================== GetTopK Tests ====================

    @Nested
    @DisplayName("GetTopK Tests")
    class GetTopKTests {

        @Test
        @DisplayName("zero k throws")
        void testZeroK() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            assertThrows(IllegalArgumentException.class, () -> sol.getTopK(0, 1000L));
        }

        @Test
        @DisplayName("no sales returns empty list")
        void testNoSales() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            List<TopKProductsSolution.ProductCount> result = sol.getTopK(3, 1000L);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("single product single sale")
        void testSingleProductSingleSale() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;
            sol.recordSale("A", now);
            List<TopKProductsSolution.ProductCount> result = sol.getTopK(3, now);
            assertEquals(1, result.size());
            assertEquals("A", result.get(0).getProductId());
            assertEquals(1, result.get(0).getCount());
        }

        @Test
        @DisplayName("returns top K sorted by count descending")
        void testTopKOrder() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            sol.recordSale("A", now, 5);
            sol.recordSale("B", now, 10);
            sol.recordSale("C", now, 3);
            sol.recordSale("D", now, 8);

            List<TopKProductsSolution.ProductCount> result = sol.getTopK(3, now);

            assertEquals(3, result.size());
            assertEquals("B", result.get(0).getProductId());
            assertEquals(10, result.get(0).getCount());
            assertEquals("D", result.get(1).getProductId());
            assertEquals(8, result.get(1).getCount());
            assertEquals("A", result.get(2).getProductId());
            assertEquals(5, result.get(2).getCount());
        }

        @Test
        @DisplayName("k larger than number of products returns all")
        void testKLargerThanProducts() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            sol.recordSale("A", now, 3);
            sol.recordSale("B", now, 1);

            List<TopKProductsSolution.ProductCount> result = sol.getTopK(10, now);

            assertEquals(2, result.size());
            assertEquals("A", result.get(0).getProductId());
            assertEquals("B", result.get(1).getProductId());
        }

        @Test
        @DisplayName("k equals 1 returns the single top product")
        void testTopOne() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            sol.recordSale("A", now, 2);
            sol.recordSale("B", now, 7);
            sol.recordSale("C", now, 5);

            List<TopKProductsSolution.ProductCount> result = sol.getTopK(1, now);

            assertEquals(1, result.size());
            assertEquals("B", result.get(0).getProductId());
            assertEquals(7, result.get(0).getCount());
        }
    }

    // ==================== Time Window Tests ====================

    @Nested
    @DisplayName("Time Window Tests")
    class TimeWindowTests {

        @Test
        @DisplayName("sales outside window are not counted")
        void testExpiredSales() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            // Sale 31 days ago — outside the 30-day window
            sol.recordSale("A", now - 31 * DAY_MS, 10);
            // Sale today
            sol.recordSale("B", now, 2);

            List<TopKProductsSolution.ProductCount> result = sol.getTopK(3, now);

            assertEquals(1, result.size());
            assertEquals("B", result.get(0).getProductId());
            assertEquals(2, result.get(0).getCount());
        }

        @Test
        @DisplayName("sales exactly at window boundary are excluded")
        void testExactBoundary() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            // Exactly 30 days ago (cutoff = now - 30 days, sales <= cutoff are excluded)
            sol.recordSale("A", now - 30 * DAY_MS);

            List<TopKProductsSolution.ProductCount> result = sol.getTopK(3, now);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("sales just inside window are counted")
        void testJustInsideWindow() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            sol.recordSale("A", now - 30 * DAY_MS + 1);

            List<TopKProductsSolution.ProductCount> result = sol.getTopK(3, now);
            assertEquals(1, result.size());
            assertEquals("A", result.get(0).getProductId());
        }

        @Test
        @DisplayName("mix of expired and valid sales counts only valid")
        void testMixedExpiredAndValid() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            // 5 expired + 3 valid for product A
            sol.recordSale("A", now - 40 * DAY_MS, 5);
            sol.recordSale("A", now - 1 * DAY_MS, 3);

            // 2 valid for product B
            sol.recordSale("B", now, 2);

            List<TopKProductsSolution.ProductCount> result = sol.getTopK(2, now);

            assertEquals(2, result.size());
            assertEquals("A", result.get(0).getProductId());
            assertEquals(3, result.get(0).getCount());
            assertEquals("B", result.get(1).getProductId());
            assertEquals(2, result.get(1).getCount());
        }
    }

    // ==================== Cleanup Tests ====================

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("cleanup removes all expired products")
        void testCleanupRemovesExpired() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            sol.recordSale("A", now - 40 * DAY_MS, 5);
            sol.recordSale("B", now, 2);

            sol.cleanup(now);

            // Only B should remain
            List<TopKProductsSolution.ProductCount> result = sol.getTopK(10, now);
            assertEquals(1, result.size());
            assertEquals("B", result.get(0).getProductId());
        }

        @Test
        @DisplayName("cleanup keeps valid sales within partially expired products")
        void testCleanupPartialExpiry() {
            TopKProductsSolution sol = new TopKProductsSolution(30);
            long now = 100 * DAY_MS;

            sol.recordSale("A", now - 40 * DAY_MS, 3); // expired
            sol.recordSale("A", now - 1 * DAY_MS, 2);  // valid

            sol.cleanup(now);

            List<TopKProductsSolution.ProductCount> result = sol.getTopK(10, now);
            assertEquals(1, result.size());
            assertEquals("A", result.get(0).getProductId());
            assertEquals(2, result.get(0).getCount());
        }
    }

    // ==================== ProductCount Tests ====================

    @Nested
    @DisplayName("ProductCount Tests")
    class ProductCountTests {

        @Test
        @DisplayName("equals and hashCode work correctly")
        void testEqualsAndHashCode() {
            TopKProductsSolution.ProductCount a = new TopKProductsSolution.ProductCount("X", 5);
            TopKProductsSolution.ProductCount b = new TopKProductsSolution.ProductCount("X", 5);
            TopKProductsSolution.ProductCount c = new TopKProductsSolution.ProductCount("Y", 5);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertNotEquals(a, c);
        }

        @Test
        @DisplayName("toString contains product id and count")
        void testToString() {
            TopKProductsSolution.ProductCount pc = new TopKProductsSolution.ProductCount("X", 7);
            String s = pc.toString();
            assertTrue(s.contains("X"));
            assertTrue(s.contains("7"));
        }
    }
}
