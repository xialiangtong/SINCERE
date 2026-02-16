import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GildedRose Tests")
public class GildedRoseTest {

    private GildedRose.Item item(String name, int sellIn, int quality) {
        return new GildedRose.Item(name, sellIn, quality);
    }

    // ==================== StrategyFactory Tests ====================

    @Nested
    @DisplayName("StrategyFactory Tests")
    class StrategyFactoryTests {

        @Test
        @DisplayName("returns DefaultStrategy for unknown item")
        void testDefaultStrategy() {
            GildedRose.Item i = item("Random Item", 10, 20);
            assertTrue(GildedRose.StrategyFactory.getStrategyType(i) instanceof GildedRose.DefaultStrategy);
        }

        @Test
        @DisplayName("returns AgedBrieStrategy for Aged Brie")
        void testAgedBrieStrategy() {
            GildedRose.Item i = item("Aged Brie", 10, 20);
            assertTrue(GildedRose.StrategyFactory.getStrategyType(i) instanceof GildedRose.AgedBrieStrategy);
        }

        @Test
        @DisplayName("returns BackstagePassesStrategy for Backstage passes")
        void testBackstagePassesStrategy() {
            GildedRose.Item i = item("Backstage passes to a TAFKAL80ETC concert", 10, 20);
            assertTrue(GildedRose.StrategyFactory.getStrategyType(i) instanceof GildedRose.BackstagePassesStrategy);
        }

        @Test
        @DisplayName("returns SulfurasStrategy for Sulfuras")
        void testSulfurasStrategy() {
            GildedRose.Item i = item("Sulfuras, Hand of Ragnaros", 0, 80);
            assertTrue(GildedRose.StrategyFactory.getStrategyType(i) instanceof GildedRose.SulfurasStrategy);
        }

        @Test
        @DisplayName("returns ConjuredStrategy for Conjured Mana Cake")
        void testConjuredStrategy() {
            GildedRose.Item i = item("Conjured Mana Cake", 10, 20);
            assertTrue(GildedRose.StrategyFactory.getStrategyType(i) instanceof GildedRose.ConjuredStrategy);
        }

        @Test
        @DisplayName("returns same singleton instance for same item type")
        void testSingleton() {
            GildedRose.Item i1 = item("Aged Brie", 10, 20);
            GildedRose.Item i2 = item("Aged Brie", 5, 30);
            assertSame(GildedRose.StrategyFactory.getStrategyType(i1),
                       GildedRose.StrategyFactory.getStrategyType(i2));
        }
    }

    // ==================== NewGildedRose: Normal Item Tests ====================

    @Nested
    @DisplayName("Normal Item Tests")
    class NormalItemTests {

