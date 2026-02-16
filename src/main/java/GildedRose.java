import java.util.HashMap;
import java.util.Map;

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

    // ==================== Strategy Interface ====================

    interface QualityStrategy {
        void changeQuality(Item item);

        default void clampQuality(Item item) {
            if (item.quality < 0) item.quality = 0;
            if (item.quality > 50) item.quality = 50;
        }
    }

    // ==================== Strategy Implementations ====================

    static class DefaultStrategy implements QualityStrategy {
        @Override
        public void changeQuality(Item item) {
            item.sellIn--;
            item.quality--;
            if (item.sellIn < 0) {
                item.quality--;
            }
            clampQuality(item);
        }
    }

    static class AgedBrieStrategy implements QualityStrategy {
        @Override
        public void changeQuality(Item item) {
            item.sellIn--;
            item.quality++;
            if (item.sellIn < 0) {
                item.quality++;
            }
            clampQuality(item);
        }
    }

    static class BackstagePassesStrategy implements QualityStrategy {
        @Override
        public void changeQuality(Item item) {
            item.sellIn--;
            if (item.sellIn < 0) {
                item.quality = 0;
            } else if (item.sellIn < 5) {
                item.quality += 3;
            } else if (item.sellIn < 10) {
                item.quality += 2;
            } else {
                item.quality++;
            }
            clampQuality(item);
        }
    }

    static class SulfurasStrategy implements QualityStrategy {
        @Override
        public void changeQuality(Item item) {
            // Sulfuras never changes — quality stays at 80, sellIn never decreases
        }

        @Override
        public void clampQuality(Item item) {
            // Sulfuras is exempt — quality stays at 80
        }
    }

    static class ConjuredStrategy implements QualityStrategy {
        @Override
        public void changeQuality(Item item) {
            item.sellIn--;
            item.quality -= 2;
            if (item.sellIn < 0) {
                item.quality -= 2;
            }
            clampQuality(item);
        }
    }

    // ==================== Strategy Factory ====================

    static class StrategyFactory {
        private static final Map<String, QualityStrategy> strategies = new HashMap<>();

        static {
            strategies.put("Aged Brie", new AgedBrieStrategy());
            strategies.put("Backstage passes to a TAFKAL80ETC concert", new BackstagePassesStrategy());
            strategies.put("Sulfuras, Hand of Ragnaros", new SulfurasStrategy());
            strategies.put("Conjured Mana Cake", new ConjuredStrategy());
        }

        private static final QualityStrategy DEFAULT = new DefaultStrategy();

        static QualityStrategy getStrategyType(Item item) {
            if(item.name.startsWith("Conjured")) {
                return strategies.get("Conjured Mana Cake");
            }
            
            return strategies.getOrDefault(item.name, DEFAULT);
        }
    }

    // ==================== New Implementation ====================

    public static class NewGildedRose {
        Item[] items;

        public NewGildedRose(Item[] items) {
            this.items = items;
        }

        public void updateQuality() {
            for (Item item : items) {
                QualityStrategy strategy = StrategyFactory.getStrategyType(item);
                strategy.changeQuality(item);
            }
        }
    }
}
