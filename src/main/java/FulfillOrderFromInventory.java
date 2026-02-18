import java.util.*;

public class FulfillOrderFromInventory {

    // -------------------------------------------------------------------------
    // Enum: Merchant (sauce types)
    // -------------------------------------------------------------------------
    enum Merchant {
        J, H, S;

        /**
         * Case-insensitive factory. Returns null for unknown or blank codes.
         */
        public static Merchant fromCode(String code) {
            if (code == null || code.trim().isEmpty()) return null;
            try {
                return Merchant.valueOf(code.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: InventorySystem
    // -------------------------------------------------------------------------
    static class InventorySystem {

        // <city, <Merchant, stock>>
        private final Map<String, Map<Merchant, Integer>> inventory;

        // city full-name -> abbreviation for display
        private final Map<String, String> cityAbbreviations;

        public InventorySystem() {
            inventory = new LinkedHashMap<String, Map<Merchant, Integer>>();
            cityAbbreviations = new LinkedHashMap<String, String>();
            initData();
        }

        private void initData() {
            Map<Merchant, Integer> stock;

            stock = new EnumMap<Merchant, Integer>(Merchant.class);
            stock.put(Merchant.J, 5);  stock.put(Merchant.H, 0);  stock.put(Merchant.S, 0);
            addCity("Toronto", "Tor", stock);

            stock = new EnumMap<Merchant, Integer>(Merchant.class);
            stock.put(Merchant.J, 10); stock.put(Merchant.H, 2);  stock.put(Merchant.S, 6);
            addCity("Vancouver", "Van", stock);

            stock = new EnumMap<Merchant, Integer>(Merchant.class);
            stock.put(Merchant.J, 3);  stock.put(Merchant.H, 5);  stock.put(Merchant.S, 5);
            addCity("Montreal", "Mon", stock);

            stock = new EnumMap<Merchant, Integer>(Merchant.class);
            stock.put(Merchant.J, 1);  stock.put(Merchant.H, 18); stock.put(Merchant.S, 2);
            addCity("Calgary", "Cal", stock);

            stock = new EnumMap<Merchant, Integer>(Merchant.class);
            stock.put(Merchant.J, 28); stock.put(Merchant.H, 2);  stock.put(Merchant.S, 12);
            addCity("Halifax", "Hali", stock);
        }

        private void addCity(String city, String abbrev, Map<Merchant, Integer> stock) {
            cityAbbreviations.put(city, abbrev);
            inventory.put(city, new EnumMap<Merchant, Integer>(stock));
        }

        /**
         * Returns the stock count for a given city and merchant.
         * Returns 0 if the city or merchant is not found.
         */
        public int getStock(String city, Merchant merchant) {
            Map<Merchant, Integer> cityStock = inventory.get(city);
            if (cityStock == null) return 0;
            return cityStock.getOrDefault(merchant, 0);
        }

        /** All registered city names (insertion-ordered). */
        public Set<String> getCities() {
            return Collections.unmodifiableSet(inventory.keySet());
        }

        /** Returns the short display name (e.g. "Vancouver" -> "Van"). */
        public String getDisplayName(String city) {
            return cityAbbreviations.getOrDefault(city, city);
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: Order
    // -------------------------------------------------------------------------
    static class Order {

        private final Map<Merchant, Integer> items;

        private Order(Map<Merchant, Integer> items) {
            this.items = items; // wrapped in unmodifiableMap by the factory
        }

        /**
         * Parses an order string like "J:3. H:2 s:4".
         * Tokens are split on whitespace, commas, and periods.
         *
         * Validation rules:
         *  - Unknown merchant codes (not in Enum) cause an IllegalArgumentException.
         *  - Malformed numbers, negative quantities, or qty == 0 are silently ignored.
         *  - If the same merchant appears twice, the last occurrence wins.
         *
         * @throws IllegalArgumentException if any token has a merchant code not in the Enum
         */
        public static Order parse(String input) {
            Map<Merchant, Integer> items = new EnumMap<Merchant, Integer>(Merchant.class);

            if (input == null || input.trim().isEmpty()) {
                return new Order(Collections.unmodifiableMap(items));
            }

            // Split on any combination of whitespace, comma, or period
            String[] tokens = input.trim().split("[\\s,\\.]+");

            for (String token : tokens) {
                if (!isValidToken(token)) continue;

                String[] parts = token.split(":", 2);
                String code = parts[0].trim();
                String valueStr = parts[1].trim();

                // Check if the code looks like a letter but is not a known merchant
                // -> throw exception per spec
                Merchant merchant = Merchant.fromCode(code);
                if (merchant == null) {
                    throw new IllegalArgumentException(
                            "Unknown merchant code in order: '" + code + "'");
                }

                int qty;
                try {
                    qty = Integer.parseInt(valueStr);
                } catch (NumberFormatException e) {
                    continue; // malformed number -> ignore
                }

                if (qty <= 0) continue; // zero or negative -> ignore

                items.put(merchant, qty);
            }

            return new Order(Collections.unmodifiableMap(items));
        }

        /**
         * A valid token must contain exactly one ':' with a non-empty code on the
         * left and a non-empty value on the right.
         */
        private static boolean isValidToken(String token) {
            if (token == null || token.trim().isEmpty()) return false;
            String[] parts = token.split(":", 2);
            if (parts.length != 2) return false;
            return !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty();
        }

        /**
         * Returns the order items. The map is unmodifiable -- set once at parse
         * time and never mutated, so no defensive copy is needed.
         */
        public Map<Merchant, Integer> getItems() {
            return items;
        }

        /** True if the order has no valid line items (all zero/negative or empty input). */
        public boolean isEmpty() {
            return items.isEmpty();
        }

        @Override
        public String toString() {
            return "Order" + items;
        }
    }

    // -------------------------------------------------------------------------
    // Core: FulfillOrderFromInventory
    // -------------------------------------------------------------------------
    private final InventorySystem inventorySystem;

    public FulfillOrderFromInventory() {
        this.inventorySystem = new InventorySystem();
    }

    /**
     * Returns the display names of all cities whose inventory can fully satisfy
     * the given order.
     *
     * Corner cases:
     *  1) If a merchant in the order has qty <= 0, that merchant is ignored
     *     (already filtered out by Order.parse, but guarded here too).
     *  2) If the order is empty (all merchants had qty <= 0 or input was blank),
     *     returns ALL cities and prints an alert.
     *  3) Unknown merchant codes cause an exception at parse time, before this
     *     method is reached.
     */
    public List<String> findProperCities(Order order) {
        // Corner case: empty order -> return all cities with an alert
        if (order.isEmpty()) {
            System.out.println("[Alert] Order is empty — returning all cities.");
            List<String> allCities = new ArrayList<String>();
            for (String city : inventorySystem.getCities()) {
                allCities.add(inventorySystem.getDisplayName(city));
            }
            return allCities;
        }

        Map<Merchant, Integer> orderItems = order.getItems();
        List<String> result = new ArrayList<String>();

        for (String city : inventorySystem.getCities()) {
            boolean canFulfill = true;

            for (Map.Entry<Merchant, Integer> entry : orderItems.entrySet()) {
                int required = entry.getValue();
                if (required <= 0) continue; // defensive: skip zero/negative

                if (inventorySystem.getStock(city, entry.getKey()) < required) {
                    canFulfill = false;
                    break;
                }
            }

            if (canFulfill) {
                result.add(inventorySystem.getDisplayName(city));
            }
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        FulfillOrderFromInventory system = new FulfillOrderFromInventory();

        String[] inputs = {
            "J:3. H:2 s:4",   // normal: Van, Mon, Hali
            "H:7 S:1",        // single city: Cal
            "J:0 H:0 S:0",   // all zero -> empty order -> all cities + alert
            "",                // empty input -> all cities + alert
            "J:100 H:100",    // no city can fulfill
        };

        for (String input : inputs) {
            Order order = Order.parse(input);
            System.out.println("Input : \"" + input + "\"");
            System.out.println("Parsed: " + order);
            List<String> result = system.findProperCities(order);
            System.out.println("Output: " + (result.isEmpty() ? "(none)" : String.join(", ", result)));
            System.out.println();
        }
    }
}
