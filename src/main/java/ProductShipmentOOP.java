import java.util.*;

/**
 * OOP-style Product Shipment Solution
 * 
 * This solution provides better extensibility through object-oriented design,
 * allowing for easy addition of product attributes, dependency rules, and shipping logic.
 */
public class ProductShipmentOOP {

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
     * Represents a dependency rule between two products.
     * Products linked by a dependency rule must be shipped together.
     */
    public static class DependencyRule {
        private final Product product1;
        private final Product product2;

        public DependencyRule(Product product1, Product product2) {
            this.product1 = product1;
            this.product2 = product2;
        }

        public Product getProduct1() {
            return product1;
        }

        public Product getProduct2() {
            return product2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyRule that = (DependencyRule) o;
            // A rule (A, B) is equal to (B, A) since it's undirected
            return (Objects.equals(product1, that.product1) && Objects.equals(product2, that.product2))
                    || (Objects.equals(product1, that.product2) && Objects.equals(product2, that.product1));
        }

        @Override
        public int hashCode() {
            // Use a symmetric hash so (A, B) and (B, A) have the same hash
            return Objects.hash(product1.getId()) + Objects.hash(product2.getId());
        }

        @Override
        public String toString() {
            return "DependencyRule{" + product1 + " <-> " + product2 + "}";
        }
    }

    /**
     * Represents a group of products that must be shipped together.
     */
    public static class ShippingGroup {
        private final Set<Product> products;

        public ShippingGroup() {
            this.products = new LinkedHashSet<>();
        }

        public void addProduct(Product product) {
            products.add(product);
        }

        public Set<Product> getProducts() {
            return Collections.unmodifiableSet(products);
        }

        public int size() {
            return products.size();
        }

        public boolean contains(Product product) {
            return products.contains(product);
        }

        @Override
        public String toString() {
            return "ShippingGroup{products=" + products + "}";
        }
    }

    /**
     * Manages the product catalog including products, dependency rules,
     * and provides methods to compute shipping groups.
     */
    public static class ProductCatalog {
        private final Map<Integer, Product> products;
        private final Set<DependencyRule> dependencyRules;
        private final Map<Product, Set<Product>> adjacencyList;

        public ProductCatalog() {
            this.products = new LinkedHashMap<>();
            this.dependencyRules = new LinkedHashSet<>();
            this.adjacencyList = new HashMap<>();
        }

        /**
         * Adds a product to the catalog.
         */
        public void addProduct(Product product) {
            products.put(product.getId(), product);
            adjacencyList.putIfAbsent(product, new HashSet<>());
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
         * Adds a dependency rule between two products.
         * Both products must already exist in the catalog.
         */
        public void addDependencyRule(DependencyRule rule) {
            Product p1 = rule.getProduct1();
            Product p2 = rule.getProduct2();

            // Ensure both products are in the catalog
            if (!products.containsKey(p1.getId())) {
                addProduct(p1);
            }
            if (!products.containsKey(p2.getId())) {
                addProduct(p2);
            }

            dependencyRules.add(rule);

            // Update adjacency list (undirected graph)
            adjacencyList.get(p1).add(p2);
            adjacencyList.get(p2).add(p1);
        }

        /**
         * Adds a dependency rule between two products by their IDs.
         */
        public void addDependencyRule(int productId1, int productId2) {
            Product p1 = products.get(productId1);
            Product p2 = products.get(productId2);
            if (p1 != null && p2 != null) {
                addDependencyRule(new DependencyRule(p1, p2));
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
         * Computes shipping groups using BFS to find connected components.
         * @return List of shipping groups
         */
        public List<ShippingGroup> getShippingGroups() {
            List<ShippingGroup> groups = new ArrayList<>();
            Set<Product> visited = new HashSet<>();

            for (Product product : products.values()) {
                if (!visited.contains(product)) {
                    ShippingGroup group = new ShippingGroup();
                    bfs(product, visited, group);
                    groups.add(group);
                }
            }

            return groups;
        }

        private void bfs(Product start, Set<Product> visited, ShippingGroup group) {
            Queue<Product> queue = new LinkedList<>();
            queue.add(start);
            visited.add(start);

            while (!queue.isEmpty()) {
                Product current = queue.poll();
                group.addProduct(current);

                for (Product neighbor : adjacencyList.getOrDefault(current, Collections.emptySet())) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        /**
         * Gets the minimum number of shipments needed.
         */
        public int getMinimumShipments() {
            return getShippingGroups().size();
        }

        @Override
        public String toString() {
            return "ProductCatalog{products=" + products.size() + 
                   ", rules=" + dependencyRules.size() + "}";
        }
    }

    public static void main(String[] args) {
        // Example usage with ProductCatalog
        ProductCatalog catalog = new ProductCatalog();

        // Add products
        Product p0 = new Product(0, "Laptop");
        Product p1 = new Product(1, "Charger");
        Product p2 = new Product(2, "Mouse");
        Product p3 = new Product(3, "Keyboard");
        Product p4 = new Product(4, "Monitor");
        
        catalog.addProducts(p0, p1, p2, p3, p4);

        // Add dependency rules
        catalog.addDependencyRule(new DependencyRule(p0, p1)); // Laptop must ship with Charger
        catalog.addDependencyRule(new DependencyRule(p1, p2)); // Charger must ship with Mouse
        catalog.addDependencyRule(new DependencyRule(p3, p4)); // Keyboard must ship with Monitor

        System.out.println("Catalog: " + catalog);
        System.out.println("All Products: " + catalog.getAllProducts());
        System.out.println("Dependency Rules: " + catalog.getDependencyRules());
        System.out.println();

        // Get shipping groups
        List<ShippingGroup> groups = catalog.getShippingGroups();
        System.out.println("Shipping Groups (" + groups.size() + " shipments needed):");
        for (int i = 0; i < groups.size(); i++) {
            System.out.println("  Shipment " + (i + 1) + ": " + groups.get(i));
        }
    }
}
