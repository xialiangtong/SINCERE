import java.util.*;

/**
 * OOP-style Product Shipment Solution for Directed Dependencies
 * 
 * This solution handles directed dependencies where product1 requires product2
 * to be shipped first. This requires topological sorting to determine valid
 * shipping order and cycle detection to identify circular dependencies.
 */
public class ProductShipmentDirected {

    /**
     * Represents a product in the catalog.
     */
    public static class Product {
        private final int id;
        private final String name;

        public Product(int id) {
            this.id = id;
            this.name = "Product-" + id;
        }

        public Product(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Product product = (Product) o;
            return id == product.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Product{id=" + id + ", name='" + name + "'}";
        }
    }

    /**
     * Represents a directed dependency rule between two products.
     * product1 depends on product2, meaning product2 must be shipped before product1.
     */
    public static class DependencyRule {
        private final Product dependent;    // The product that has a dependency
        private final Product prerequisite; // The product that must be shipped first

        /**
         * Creates a dependency rule where 'dependent' requires 'prerequisite' to be shipped first.
         * @param dependent The product that depends on another
         * @param prerequisite The product that must be shipped before the dependent
         */
        public DependencyRule(Product dependent, Product prerequisite) {
            this.dependent = dependent;
            this.prerequisite = prerequisite;
        }

        public Product getDependent() {
            return dependent;
        }

        public Product getPrerequisite() {
            return prerequisite;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyRule that = (DependencyRule) o;
            return Objects.equals(dependent, that.dependent) && 
                   Objects.equals(prerequisite, that.prerequisite);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dependent, prerequisite);
        }

