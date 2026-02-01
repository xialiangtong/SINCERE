import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProductShipmentTest {

    private ProductShipment solution;

    @BeforeEach
    void setUp() {
        solution = new ProductShipment();
    }

    @Test
    @DisplayName("No dependencies - each product ships separately")
    void testNoDependencies() {
        int n = 4;
        int[][] dependencies = {};

        List<List<Integer>> result = solution.getProductShipmentList(n, dependencies);

        assertEquals(4, result.size(), "Should have 4 separate shipments");
        // Each product should be in its own group
        for (List<Integer> group : result) {
            assertEquals(1, group.size(), "Each group should contain exactly 1 product");
        }
    }

    @Test
    @DisplayName("All products should be shipped together")
    void testAllProductsShippedTogether() {
        int n = 4;
        int[][] dependencies = {{0, 1}, {1, 2}, {2, 3}};

        List<List<Integer>> result = solution.getProductShipmentList(n, dependencies);

        assertEquals(1, result.size(), "Should have only 1 shipment");
        assertEquals(4, result.get(0).size(), "The single shipment should contain all 4 products");
        
        // Verify all products are present
        Set<Integer> products = new HashSet<>(result.get(0));
        assertTrue(products.contains(0));
        assertTrue(products.contains(1));
        assertTrue(products.contains(2));
        assertTrue(products.contains(3));
    }

    @Test
    @DisplayName("Duplicates in dependencies - should handle gracefully")
    void testDuplicateDependencies() {
        int n = 3;
        int[][] dependencies = {{0, 1}, {0, 1}, {1, 2}, {2, 1}}; // duplicates: 0-1 appears twice, 1-2 and 2-1

        List<List<Integer>> result = solution.getProductShipmentList(n, dependencies);

        assertEquals(1, result.size(), "Should have 1 shipment despite duplicates");
        assertEquals(3, result.get(0).size(), "Should contain all 3 products");
    }

    @Test
    @DisplayName("Products shipped in several batches - multiple connected components")
    void testMultipleBatches() {
        int n = 6;
        int[][] dependencies = {{0, 1}, {2, 3}, {4, 5}}; // Three separate groups: [0,1], [2,3], [4,5]

        List<List<Integer>> result = solution.getProductShipmentList(n, dependencies);

        assertEquals(3, result.size(), "Should have 3 separate shipments");
        
        // Each group should have 2 products
        for (List<Integer> group : result) {
            assertEquals(2, group.size(), "Each group should contain 2 products");
        }

        // Verify the groupings are correct
        Set<Set<Integer>> expectedGroups = new HashSet<>();
        expectedGroups.add(new HashSet<>(Arrays.asList(0, 1)));
        expectedGroups.add(new HashSet<>(Arrays.asList(2, 3)));
        expectedGroups.add(new HashSet<>(Arrays.asList(4, 5)));

        Set<Set<Integer>> actualGroups = new HashSet<>();
        for (List<Integer> group : result) {
            actualGroups.add(new HashSet<>(group));
        }

        assertEquals(expectedGroups, actualGroups, "Groups should match expected groupings");
    }

    @Test
    @DisplayName("Single product with no dependencies")
    void testSingleProduct() {
        int n = 1;
        int[][] dependencies = {};

        List<List<Integer>> result = solution.getProductShipmentList(n, dependencies);

        assertEquals(1, result.size(), "Should have 1 shipment");
        assertEquals(1, result.get(0).size(), "Should contain 1 product");
        assertEquals(0, result.get(0).get(0), "Product should be 0");
    }

    @Test
    @DisplayName("Mixed scenario - some connected, some isolated")
    void testMixedScenario() {
        int n = 5;
        int[][] dependencies = {{0, 1}, {1, 2}}; // Products 0,1,2 connected; 3,4 isolated

        List<List<Integer>> result = solution.getProductShipmentList(n, dependencies);

        assertEquals(3, result.size(), "Should have 3 shipments");
        
        // Find the group with 3 products (connected component)
        boolean foundConnectedGroup = false;
        int isolatedCount = 0;
        for (List<Integer> group : result) {
            if (group.size() == 3) {
                foundConnectedGroup = true;
                Set<Integer> products = new HashSet<>(group);
                assertTrue(products.contains(0));
                assertTrue(products.contains(1));
                assertTrue(products.contains(2));
            } else if (group.size() == 1) {
                isolatedCount++;
            }
        }
        
        assertTrue(foundConnectedGroup, "Should have one group with 3 connected products");
        assertEquals(2, isolatedCount, "Should have 2 isolated products");
    }
}