        @Test
        @DisplayName("quality and sellIn decrease by 1")
        void testNormalDegrade() {
            GildedRose.Item[] items = { item("Normal Item", 10, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(9, items[0].sellIn);
            assertEquals(19, items[0].quality);
        }

        @Test
        @DisplayName("quality degrades twice as fast after sell date")
        void testNormalDegradePastSellDate() {
            GildedRose.Item[] items = { item("Normal Item", 0, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(-1, items[0].sellIn);
            assertEquals(18, items[0].quality);
        }

        @Test
        @DisplayName("quality never goes below 0")
        void testNormalQualityFloor() {
            GildedRose.Item[] items = { item("Normal Item", 5, 0) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(0, items[0].quality);
        }

        @Test
        @DisplayName("quality never goes below 0 after sell date")
        void testNormalQualityFloorPastSellDate() {
            GildedRose.Item[] items = { item("Normal Item", 0, 1) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(0, items[0].quality);
        }
    }

    // ==================== NewGildedRose: Aged Brie Tests ====================

    @Nested
    @DisplayName("Aged Brie Tests")
    class AgedBrieTests {

        @Test
        @DisplayName("quality increases by 1")
        void testAgedBrieIncreases() {
            GildedRose.Item[] items = { item("Aged Brie", 10, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(9, items[0].sellIn);
            assertEquals(21, items[0].quality);
        }

        @Test
        @DisplayName("quality increases by 2 after sell date")
        void testAgedBrieIncreasesPastSellDate() {
            GildedRose.Item[] items = { item("Aged Brie", 0, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(22, items[0].quality);
        }

        @Test
        @DisplayName("quality never exceeds 50")
        void testAgedBrieQualityCap() {
            GildedRose.Item[] items = { item("Aged Brie", 10, 50) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(50, items[0].quality);
        }

        @Test
        @DisplayName("quality capped at 50 even after sell date")
        void testAgedBrieQualityCapPastSellDate() {
            GildedRose.Item[] items = { item("Aged Brie", 0, 49) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(50, items[0].quality);
        }
    }

    // ==================== NewGildedRose: Backstage Passes Tests ====================

    @Nested
    @DisplayName("Backstage Passes Tests")
    class BackstagePassesTests {

        @Test
        @DisplayName("quality increases by 1 when sellIn > 10")
        void testBackstageMoreThan10() {
            GildedRose.Item[] items = { item("Backstage passes to a TAFKAL80ETC concert", 15, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(21, items[0].quality);
        }

        @Test
        @DisplayName("quality increases by 2 when sellIn is 10")
        void testBackstageSellIn10() {
            GildedRose.Item[] items = { item("Backstage passes to a TAFKAL80ETC concert", 10, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(22, items[0].quality);
        }

        @Test
        @DisplayName("quality increases by 2 when sellIn is 6")
        void testBackstageSellIn6() {
            GildedRose.Item[] items = { item("Backstage passes to a TAFKAL80ETC concert", 6, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(22, items[0].quality);
        }

        @Test
        @DisplayName("quality increases by 3 when sellIn is 5")
        void testBackstageSellIn5() {
            GildedRose.Item[] items = { item("Backstage passes to a TAFKAL80ETC concert", 5, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(23, items[0].quality);
        }

        @Test
        @DisplayName("quality increases by 3 when sellIn is 1")
        void testBackstageSellIn1() {
            GildedRose.Item[] items = { item("Backstage passes to a TAFKAL80ETC concert", 1, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(23, items[0].quality);
        }

        @Test
        @DisplayName("quality drops to 0 after concert (sellIn = 0)")
        void testBackstageAfterConcert() {
            GildedRose.Item[] items = { item("Backstage passes to a TAFKAL80ETC concert", 0, 50) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(0, items[0].quality);
        }

        @Test
        @DisplayName("quality never exceeds 50")
        void testBackstageQualityCap() {
            GildedRose.Item[] items = { item("Backstage passes to a TAFKAL80ETC concert", 3, 49) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(50, items[0].quality);
        }
    }

    // ==================== NewGildedRose: Sulfuras Tests ====================

    @Nested
    @DisplayName("Sulfuras Tests")
    class SulfurasTests {

        @Test
        @DisplayName("quality stays at 80")
        void testSulfurasQualityUnchanged() {
            GildedRose.Item[] items = { item("Sulfuras, Hand of Ragnaros", 0, 80) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(80, items[0].quality);
        }

        @Test
        @DisplayName("sellIn never decreases")
        void testSulfurasSellInUnchanged() {
            GildedRose.Item[] items = { item("Sulfuras, Hand of Ragnaros", 5, 80) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(5, items[0].sellIn);
        }

        @Test
        @DisplayName("quality stays at 80 over multiple days")
        void testSulfurasMultipleDays() {
            GildedRose.Item[] items = { item("Sulfuras, Hand of Ragnaros", 0, 80) };
            GildedRose.NewGildedRose app = new GildedRose.NewGildedRose(items);
            for (int i = 0; i < 10; i++) app.updateQuality();
            assertEquals(80, items[0].quality);
            assertEquals(0, items[0].sellIn);
        }
    }

    // ==================== NewGildedRose: Conjured Tests ====================

    @Nested
    @DisplayName("Conjured Tests")
    class ConjuredTests {

        @Test
        @DisplayName("quality degrades by 2 before sell date")
        void testConjuredDegrade() {
            GildedRose.Item[] items = { item("Conjured Mana Cake", 10, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(9, items[0].sellIn);
            assertEquals(18, items[0].quality);
        }

        @Test
        @DisplayName("quality degrades by 4 after sell date")
        void testConjuredDegradePastSellDate() {
            GildedRose.Item[] items = { item("Conjured Mana Cake", 0, 20) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(16, items[0].quality);
        }

        @Test
        @DisplayName("quality never goes below 0")
        void testConjuredQualityFloor() {
            GildedRose.Item[] items = { item("Conjured Mana Cake", 5, 1) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(0, items[0].quality);
        }

        @Test
        @DisplayName("quality never goes below 0 after sell date")
        void testConjuredQualityFloorPastSellDate() {
            GildedRose.Item[] items = { item("Conjured Mana Cake", 0, 3) };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(0, items[0].quality);
        }
    }

    // ==================== NewGildedRose: Multiple Items Tests ====================

    @Nested
    @DisplayName("Multiple Items Tests")
    class MultipleItemsTests {

        @Test
        @DisplayName("updates all items in array")
        void testMultipleItems() {
            GildedRose.Item[] items = {
                item("Normal Item", 10, 20),
                item("Aged Brie", 10, 20),
                item("Sulfuras, Hand of Ragnaros", 0, 80),
                item("Backstage passes to a TAFKAL80ETC concert", 15, 20),
                item("Conjured Mana Cake", 10, 20)
            };
            new GildedRose.NewGildedRose(items).updateQuality();
            assertEquals(19, items[0].quality); // Normal: -1
            assertEquals(21, items[1].quality); // Aged Brie: +1
            assertEquals(80, items[2].quality); // Sulfuras: unchanged
            assertEquals(21, items[3].quality); // Backstage: +1
            assertEquals(18, items[4].quality); // Conjured: -2
        }

        @Test
        @DisplayName("empty items array does not throw")
        void testEmptyItems() {
            GildedRose.Item[] items = {};
            assertDoesNotThrow(() -> new GildedRose.NewGildedRose(items).updateQuality());
        }

        @Test
        @DisplayName("multiple days update correctly")
        void testMultipleDays() {
            GildedRose.Item[] items = { item("Normal Item", 5, 10) };
            GildedRose.NewGildedRose app = new GildedRose.NewGildedRose(items);
            for (int i = 0; i < 5; i++) app.updateQuality();
            assertEquals(0, items[0].sellIn);
            assertEquals(5, items[0].quality);
        }

        @Test
        @DisplayName("normal item over many days reaches 0 quality")
        void testNormalItemDegradesToZero() {
            GildedRose.Item[] items = { item("Normal Item", 5, 10) };
            GildedRose.NewGildedRose app = new GildedRose.NewGildedRose(items);
            for (int i = 0; i < 20; i++) app.updateQuality();
            assertEquals(0, items[0].quality);
        }
    }

    // ==================== Legacy vs New Comparison Tests ====================

    @Nested
    @DisplayName("Legacy vs New Comparison Tests")
    class ComparisonTests {

        private void assertSameBehavior(String name, int sellIn, int quality) {
            GildedRose.Item legacyItem = item(name, sellIn, quality);
            GildedRose.Item newItem = item(name, sellIn, quality);

            new GildedRose.LegacyGildedRose(new GildedRose.Item[]{ legacyItem }).updateQuality();
            new GildedRose.NewGildedRose(new GildedRose.Item[]{ newItem }).updateQuality();

            assertEquals(legacyItem.sellIn, newItem.sellIn,
                    name + " sellIn mismatch at initial sellIn=" + sellIn + ", quality=" + quality);
            assertEquals(legacyItem.quality, newItem.quality,
                    name + " quality mismatch at initial sellIn=" + sellIn + ", quality=" + quality);
        }

        @Test
        @DisplayName("normal item matches legacy")
        void testNormalMatchesLegacy() {
            assertSameBehavior("Normal Item", 10, 20);
            assertSameBehavior("Normal Item", 0, 20);
            assertSameBehavior("Normal Item", -1, 20);
            assertSameBehavior("Normal Item", 5, 0);
            assertSameBehavior("Normal Item", 0, 1);
        }

        @Test
        @DisplayName("Aged Brie matches legacy")
        void testAgedBrieMatchesLegacy() {
            assertSameBehavior("Aged Brie", 10, 20);
            assertSameBehavior("Aged Brie", 0, 20);
            assertSameBehavior("Aged Brie", -1, 48);
            assertSameBehavior("Aged Brie", 5, 50);
            assertSameBehavior("Aged Brie", 0, 49);
        }

        @Test
        @DisplayName("Backstage passes matches legacy")
        void testBackstageMatchesLegacy() {
            assertSameBehavior("Backstage passes to a TAFKAL80ETC concert", 15, 20);
            assertSameBehavior("Backstage passes to a TAFKAL80ETC concert", 11, 20);
            assertSameBehavior("Backstage passes to a TAFKAL80ETC concert", 10, 20);
            assertSameBehavior("Backstage passes to a TAFKAL80ETC concert", 6, 20);
            assertSameBehavior("Backstage passes to a TAFKAL80ETC concert", 5, 20);
            assertSameBehavior("Backstage passes to a TAFKAL80ETC concert", 1, 20);
            assertSameBehavior("Backstage passes to a TAFKAL80ETC concert", 0, 50);
            assertSameBehavior("Backstage passes to a TAFKAL80ETC concert", 5, 49);
        }

        @Test
        @DisplayName("Sulfuras matches legacy")
        void testSulfurasMatchesLegacy() {
            assertSameBehavior("Sulfuras, Hand of Ragnaros", 0, 80);
            assertSameBehavior("Sulfuras, Hand of Ragnaros", 5, 80);
            assertSameBehavior("Sulfuras, Hand of Ragnaros", -1, 80);
        }

        @Test
        @DisplayName("multiple days comparison matches legacy")
        void testMultipleDaysMatchesLegacy() {
            String[] names = { "Normal Item", "Aged Brie",
                    "Backstage passes to a TAFKAL80ETC concert",
                    "Sulfuras, Hand of Ragnaros" };
            int[][] configs = { {10, 20}, {5, 48}, {3, 20}, {0, 80} };

            for (int n = 0; n < names.length; n++) {
                GildedRose.Item legacyItem = item(names[n], configs[n][0], configs[n][1]);
                GildedRose.Item newItem = item(names[n], configs[n][0], configs[n][1]);
                GildedRose.LegacyGildedRose legacy = new GildedRose.LegacyGildedRose(new GildedRose.Item[]{ legacyItem });
                GildedRose.NewGildedRose newer = new GildedRose.NewGildedRose(new GildedRose.Item[]{ newItem });

                for (int day = 0; day < 30; day++) {
                    legacy.updateQuality();
                    newer.updateQuality();
                    assertEquals(legacyItem.sellIn, newItem.sellIn,
                            names[n] + " sellIn mismatch on day " + (day + 1));
                    assertEquals(legacyItem.quality, newItem.quality,
                            names[n] + " quality mismatch on day " + (day + 1));
                }
            }
        }
    }
}
