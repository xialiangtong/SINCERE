import java.util.HashMap;
import java.util.Map;

public class LRUSolution<K, V> {

    private final Node<K, V> head;
    private final Node<K, V> tail;
    private final int capacity;
    private final Map<K, Node<K, V>> map;

    // ======================== Constructor ========================

    public LRUSolution(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    // ======================== Public Methods ========================

    public synchronized V get(K key) {
        if (!map.containsKey(key)) return null;
        Node<K, V> node = map.get(key);
        moveToHead(node);
        return node.value;
    }

    public synchronized boolean write(K key, V value) {
        if (capacity <= 0) return false;

        // Update existing key
        Node<K, V> node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
            return true;
        }

        // Evict if at capacity
        if (map.size() >= capacity) {
            evict();
        }

        // Insert new key
        Node<K, V> newNode = new Node<>(key, value);
        map.put(key, newNode);
        addAfterHead(newNode);
        return true;
    }

    // ======================== Private Helpers ========================

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addAfterHead(node);
    }

    private void addAfterHead(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    private void evict() {
        Node<K, V> lru = tail.prev;
        removeNode(lru);
        map.remove(lru.key);
    }

    // ======================== Inner Class: Node ========================

    static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
