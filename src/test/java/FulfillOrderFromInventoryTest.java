import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FulfillOrderFromInventory Tests")
public class FulfillOrderFromInventoryTest {

    // =========================================================================
    // 1. Merchant Enum Tests
    // =========================================================================

    @Nested
    @DisplayName("Merchant Enum")
    class MerchantTests {

        @Test
        @DisplayName("fromCode returns correct merchant for uppercase codes")
        void testFromCodeUppercase() {
            assertEquals(FulfillOrderFromInventory.Merchant.J, FulfillOrderFromInventory.Merchant.fromCode("J"));
            assertEquals(FulfillOrderFromInventory.Merchant.H, FulfillOrderFromInventory.Merchant.fromCode("H"));
            assertEquals(FulfillOrderFromInventory.Merchant.S, FulfillOrderFromInventory.Merchant.fromCode("S"));
        }

        @Test
        @DisplayName("fromCode is case-insensitive")
        void testFromCodeCaseInsensitive() {
            assertEquals(FulfillOrderFromInventory.Merchant.J, FulfillOrderFromInventory.Merchant.fromCode("j"));
            assertEquals(FulfillOrderFromInventory.Merchant.H, FulfillOrderFromInventory.Merchant.fromCode("h"));
            assertEquals(FulfillOrderFromInventory.Merchant.S, FulfillOrderFromInventory.Merchant.fromCode("s"));
        }

        @Test
        @DisplayName("fromCode returns null for unknown code")
        void testFromCodeUnknown() {
            assertNull(FulfillOrderFromInventory.Merchant.fromCode("X"));
            assertNull(FulfillOrderFromInventory.Merchant.fromCode("banana"));
        }

        @Test
        @DisplayName("fromCode returns null for null input")
        void testFromCodeNull() {
            assertNull(FulfillOrderFromInventory.Merchant.fromCode(null));
        }

        @Test
        @DisplayName("fromCode returns null for blank input")
        void testFromCodeBlank() {
            assertNull(FulfillOrderFromInventory.Merchant.fromCode(""));
            assertNull(FulfillOrderFromInventory.Merchant.fromCode("   "));
        }

        @Test
        @DisplayName("fromCode trims whitespace around code")
        void testFromCodeWithWhitespace() {
            assertEquals(FulfillOrderFromInventory.Merchant.J, FulfillOrderFromInventory.Merchant.fromCode("  J  "));
        }
    }

    // =========================================================================
    // 2. InventorySystem Tests
    // =========================================================================

    @Nested
    @DisplayName("InventorySystem")
    class InventorySystemTests {

        private final FulfillOrderFromInventory.InventorySystem inv = new FulfillOrderFromInventory.InventorySystem();

        @Test
        @DisplayName("has five cities")
        void testCityCount() {
            assertEquals(5, inv.getCities().size());
        }

        @Test
        @DisplayName("getCities returns all expected cities")
        void testCityNames() {
            Set<String> cities = inv.getCities();
            assertTrue(cities.contains("Toronto"));
            assertTrue(cities.contains("Vancouver"));
            assertTrue(cities.contains("Montreal"));
            assertTrue(cities.contains("Calgary"));
            assertTrue(cities.contains("Halifax"));
        }

        @Test
        @DisplayName("getStock returns correct values for Toronto")
        void testStockToronto() {
            assertEquals(5, inv.getStock("Toronto", FulfillOrderFromInventory.Merchant.J));
            assertEquals(0, inv.getStock("Toronto", FulfillOrderFromInventory.Merchant.H));
            assertEquals(0, inv.getStock("Toronto", FulfillOrderFromInventory.Merchant.S));
        }

        @Test
        @DisplayName("getStock returns correct values for Calgary")
        void testStockCalgary() {
            assertEquals(1,  inv.getStock("Calgary", FulfillOrderFromInventory.Merchant.J));
            assertEquals(18, inv.getStock("Calgary", FulfillOrderFromInventory.Merchant.H));
            assertEquals(2,  inv.getStock("Calgary", FulfillOrderFromInventory.Merchant.S));
        }

        @Test
        @DisplayName("getStock returns 0 for unknown city")
        void testStockUnknownCity() {
            assertEquals(0, inv.getStock("Winnipeg", FulfillOrderFromInventory.Merchant.J));
        }

