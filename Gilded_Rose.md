/**
 * Gilded Rose Kata - Inventory Management System
 * 
 * A classic refactoring exercise. The inn sells items that degrade in quality over time.
 * 
 * Rules:
 * - All items have a sellIn value (days to sell) and quality value (0-50)
 * - At end of each day, both values decrease for most items
 * - Once sellIn date passes, quality degrades twice as fast
 * - Quality is never negative and never more than 50 (except Sulfuras = 80)
 * 
 * Special Items:
 * - "Aged Brie": increases in quality the older it gets
 * - "Backstage passes": quality increases as sellIn approaches:
 *   +1 normally, +2 when ≤10 days, +3 when ≤5 days, drops to 0 after concert
 * - "Sulfuras": legendary item, never sold, never decreases (quality = 80)
 * - "Conjured": degrades twice as fast as normal items
 */
public class GildedRose {

    // ==================== Item Class (Cannot be modified per kata rules) ====================
    
    /**
     * Represents an item in the inn's inventory.
     * This class cannot be modified (legacy constraint from the goblin).
     */
    public static class Item {
        public String name;
        public int sellIn;
        public int quality;

        public Item(String name, int sellIn, int quality) {
            this.name = name;
            this.sellIn = sellIn;
            this.quality = quality;
        }

        @Override
        public String toString() {
            return name + ", " + sellIn + ", " + quality;
        }
    }

    // ==================== Legacy Code (The messy original) ====================
    
    /**
     * Legacy implementation with deeply nested conditionals.
     * This is the "before" code that needs refactoring.
     */
    public static class LegacyGildedRose {
        Item[] items;

        public LegacyGildedRose(Item[] items) {
            this.items = items;
        }

        public void updateQuality() {
            for (int i = 0; i < items.length; i++) {
                if (!items[i].name.equals("Aged Brie")
                        && !items[i].name.equals("Backstage passes to a TAFKAL80ETC concert")) {
                    if (items[i].quality > 0) {
                        if (!items[i].name.equals("Sulfuras, Hand of Ragnaros")) {
                            items[i].quality = items[i].quality - 1;
                        }
                    }
                } else {
                    if (items[i].quality < 50) {
                        items[i].quality = items[i].quality + 1;

                        if (items[i].name.equals("Backstage passes to a TAFKAL80ETC concert")) {
                            if (items[i].sellIn < 11) {
                                if (items[i].quality < 50) {
                                    items[i].quality = items[i].quality + 1;
                                }
                            }

                            if (items[i].sellIn < 6) {
                                if (items[i].quality < 50) {
                                    items[i].quality = items[i].quality + 1;
                                }
                            }
                        }
                    }
                }

                if (!items[i].name.equals("Sulfuras, Hand of Ragnaros")) {
                    items[i].sellIn = items[i].sellIn - 1;
                }

                if (items[i].sellIn < 0) {
                    if (!items[i].name.equals("Aged Brie")) {
                        if (!items[i].name.equals("Backstage passes to a TAFKAL80ETC concert")) {
                            if (items[i].quality > 0) {
                                if (!items[i].name.equals("Sulfuras, Hand of Ragnaros")) {
                                    items[i].quality = items[i].quality - 1;
                                }
                            }
                        } else {
                            items[i].quality = items[i].quality - items[i].quality;
                        }
                    } else {
                        if (items[i].quality < 50) {
                            items[i].quality = items[i].quality + 1;
                        }
                    }
                }
            }
        }
    }
}