import java.util.HashMap;
import java.util.Map;

/**
 * 460. LFU Cache (Hard)
 *
 * Design: O(1) get and put using:
 * - keyMap: key → Node (for O(1) key lookup)
 * - freqMap: frequency → DoublyLinkedList (for O(1) eviction of LFU/LRU)
 * - minFreq: tracks the current minimum frequency for O(1) eviction
 *
 * Each frequency bucket is a doubly-linked list ordered by recency.
 * The tail of the min-frequency list is the eviction candidate (LFU + LRU tie-break).
 */
public class LFUCache {

    private final int capacity;
    private int size;
    private int minFreq;

    private final Map<Integer, Node> keyMap;       // key → node
    private final Map<Integer, DLinkedList> freqMap; // freq → doubly-linked list

    // ==================== Node ====================

    static class Node {
        int key;
        int value;
        int freq;
        Node prev;
        Node next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
            this.freq = 1;
        }
    }

    // ==================== Doubly Linked List ====================

    /**
     * Doubly-linked list with sentinel head/tail.
     * Head side = most recently used, Tail side = least recently used.
     */
    static class DLinkedList {
        Node head; // sentinel
        Node tail; // sentinel
        int size;

        DLinkedList() {
            head = new Node(0, 0);
            tail = new Node(0, 0);
            head.next = tail;
            tail.prev = head;
            size = 0;
        }

        /**
         * Add node right after head (most recent position).
         */
        void addFirst(Node node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
            size++;
        }

        /**
         * Remove a specific node from the list.
         */
        void remove(Node node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            node.prev = null;
            node.next = null;
            size--;
        }

        /**
         * Remove and return the last node (least recently used).
         */
        Node removeLast() {
            if (size == 0) return null;
            Node last = tail.prev;
            remove(last);
            return last;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }

    // ==================== LFUCache ====================

    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.minFreq = 0;
        this.keyMap = new HashMap<>();
        this.freqMap = new HashMap<>();
    }

    /**
     * Get the value for the key. Returns -1 if not found.
     * Increments the frequency of the key.
     */
    public int get(int key) {
        if (!keyMap.containsKey(key)) return -1;

        Node node = keyMap.get(key);
        incrementFreq(node);
        return node.value;
    }

    /**
     * Put key-value pair. If key exists, update value and increment frequency.
     * If at capacity, evict the LFU (tie-break: LRU) entry first.
     */
    public void put(int key, int value) {
        if (capacity == 0) return;

        if (keyMap.containsKey(key)) {
            // Update existing
            Node node = keyMap.get(key);
            node.value = value;
            incrementFreq(node);
            return;
        }

        // Evict if at capacity
        if (size == capacity) {
            evict();
        }

        // Insert new node
        Node newNode = new Node(key, value);
        keyMap.put(key, newNode);
        freqMap.computeIfAbsent(1, k -> new DLinkedList()).addFirst(newNode);
        minFreq = 1; // new node always has freq 1
        size++;
    }

    /**
     * Move node from its current frequency list to freq+1 list.
     * Update minFreq if the old frequency list becomes empty.
     */
    private void incrementFreq(Node node) {
        int oldFreq = node.freq;
        DLinkedList oldList = freqMap.get(oldFreq);
        oldList.remove(node);

        // If old list is now empty and was the min, bump minFreq
        if (oldList.isEmpty()) {
            freqMap.remove(oldFreq);
            if (minFreq == oldFreq) {
                minFreq++;
            }
        }

        node.freq++;
        freqMap.computeIfAbsent(node.freq, k -> new DLinkedList()).addFirst(node);
    }

    /**
     * Evict the least frequently used node (LRU tie-break).
     */
    private void evict() {
        DLinkedList minList = freqMap.get(minFreq);
        Node evicted = minList.removeLast();

        if (evicted != null) {
            keyMap.remove(evicted.key);
            size--;
        }

        if (minList.isEmpty()) {
            freqMap.remove(minFreq);
        }
    }
}