        @Test
        @DisplayName("display name abbreviations are correct")
        void testDisplayNames() {
            assertEquals("Tor",  inv.getDisplayName("Toronto"));
            assertEquals("Van",  inv.getDisplayName("Vancouver"));
            assertEquals("Mon",  inv.getDisplayName("Montreal"));
            assertEquals("Cal",  inv.getDisplayName("Calgary"));
            assertEquals("Hali", inv.getDisplayName("Halifax"));
        }

        @Test
        @DisplayName("display name falls back to full name for unknown city")
        void testDisplayNameFallback() {
            assertEquals("Winnipeg", inv.getDisplayName("Winnipeg"));
        }

        @Test
        @DisplayName("getCities returns unmodifiable set")
        void testCitiesUnmodifiable() {
            assertThrows(UnsupportedOperationException.class, () -> {
                inv.getCities().add("Winnipeg");
            });
        }
    }

    // =========================================================================
    // 3. Order Parsing Tests
    // =========================================================================

    @Nested
    @DisplayName("Order Parsing")
    class OrderParsingTests {

        @Test
        @DisplayName("parses a normal order string")
        void testParseNormal() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:3 H:2 S:4");
            Map<FulfillOrderFromInventory.Merchant, Integer> items = order.getItems();
            assertEquals(3, items.get(FulfillOrderFromInventory.Merchant.J));
            assertEquals(2, items.get(FulfillOrderFromInventory.Merchant.H));
            assertEquals(4, items.get(FulfillOrderFromInventory.Merchant.S));
        }