        @Override
        public String toString() {
            return "DependencyRule{" + dependent.getName() + " depends on " + prerequisite.getName() + "}";
        }
    }

    /**
     * Represents an ordered shipment containing products in their required shipping order.
     */
    public static class ShipmentOrder {
        private final List<Product> orderedProducts;
        private final boolean isValid;
        private final String errorMessage;

        private ShipmentOrder(List<Product> orderedProducts, boolean isValid, String errorMessage) {
            this.orderedProducts = orderedProducts;
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public static ShipmentOrder valid(List<Product> orderedProducts) {
            return new ShipmentOrder(new ArrayList<>(orderedProducts), true, null);
        }

        public static ShipmentOrder invalid(String errorMessage) {
            return new ShipmentOrder(Collections.emptyList(), false, errorMessage);
        }

        public List<Product> getOrderedProducts() {
            return Collections.unmodifiableList(orderedProducts);
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            if (isValid) {
                return "ShipmentOrder{valid=true, order=" + orderedProducts + "}";
            } else {
                return "ShipmentOrder{valid=false, error='" + errorMessage + "'}";
            }
        }
    }

    /**
     * Result of cycle detection containing whether a cycle exists and the cycle path if found.
     */
    public static class CycleDetectionResult {
        private final boolean hasCycle;
        private final List<Product> cyclePath;

        private CycleDetectionResult(boolean hasCycle, List<Product> cyclePath) {
            this.hasCycle = hasCycle;
            this.cyclePath = cyclePath;
        }

        public static CycleDetectionResult noCycle() {
            return new CycleDetectionResult(false, Collections.emptyList());
        }

        public static CycleDetectionResult cycleFound(List<Product> cyclePath) {
            return new CycleDetectionResult(true, new ArrayList<>(cyclePath));
        }

        public boolean hasCycle() {
            return hasCycle;
        }

        public List<Product> getCyclePath() {
            return Collections.unmodifiableList(cyclePath);
        }

        @Override
        public String toString() {
            if (hasCycle) {
                return "CycleDetectionResult{hasCycle=true, path=" + cyclePath + "}";
            } else {
                return "CycleDetectionResult{hasCycle=false}";
            }
        }
    }

    /**
     * Manages the product catalog with directed dependencies.
     * Provides topological sorting and cycle detection.
     */
    public static class ProductCatalog {
        private final Map<Integer, Product> products;
        private final Set<DependencyRule> dependencyRules;
        private final Map<Product, Set<Product>> adjacencyList;  // dependent -> prerequisites
        private final Map<Product, Integer> inDegree;            // Number of dependencies for each product

        public ProductCatalog() {
            this.products = new LinkedHashMap<>();
            this.dependencyRules = new LinkedHashSet<>();
            this.adjacencyList = new HashMap<>();
            this.inDegree = new HashMap<>();
        }

        /**
         * Adds a product to the catalog.
         */
        public void addProduct(Product product) {
            products.put(product.getId(), product);
            adjacencyList.putIfAbsent(product, new HashSet<>());
            inDegree.putIfAbsent(product, 0);
        }

        /**
         * Adds multiple products to the catalog.
         */
        public void addProducts(Product... productsToAdd) {
            for (Product product : productsToAdd) {
                addProduct(product);
            }
        }

        /**
         * Adds a directed dependency rule.
         * The dependent product requires the prerequisite to be shipped first.
         */
        public void addDependencyRule(DependencyRule rule) {
            Product dependent = rule.getDependent();
            Product prerequisite = rule.getPrerequisite();

            // Ensure both products are in the catalog
            if (!products.containsKey(dependent.getId())) {
                addProduct(dependent);
            }
            if (!products.containsKey(prerequisite.getId())) {
                addProduct(prerequisite);
            }

            // Avoid duplicate rules
            if (dependencyRules.contains(rule)) {
                return;
            }

            dependencyRules.add(rule);

            // Update adjacency list: prerequisite -> dependent (edge direction for topological sort)
            adjacencyList.get(prerequisite).add(dependent);
            
            // Update in-degree: dependent has one more prerequisite
            inDegree.put(dependent, inDegree.getOrDefault(dependent, 0) + 1);
        }

        /**
         * Adds a dependency rule: product with dependentId depends on product with prerequisiteId.
         */
        public void addDependencyRule(int dependentId, int prerequisiteId) {
            Product dependent = products.get(dependentId);
            Product prerequisite = products.get(prerequisiteId);
            if (dependent != null && prerequisite != null) {
                addDependencyRule(new DependencyRule(dependent, prerequisite));
            }
        }

        /**
         * Gets a product by its ID.
         */
        public Product getProduct(int id) {
            return products.get(id);
        }

        /**
         * Gets all products in the catalog.
         */
        public Collection<Product> getAllProducts() {
            return Collections.unmodifiableCollection(products.values());
        }

        /**
         * Gets all dependency rules.
         */
        public Set<DependencyRule> getDependencyRules() {
            return Collections.unmodifiableSet(dependencyRules);
        }

        /**
         * Detects if there are circular dependencies in the catalog.
         * Uses DFS-based cycle detection.
         */
        public CycleDetectionResult detectCycle() {
            Set<Product> visited = new HashSet<>();
            Set<Product> recursionStack = new HashSet<>();
            List<Product> cyclePath = new ArrayList<>();

            for (Product product : products.values()) {
                if (detectCycleDFS(product, visited, recursionStack, cyclePath)) {
                    Collections.reverse(cyclePath);
                    return CycleDetectionResult.cycleFound(cyclePath);
                }
            }

            return CycleDetectionResult.noCycle();
        }

        private boolean detectCycleDFS(Product current, Set<Product> visited, 
                                       Set<Product> recursionStack, List<Product> cyclePath) {
            if (recursionStack.contains(current)) {
                cyclePath.add(current);
                return true;
            }

            if (visited.contains(current)) {
                return false;
            }

            visited.add(current);
            recursionStack.add(current);

            for (Product neighbor : adjacencyList.getOrDefault(current, Collections.emptySet())) {
                if (detectCycleDFS(neighbor, visited, recursionStack, cyclePath)) {
                    cyclePath.add(current);
                    return true;
                }
            }

            recursionStack.remove(current);
            return false;
        }

        /**
         * Computes a valid shipping order using topological sort (Kahn's algorithm).
         * Returns an invalid result if circular dependencies exist.
         */
        public ShipmentOrder getShippingOrder() {
            // Check for cycles first
            CycleDetectionResult cycleResult = detectCycle();
            if (cycleResult.hasCycle()) {
                return ShipmentOrder.invalid("Circular dependency detected: " + cycleResult.getCyclePath());
            }

            // Kahn's algorithm for topological sort
            Map<Product, Integer> inDegreeCopy = new HashMap<>(inDegree);
            Queue<Product> queue = new LinkedList<>();
            List<Product> result = new ArrayList<>();

            // Start with products that have no prerequisites
            for (Product product : products.values()) {
                if (inDegreeCopy.getOrDefault(product, 0) == 0) {
                    queue.add(product);
                }
            }

            while (!queue.isEmpty()) {
                Product current = queue.poll();
                result.add(current);

                for (Product dependent : adjacencyList.getOrDefault(current, Collections.emptySet())) {
                    int newDegree = inDegreeCopy.get(dependent) - 1;
                    inDegreeCopy.put(dependent, newDegree);
                    if (newDegree == 0) {
                        queue.add(dependent);
                    }
                }
            }

            if (result.size() != products.size()) {
                return ShipmentOrder.invalid("Unable to determine shipping order - possible undetected cycle");
            }

            return ShipmentOrder.valid(result);
        }

        /**
         * Checks if shipping is possible (no circular dependencies).
         */
        public boolean isShippingPossible() {
            return !detectCycle().hasCycle();
        }

        /**
         * Gets the number of shipment batches needed when respecting dependencies.
         * Products with no remaining prerequisites can be shipped in the same batch.
         */
        public int getMinimumBatches() {
            if (!isShippingPossible()) {
                return -1; // Invalid due to cycle
            }

            Map<Product, Integer> batchLevel = new HashMap<>();
            Map<Product, Integer> inDegreeCopy = new HashMap<>(inDegree);
            Queue<Product> queue = new LinkedList<>();

            // Start with products that have no prerequisites (batch 0)
            for (Product product : products.values()) {
                if (inDegreeCopy.getOrDefault(product, 0) == 0) {
                    queue.add(product);
                    batchLevel.put(product, 0);
                }
            }

            int maxBatch = 0;

            while (!queue.isEmpty()) {
                Product current = queue.poll();
                int currentBatch = batchLevel.get(current);

                for (Product dependent : adjacencyList.getOrDefault(current, Collections.emptySet())) {
                    int newDegree = inDegreeCopy.get(dependent) - 1;
                    inDegreeCopy.put(dependent, newDegree);
                    
                    // Dependent goes in the next batch after its latest prerequisite
                    int dependentBatch = Math.max(
                        batchLevel.getOrDefault(dependent, 0), 
                        currentBatch + 1
                    );
                    batchLevel.put(dependent, dependentBatch);
                    maxBatch = Math.max(maxBatch, dependentBatch);

                    if (newDegree == 0) {
                        queue.add(dependent);
                    }
                }
            }

            return maxBatch + 1; // Convert from 0-indexed to count
        }

        @Override
        public String toString() {
            return "ProductCatalog{products=" + products.size() + 
                   ", rules=" + dependencyRules.size() + "}";
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Directed Dependency Example ===\n");

        ProductCatalog catalog = new ProductCatalog();

        // Add products
        Product laptop = new Product(0, "Laptop");
        Product charger = new Product(1, "Charger");
        Product battery = new Product(2, "Battery");
        Product keyboard = new Product(3, "Keyboard");
        Product mouse = new Product(4, "Mouse");

        catalog.addProducts(laptop, charger, battery, keyboard, mouse);

        // Add directed dependencies:
        // - Laptop depends on Charger (charger must ship first)
        // - Laptop depends on Battery (battery must ship first)
        // - Charger depends on Battery (battery must ship first)
        catalog.addDependencyRule(new DependencyRule(laptop, charger));
        catalog.addDependencyRule(new DependencyRule(laptop, battery));
        catalog.addDependencyRule(new DependencyRule(charger, battery));

        System.out.println("Catalog: " + catalog);
        System.out.println("Dependency Rules: " + catalog.getDependencyRules());
        System.out.println();

        // Check for cycles
        CycleDetectionResult cycleResult = catalog.detectCycle();
        System.out.println("Cycle Detection: " + cycleResult);
        System.out.println("Shipping Possible: " + catalog.isShippingPossible());
        System.out.println();

        // Get shipping order
        ShipmentOrder order = catalog.getShippingOrder();
        System.out.println("Shipping Order: " + order);
        System.out.println("Minimum Batches: " + catalog.getMinimumBatches());

        System.out.println("\n=== Circular Dependency Example ===\n");

        ProductCatalog catalogWithCycle = new ProductCatalog();
        Product a = new Product(10, "ProductA");
        Product b = new Product(11, "ProductB");
        Product c = new Product(12, "ProductC");

        catalogWithCycle.addProducts(a, b, c);

        // Create a cycle: A -> B -> C -> A
        catalogWithCycle.addDependencyRule(new DependencyRule(a, b));
        catalogWithCycle.addDependencyRule(new DependencyRule(b, c));
        catalogWithCycle.addDependencyRule(new DependencyRule(c, a));

        System.out.println("Catalog with cycle: " + catalogWithCycle);
        System.out.println("Cycle Detection: " + catalogWithCycle.detectCycle());
        System.out.println("Shipping Order: " + catalogWithCycle.getShippingOrder());
    }
}
