import java.util.*;

public class BoxMatcher {

    // -------------------------------------------------------------------------
    // Enum: BoxSize
    // -------------------------------------------------------------------------
    enum BoxSize {
        M, L;
    }

    // -------------------------------------------------------------------------
    // Enum: Item (product types with box capacity info)
    // -------------------------------------------------------------------------
    enum Item {
        Cam(1, 2),
        Game(0, 2),
        Blue(0, 1);

        private final int mediumBoxCapacity;
        private final int largeBoxCapacity;

        Item(int mediumBoxCapacity, int largeBoxCapacity) {
            this.mediumBoxCapacity = mediumBoxCapacity;
            this.largeBoxCapacity = largeBoxCapacity;
        }

        public int getMediumBoxCapacity() {
            return mediumBoxCapacity;
        }

        public int getLargeBoxCapacity() {
            return largeBoxCapacity;
        }

        public boolean fitsInMedium() {
            return mediumBoxCapacity > 0;
        }

        /**
         * Case-insensitive lookup. Returns null if the code is unknown.
         */
        public static Item fromCode(String code) {
            if (code == null || code.trim().isEmpty()) return null;
            String trimmed = code.trim();
            for (Item item : values()) {
                if (item.name().equalsIgnoreCase(trimmed)) {
                    return item;
                }
            }
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: Box (one packed box in the result)
    // -------------------------------------------------------------------------
    static class Box {
        private final BoxSize size;
        private final Item itemType;
        private final int count;

        public Box(BoxSize size, Item itemType, int count) {
            this.size = size;
            this.itemType = itemType;
            this.count = count;
        }

        public BoxSize getSize() { return size; }
        public Item getItemType() { return itemType; }
        public int getCount() { return count; }

        /**
         * Format: "L: [\"Cam\", \"Cam\"]" or "M: [\"Cam\"]"
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(size.name()).append(": [");
            for (int i = 0; i < count; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(itemType.name()).append("\"");
            }
            sb.append("]");
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Box box = (Box) o;
            return count == box.count && size == box.size && itemType == box.itemType;
        }

        @Override
        public int hashCode() {
            int result = size.hashCode();
            result = 31 * result + itemType.hashCode();
            result = 31 * result + count;
            return result;
        }
    }

    // -------------------------------------------------------------------------
    // Core logic
    // -------------------------------------------------------------------------

    /**
     * Takes a list of item identifier strings and returns the list of packed
     * boxes. Boxes contain only one type of product. The algorithm minimizes
     * box usage by filling large boxes first, then using medium for leftovers
     * when the item fits in a medium box.
     *
     * @throws IllegalArgumentException if any item code is unrecognized
     */
    public List<Box> matchBoxes(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        // Count items, preserving the order of first appearance
        Map<Item, Integer> itemCounts = countItems(input);

        List<Box> result = new ArrayList<Box>();
        for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
            List<Box> boxes = packItem(entry.getKey(), entry.getValue());
            result.addAll(boxes);
        }
        return result;
    }

    /**
     * Counts items from the input list, preserving first-appearance order.
     *
     * @throws IllegalArgumentException if an unknown item code is encountered
     */
    private Map<Item, Integer> countItems(List<String> input) {
        Map<Item, Integer> counts = new LinkedHashMap<Item, Integer>();
        for (String code : input) {
            Item item = Item.fromCode(code);
            if (item == null) {
                throw new IllegalArgumentException(
                        "Unknown item code: '" + code + "'");
            }
            counts.put(item, counts.getOrDefault(item, 0) + 1);
        }
        return counts;
    }

    /**
     * Packs a given count of a single item type into boxes.
     *
     * Strategy:
     *   1. Fill large boxes to capacity (each holds item.largeBoxCapacity).
     *   2. If there's a remainder and the item fits in a medium box, use medium.
     *   3. Otherwise, put the remainder in one more large box.
     */
    private List<Box> packItem(Item item, int count) {
        List<Box> boxes = new ArrayList<Box>();
        if (count <= 0) return boxes;

        int largeCap = item.getLargeBoxCapacity();
        int fullLargeBoxes = count / largeCap;
        int remainder = count % largeCap;

        // Step 1: full large boxes
        for (int i = 0; i < fullLargeBoxes; i++) {
            boxes.add(new Box(BoxSize.L, item, largeCap));
        }

        // Step 2: handle remainder
        if (remainder > 0) {
            if (item.fitsInMedium() && remainder <= item.getMediumBoxCapacity()) {
                boxes.add(new Box(BoxSize.M, item, remainder));
            } else {
                boxes.add(new Box(BoxSize.L, item, remainder));
            }
        }

        return boxes;
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        BoxMatcher matcher = new BoxMatcher();

        List<List<String>> testInputs = new ArrayList<List<String>>();
        testInputs.add(Collections.emptyList());
        testInputs.add(Arrays.asList("Cam"));
        testInputs.add(Arrays.asList("Cam", "Game"));
        testInputs.add(Arrays.asList("Game", "Blue"));
        testInputs.add(Arrays.asList("Game", "Game", "Blue"));
        testInputs.add(Arrays.asList("Cam", "Cam", "Game", "Game"));
        testInputs.add(Arrays.asList("Cam", "Cam", "Cam", "Game", "Game", "Game", "Cam", "Blue"));
        testInputs.add(Arrays.asList("Cam", "Cam", "Cam", "Game", "Game", "Cam", "Cam", "Blue", "Blue"));

        for (List<String> input : testInputs) {
            List<Box> result = matcher.matchBoxes(input);
            System.out.println("Input : " + input);
            System.out.println("Output: " + result);
            System.out.println();
        }
    }
}