        @Test
        @DisplayName("handles mixed delimiters: period, space")
        void testParseMixedDelimiters() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:3. H:2 S:4");
            assertEquals(3, order.getItems().size());
        }

        @Test
        @DisplayName("handles case-insensitive merchant codes")
        void testParseLowercase() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("j:3 h:2 s:4");
            assertEquals(3, order.getItems().get(FulfillOrderFromInventory.Merchant.J));
            assertEquals(2, order.getItems().get(FulfillOrderFromInventory.Merchant.H));
            assertEquals(4, order.getItems().get(FulfillOrderFromInventory.Merchant.S));
        }

        @Test
        @DisplayName("ignores merchant with qty 0")
        void testParseZeroQty() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:0 H:2");
            assertNull(order.getItems().get(FulfillOrderFromInventory.Merchant.J));
            assertEquals(2, order.getItems().get(FulfillOrderFromInventory.Merchant.H));
        }

        @Test
        @DisplayName("ignores merchant with negative qty")
        void testParseNegativeQty() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:-5 H:2");
            assertNull(order.getItems().get(FulfillOrderFromInventory.Merchant.J));
            assertEquals(2, order.getItems().get(FulfillOrderFromInventory.Merchant.H));
        }

        @Test
        @DisplayName("ignores token with malformed number")
        void testParseMalformedNumber() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:abc H:2");
            assertNull(order.getItems().get(FulfillOrderFromInventory.Merchant.J));
            assertEquals(2, order.getItems().get(FulfillOrderFromInventory.Merchant.H));
        }

        @Test
        @DisplayName("throws exception for unknown merchant code")
        void testParseUnknownMerchant() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                FulfillOrderFromInventory.Order.parse("X:5");
            });
            assertTrue(ex.getMessage().contains("X"));
        }

        @Test
        @DisplayName("empty string produces empty order")
        void testParseEmpty() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("");
            assertTrue(order.isEmpty());
        }

        @Test
        @DisplayName("null input produces empty order")
        void testParseNull() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse(null);
            assertTrue(order.isEmpty());
        }

        @Test
        @DisplayName("all-zero order produces empty items")
        void testParseAllZero() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:0 H:0 S:0");
            assertTrue(order.isEmpty());
        }

        @Test
        @DisplayName("last occurrence wins for duplicate merchant")
        void testParseDuplicateMerchant() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:3 J:7");
            assertEquals(7, order.getItems().get(FulfillOrderFromInventory.Merchant.J));
        }

        @Test
        @DisplayName("items map is unmodifiable")
        void testItemsUnmodifiable() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:3");
            assertThrows(UnsupportedOperationException.class, () -> {
                order.getItems().put(FulfillOrderFromInventory.Merchant.H, 5);
            });
        }
    }

    // =========================================================================
    // 4. findProperCities — Core Logic Tests
    // =========================================================================

    @Nested
    @DisplayName("findProperCities")
    class FindProperCitiesTests {

        private final FulfillOrderFromInventory system = new FulfillOrderFromInventory();

        // ----- Example from problem statement -----

        @Test
        @DisplayName("example 1: J:3. H:2 s:4 -> Van, Mon, Hali")
        void testExample1() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:3. H:2 s:4");
            List<String> result = system.findProperCities(order);
            assertEquals(Arrays.asList("Van", "Mon", "Hali"), result);
        }

        @Test
        @DisplayName("example 2: H:7 S:1 -> Cal")
        void testExample2() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("H:7 S:1");
            List<String> result = system.findProperCities(order);
            assertEquals(Collections.singletonList("Cal"), result);
        }

        // ----- Multiple cities can cover -----

        @Test
        @DisplayName("multiple cities: J:1 -> all five cities have J >= 1")
        void testMultipleCitiesAllMatch() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:1");
            List<String> result = system.findProperCities(order);
            assertEquals(Arrays.asList("Tor", "Van", "Mon", "Cal", "Hali"), result);
        }

        @Test
        @DisplayName("multiple cities: S:2 -> Van, Mon, Cal, Hali")
        void testMultipleCitiesSome() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("S:2");
            List<String> result = system.findProperCities(order);
            assertEquals(Arrays.asList("Van", "Mon", "Cal", "Hali"), result);
        }

        // ----- Only one city can cover -----

        @Test
        @DisplayName("single city: J:28 -> only Hali")
        void testSingleCityHali() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:28");
            List<String> result = system.findProperCities(order);
            assertEquals(Collections.singletonList("Hali"), result);
        }

        @Test
        @DisplayName("single city: H:18 -> only Cal")
        void testSingleCityCal() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("H:18");
            List<String> result = system.findProperCities(order);
            assertEquals(Collections.singletonList("Cal"), result);
        }

        // ----- No city can cover -----

        @Test
        @DisplayName("no city: J:100 -> empty result")
        void testNoCityHugeQty() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:100");
            List<String> result = system.findProperCities(order);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("no city: all merchants need huge qty")
        void testNoCityAllHuge() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:100 H:100 S:100");
            List<String> result = system.findProperCities(order);
            assertTrue(result.isEmpty());
        }

        // ----- Corner case: empty order -> all cities + alert -----

        @Test
        @DisplayName("empty order returns all cities")
        void testEmptyOrderReturnsAllCities() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("");
            List<String> result = system.findProperCities(order);
            assertEquals(Arrays.asList("Tor", "Van", "Mon", "Cal", "Hali"), result);
        }

        @Test
        @DisplayName("all-zero order returns all cities")
        void testAllZeroOrderReturnsAllCities() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:0 H:0 S:0");
            List<String> result = system.findProperCities(order);
            assertEquals(Arrays.asList("Tor", "Van", "Mon", "Cal", "Hali"), result);
        }

        // ----- Corner case: partial invalid tokens ignored -----

        @Test
        @DisplayName("partial invalid: J:abc H:2 -> only H:2 is used")
        void testPartialInvalidMalformedNumber() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:abc H:2");
            List<String> result = system.findProperCities(order);
            // H >= 2: Van(2), Mon(5), Cal(18), Hali(2)
            assertEquals(Arrays.asList("Van", "Mon", "Cal", "Hali"), result);
        }

        @Test
        @DisplayName("partial invalid: J:-1 S:5 -> only S:5 is used")
        void testPartialInvalidNegativeQty() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:-1 S:5");
            List<String> result = system.findProperCities(order);
            // S >= 5: Van(6), Mon(5), Hali(12)
            assertEquals(Arrays.asList("Van", "Mon", "Hali"), result);
        }

        @Test
        @DisplayName("partial invalid: J:0 H:2 -> only H:2 is used")
        void testPartialInvalidZeroQty() {
            FulfillOrderFromInventory.Order order = FulfillOrderFromInventory.Order.parse("J:0 H:2");
            List<String> result = system.findProperCities(order);
            // H >= 2: Van(2), Mon(5), Cal(18), Hali(2)
            assertEquals(Arrays.asList("Van", "Mon", "Cal", "Hali"), result);
        }

        // ----- Corner case: unknown merchant -> exception -----

        @Test
        @DisplayName("unknown merchant code throws IllegalArgumentException")
        void testUnknownMerchantThrows() {
            assertThrows(IllegalArgumentException.class, () -> {
                FulfillOrderFromInventory.Order.parse("X:5");
            });
        }
    }
}
