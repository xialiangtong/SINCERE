import java.util.*;

/**
 * Product Shipping System - Undirected Graph Solution
 * 
 * Finds which products must be shipped together based on dependency rules.
 * Uses BFS to find connected components in an undirected graph.
 */
public class ProductShipping {

    // ==================== Domain Classes ====================

    /**
     * Represents a product in the catalog.
     */
    public static class Product {
        private final String id;
        private final String name;

        public Product(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }

        @Override
        public String toString() {
            return String.format("Product{id='%s', name='%s'}", id, name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Product product = (Product) o;
            return Objects.equals(id, product.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * Represents a dependency rule between two products.
     * In an undirected graph, (A, B) means A and B must ship together.
     */
    public static class DependencyRule {
        private final String productA;
        private final String productB;

        public DependencyRule(String productA, String productB) {
            this.productA = productA;
            this.productB = productB;
        }

        public String getProductA() { return productA; }
        public String getProductB() { return productB; }

        @Override
        public String toString() {
            return String.format("(%s <-> %s)", productA, productB);
        }
    }

    // ==================== Graph ====================

    /**
     * Undirected graph using adjacency list representation.
     */
    public static class UndirectedGraph {
        private final Map<String, Set<String>> adjacencyList;

        public UndirectedGraph() {
            this.adjacencyList = new HashMap<>();
        }

        /**
         * Add a node to the graph.
         */
        public void addNode(String productId) {
            adjacencyList.putIfAbsent(productId, new HashSet<>());
        }

        /**
         * Add an undirected edge between two products.
         * This adds both A->B and B->A connections.
         */
        public void addEdge(String productA, String productB) {
            addNode(productA);
            addNode(productB);
            adjacencyList.get(productA).add(productB);
            adjacencyList.get(productB).add(productA);
        }

        /**
         * Get all neighbors of a node.
         */
        public Set<String> getNeighbors(String node) {
            return adjacencyList.getOrDefault(node, Collections.emptySet());
        }

        /**
         * Get all nodes in the graph.
         */
        public Set<String> getNodes() {
            return adjacencyList.keySet();
        }

        /**
         * Check if a node exists in the graph.
         */
        public boolean hasNode(String node) {
            return adjacencyList.containsKey(node);
        }
    }

    // ==================== Shipping Group ====================

    /**
     * Represents a group of products that must be shipped together.
     */
    public static class ShippingGroup {
        private final int groupId;
        private final Set<String> products;

        public ShippingGroup(int groupId, Set<String> products) {
            this.groupId = groupId;
            this.products = new HashSet<>(products);
        }

        public int getGroupId() { return groupId; }
        public Set<String> getProducts() { return Collections.unmodifiableSet(products); }
        public int size() { return products.size(); }
        public boolean contains(String productId) { return products.contains(productId); }

        @Override
        public String toString() {
            return String.format("Shipment #%d: %s", groupId, products);
        }
    }

    // ==================== Shipping Result ====================

    /**
     * Result of shipping analysis containing all groups and statistics.
     */
    public static class ShippingResult {
        private final List<ShippingGroup> groups;
        private final int minShipments;

        public ShippingResult(List<ShippingGroup> groups) {
            this.groups = new ArrayList<>(groups);
            this.minShipments = groups.size();
        }

        public List<ShippingGroup> getGroups() { 
            return Collections.unmodifiableList(groups); 
        }
        
        public int getMinShipments() { 
            return minShipments; 
        }

        /**
         * Find which group contains a specific product.
         */
        public Optional<ShippingGroup> findGroupForProduct(String productId) {
            return groups.stream()
                    .filter(g -> g.contains(productId))
                    .findFirst();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Shipping Analysis ===\n");
            sb.append(String.format("Minimum shipments needed: %d\n", minShipments));
            sb.append("\nShipping Groups:\n");
            for (ShippingGroup group : groups) {
                sb.append("  ").append(group).append("\n");
            }
            return sb.toString();
        }
    }

    // ==================== Shipping Group Finder ====================

    /**
     * Finds connected components (shipping groups) using BFS.
     */
    public static class ShippingGroupFinder {
        private final UndirectedGraph graph;
        private final Set<String> visited;

        public ShippingGroupFinder(UndirectedGraph graph) {
            this.graph = graph;
            this.visited = new HashSet<>();
        }

        /**
         * Find all shipping groups (connected components).
         */
        public List<ShippingGroup> findGroups() {
            List<ShippingGroup> groups = new ArrayList<>();
            visited.clear();
            int groupId = 1;

            for (String node : graph.getNodes()) {
                if (!visited.contains(node)) {
                    Set<String> component = bfs(node);
                    groups.add(new ShippingGroup(groupId++, component));
                }
            }

            return groups;
        }

        /**
         * BFS to find all nodes connected to the start node.
         */
        private Set<String> bfs(String startNode) {
            Set<String> component = new HashSet<>();
            Queue<String> queue = new LinkedList<>();

            queue.offer(startNode);
            visited.add(startNode);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                component.add(current);

                for (String neighbor : graph.getNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }

            return component;
        }

        /**
         * Get the minimum number of shipments needed.
         */
        public int getMinShipments() {
            return findGroups().size();
        }
    }

    // ==================== Main API ====================

    /**
     * Analyze shipping requirements for a list of products with dependency rules.
     * 
     * @param productIds List of product IDs to ship
     * @param rules List of dependency rules (A, B means A and B must ship together)
     * @return ShippingResult containing groups and minimum shipments
     */
    public static ShippingResult analyze(List<String> productIds, List<DependencyRule> rules) {
        // Build the undirected graph
        UndirectedGraph graph = new UndirectedGraph();

        // Add all products as nodes
        for (String productId : productIds) {
            graph.addNode(productId);
        }

        // Add dependency edges (undirected)
        for (DependencyRule rule : rules) {
            // Only add edge if both products are in our list
            if (graph.hasNode(rule.getProductA()) && graph.hasNode(rule.getProductB())) {
                graph.addEdge(rule.getProductA(), rule.getProductB());
            }
        }

        // Find shipping groups
        ShippingGroupFinder finder = new ShippingGroupFinder(graph);
        List<ShippingGroup> groups = finder.findGroups();

        return new ShippingResult(groups);
    }

    /**
     * Convenience method using string arrays.
     */
    public static ShippingResult analyze(String[] productIds, String[][] rules) {
        List<String> products = Arrays.asList(productIds);
        List<DependencyRule> dependencyRules = new ArrayList<>();
        
        for (String[] rule : rules) {
            if (rule.length >= 2) {
                dependencyRules.add(new DependencyRule(rule[0], rule[1]));
            }
        }
        
        return analyze(products, dependencyRules);
    }

    // ==================== Demo ====================

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║       Product Shipping - Undirected Graph Demo         ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Example 1: Basic connected components
        System.out.println("Example 1: Basic Grouping");
        System.out.println("─────────────────────────────────────────");
        String[] products1 = {"A", "B", "C", "D", "E", "F"};
        String[][] rules1 = {
            {"A", "B"},  // A and B must ship together
            {"B", "C"},  // B and C must ship together
            {"D", "E"}   // D and E must ship together
        };
        
        System.out.println("Products: " + Arrays.toString(products1));
        System.out.println("Rules: A<->B, B<->C, D<->E");
        System.out.println("\nGraph visualization:");
        System.out.println("  A ─── B ─── C       D ─── E       F (isolated)\n");
        
        ShippingResult result1 = analyze(products1, rules1);
        System.out.println(result1);

        // Example 2: Chain of dependencies
        System.out.println("\nExample 2: Chain Dependencies");
        System.out.println("─────────────────────────────────────────");
        String[] products2 = {"P1", "P2", "P3", "P4", "P5"};
        String[][] rules2 = {
            {"P1", "P2"},
            {"P2", "P3"},
            {"P3", "P4"},
            {"P4", "P5"}
        };
        
        System.out.println("Products: " + Arrays.toString(products2));
        System.out.println("Rules: P1<->P2<->P3<->P4<->P5 (chain)\n");
        System.out.println("Graph visualization:");
        System.out.println("  P1 ─── P2 ─── P3 ─── P4 ─── P5\n");
        
        ShippingResult result2 = analyze(products2, rules2);
        System.out.println(result2);

        // Example 3: Multiple isolated groups
        System.out.println("\nExample 3: Multiple Isolated Groups");
        System.out.println("─────────────────────────────────────────");
        String[] products3 = {"X", "Y", "Z", "W"};
        String[][] rules3 = {}; // No dependencies
        
        System.out.println("Products: " + Arrays.toString(products3));
        System.out.println("Rules: None (all isolated)\n");
        
        ShippingResult result3 = analyze(products3, rules3);
        System.out.println(result3);

        // Example 4: All connected
        System.out.println("\nExample 4: All Connected (One Shipment)");
        System.out.println("─────────────────────────────────────────");
        String[] products4 = {"M", "N", "O", "P"};
        String[][] rules4 = {
            {"M", "N"},
            {"N", "O"},
            {"O", "P"},
            {"P", "M"}  // Forms a cycle - all connected
        };
        
        System.out.println("Products: " + Arrays.toString(products4));
        System.out.println("Rules: M<->N<->O<->P<->M (cycle - all connected)\n");
        System.out.println("Graph visualization:");
        System.out.println("  M ─── N");
        System.out.println("  │     │");
        System.out.println("  P ─── O\n");
        
        ShippingResult result4 = analyze(products4, rules4);
        System.out.println(result4);

        // Example 5: Find which group a product belongs to
        System.out.println("\nExample 5: Query Product Group");
        System.out.println("─────────────────────────────────────────");
        String queryProduct = "B";
        result1.findGroupForProduct(queryProduct).ifPresentOrElse(
            group -> System.out.printf("Product '%s' belongs to %s%n", queryProduct, group),
            () -> System.out.printf("Product '%s' not found in any group%n", queryProduct)
        );
    }
}
