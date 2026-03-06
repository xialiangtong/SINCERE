import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Top K Most Sold Products in the Last N Days
 *
 * Tracks product sales with timestamps and supports querying
 * the top K most sold products within a sliding time window.
 */
public class TopKProductsSolution {

    private final long windowMs;
    private final Map<String, Deque<Long>> salesMap; // productId -> timestamps

    // ======================== Constructor ========================

    public TopKProductsSolution(long windowDays) {
        if (windowDays <= 0) {
            throw new IllegalArgumentException("windowDays must be > 0");
        }
        this.windowMs = windowDays * 24 * 60 * 60 * 1000L;
        this.salesMap = new HashMap<>();
    }

    // ======================== Public Methods ========================

    /**
     * Record a sale for the given product at the current time.
     */
    public void recordSale(String productId) {
        recordSale(productId, System.currentTimeMillis());
    }

    /**
     * Record a sale for the given product at the specified timestamp.
     */
    public void recordSale(String productId, long timestamp) {
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("productId must not be null or empty");
        }
        salesMap.computeIfAbsent(productId, k -> new ArrayDeque<>()).addLast(timestamp);
    }

    /**
     * Record multiple sales for the given product at the specified timestamp.
     */
    public void recordSale(String productId, long timestamp, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        for (int i = 0; i < quantity; i++) {
            recordSale(productId, timestamp);
        }
    }

    /**
     * Get the top K most sold products within the time window, as of now.
     */
    public List<ProductCount> getTopK(int k) {
        return getTopK(k, System.currentTimeMillis());
    }

    /**
     * Get the top K most sold products within the time window, as of the given time.
     */
    public List<ProductCount> getTopK(int k, long now) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be > 0");
        }

        long cutoff = now - windowMs;

        // Min-heap of size K (smallest count on top → easy to evict)
        PriorityQueue<ProductCount> minHeap = new PriorityQueue<>(
                Comparator.comparingInt(ProductCount::getCount));

        for (Map.Entry<String, Deque<Long>> entry : salesMap.entrySet()) {
            String productId = entry.getKey();
            Deque<Long> timestamps = entry.getValue();

            // Evict expired timestamps from the front
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }

            int count = timestamps.size();
            if (count == 0) continue;

            if (minHeap.size() < k) {
                minHeap.offer(new ProductCount(productId, count));
            } else if (count > minHeap.peek().getCount()) {
                minHeap.poll();
                minHeap.offer(new ProductCount(productId, count));
            }
        }

        // Drain heap into result list, sorted descending by count
        List<ProductCount> result = new ArrayList<>(minHeap.size());
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll());
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Remove all expired entries to free memory.
     */
    public void cleanup(long now) {
        long cutoff = now - windowMs;
        Iterator<Map.Entry<String, Deque<Long>>> it = salesMap.entrySet().iterator();
        while (it.hasNext()) {
            Deque<Long> timestamps = it.next().getValue();
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.isEmpty()) {
                it.remove();
            }
        }
    }

    // ======================== ProductCount ========================

    /**
     * Holds a product ID and its sale count.
     */
    public static class ProductCount {
        private final String productId;
        private final int count;

        public ProductCount(String productId, int count) {
            this.productId = productId;
            this.count = count;
        }

        public String getProductId() {
            return productId;
        }

        public int getCount() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProductCount)) return false;
            ProductCount that = (ProductCount) o;
            return count == that.count && Objects.equals(productId, that.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, count);
        }

        @Override
        public String toString() {
            return "ProductCount{" + productId + ", count=" + count + "}";
        }
    }
}
