import java.util.HashMap;
import java.util.Map;

public class LFUSolution<K, V> {

    private final int capacity;
    private int minFreq;
    private final Map<K, Node<K, V>> keyMap;
    private final Map<Integer, DLinkedNode<K, V>> freqMap;

    // ======================== Constructor ========================

    public LFUSolution(int capacity) {
        this.capacity = capacity;
        this.minFreq = 0;
        this.keyMap = new HashMap<>();
        this.freqMap = new HashMap<>();
    }

    // ======================== Public Methods ========================

    public synchronized V get(K key) {
        if (!keyMap.containsKey(key)) return null;
        Node<K, V> node = keyMap.get(key);
        increaseFreq(node);
        return node.value;
    }

    public synchronized boolean write(K key, V value) {
        if (capacity <= 0) return false;

        // Update existing key
        Node<K, V> node = keyMap.get(key);
        if (node != null) {
            node.value = value;
            increaseFreq(node);
            return true;
        }

        // Evict if at capacity
        if (keyMap.size() >= capacity) {
            evict();
        }

        // Insert new key
        Node<K, V> newNode = new Node<>(key, value);
        keyMap.put(key, newNode);
        DLinkedNode<K, V> list = freqMap.computeIfAbsent(1, k -> new DLinkedNode<>());
        list.addFirst(newNode);
        minFreq = 1;
        return true;
    }

    // ======================== Private Helpers ========================

    private void evict() {
        DLinkedNode<K, V> minList = freqMap.get(minFreq);
        Node<K, V> evicted = minList.removeLast();
        keyMap.remove(evicted.key);
        if (minList.isEmpty()) {
            freqMap.remove(minFreq);
        }
    }

    private void increaseFreq(Node<K, V> node) {
        int oldFreq = node.freq;
        DLinkedNode<K, V> oldList = freqMap.get(oldFreq);
        oldList.remove(node);
        if (oldList.isEmpty()) {
            freqMap.remove(oldFreq);
            if (minFreq == oldFreq) {
                minFreq++;
            }
        }
        node.freq++;
        DLinkedNode<K, V> newList = freqMap.computeIfAbsent(node.freq, k -> new DLinkedNode<>());
        newList.addFirst(node);
    }

    // ======================== Inner Class: Node ========================

    static class Node<K, V> {
        K key;
        V value;
        int freq;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.freq = 1;
        }
    }

    // ======================== Inner Class: DLinkedNode ========================

    static class DLinkedNode<K, V> {
        Node<K, V> head;
        Node<K, V> tail;
        int size;

        DLinkedNode() {
            head = new Node<>(null, null);
            tail = new Node<>(null, null);
            head.next = tail;
            tail.prev = head;
            size = 0;
        }

        void addFirst(Node<K, V> node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
            size++;
        }

        void remove(Node<K, V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            node.prev = null;
            node.next = null;
            size--;
        }

        Node<K, V> removeLast() {
            if (isEmpty()) return null;
            Node<K, V> last = tail.prev;
            remove(last);
            return last;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }
}
