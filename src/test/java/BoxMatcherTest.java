import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BoxMatcher Tests")
public class BoxMatcherTest {

    // =========================================================================
    // 1. Item Enum Tests
    // =========================================================================

    @Nested
    @DisplayName("Item Enum")
    class ItemEnumTests {

        @Test
        @DisplayName("Cam capacities: medium=1, large=2")
        void testCamCapacity() {
            assertEquals(1, BoxMatcher.Item.Cam.getMediumBoxCapacity());
            assertEquals(2, BoxMatcher.Item.Cam.getLargeBoxCapacity());
            assertTrue(BoxMatcher.Item.Cam.fitsInMedium());
        }

        @Test
        @DisplayName("Game capacities: medium=0, large=2")
        void testGameCapacity() {
            assertEquals(0, BoxMatcher.Item.Game.getMediumBoxCapacity());
            assertEquals(2, BoxMatcher.Item.Game.getLargeBoxCapacity());
            assertFalse(BoxMatcher.Item.Game.fitsInMedium());
        }

        @Test
        @DisplayName("Blue capacities: medium=0, large=1")
        void testBlueCapacity() {
            assertEquals(0, BoxMatcher.Item.Blue.getMediumBoxCapacity());
            assertEquals(1, BoxMatcher.Item.Blue.getLargeBoxCapacity());
            assertFalse(BoxMatcher.Item.Blue.fitsInMedium());
        }

        @Test
        @DisplayName("fromCode is case-insensitive")
        void testFromCodeCaseInsensitive() {
            assertEquals(BoxMatcher.Item.Cam, BoxMatcher.Item.fromCode("cam"));
            assertEquals(BoxMatcher.Item.Cam, BoxMatcher.Item.fromCode("CAM"));
            assertEquals(BoxMatcher.Item.Game, BoxMatcher.Item.fromCode("game"));
            assertEquals(BoxMatcher.Item.Blue, BoxMatcher.Item.fromCode("blue"));
        }

        @Test
        @DisplayName("fromCode returns null for unknown code")
        void testFromCodeUnknown() {
            assertNull(BoxMatcher.Item.fromCode("Phone"));
            assertNull(BoxMatcher.Item.fromCode("X"));
        }

        @Test
        @DisplayName("fromCode returns null for null/blank")
        void testFromCodeNullBlank() {
            assertNull(BoxMatcher.Item.fromCode(null));
            assertNull(BoxMatcher.Item.fromCode(""));
            assertNull(BoxMatcher.Item.fromCode("   "));
        }
    }

    // =========================================================================
    // 2. Box Tests
    // =========================================================================

    @Nested
    @DisplayName("Box")
    class BoxTests {

        @Test
        @DisplayName("box stores size, item type, and count")
        void testBoxFields() {
            BoxMatcher.Box box = new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Cam, 2);
            assertEquals(BoxMatcher.BoxSize.L, box.getSize());
            assertEquals(BoxMatcher.Item.Cam, box.getItemType());
            assertEquals(2, box.getCount());
        }

        @Test
        @DisplayName("toString formats correctly for large box with 2 items")
        void testToStringLarge() {
            BoxMatcher.Box box = new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Cam, 2);
            assertEquals("L: [\"Cam\", \"Cam\"]", box.toString());
        }

        @Test
        @DisplayName("toString formats correctly for medium box with 1 item")
        void testToStringMedium() {
            BoxMatcher.Box box = new BoxMatcher.Box(BoxMatcher.BoxSize.M, BoxMatcher.Item.Cam, 1);
            assertEquals("M: [\"Cam\"]", box.toString());
        }

        @Test
        @DisplayName("equals and hashCode work correctly")
        void testEquality() {
            BoxMatcher.Box a = new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 2);
            BoxMatcher.Box b = new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 2);
            BoxMatcher.Box c = new BoxMatcher.Box(BoxMatcher.BoxSize.M, BoxMatcher.Item.Game, 1);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertNotEquals(a, c);
        }
    }

    // =========================================================================
    // 3. matchBoxes — Examples from Problem Statement
    // =========================================================================

    @Nested
    @DisplayName("matchBoxes — Problem Examples")
    class ProblemExampleTests {

        private final BoxMatcher matcher = new BoxMatcher();

        @Test
        @DisplayName("[] -> []")
        void testEmptyInput() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Collections.emptyList());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("[Cam] -> [M: [Cam]]")
        void testSingleCam() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("Cam"));
            assertEquals(1, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.M, BoxMatcher.Item.Cam, 1), result.get(0));
        }

        @Test
        @DisplayName("[Cam, Game] -> [M: [Cam], L: [Game]]")
        void testCamAndGame() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("Cam", "Game"));
            assertEquals(2, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.M, BoxMatcher.Item.Cam, 1), result.get(0));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 1), result.get(1));
        }

        @Test
        @DisplayName("[Game, Blue] -> [L: [Game], L: [Blue]]")
        void testGameAndBlue() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("Game", "Blue"));
            assertEquals(2, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 1), result.get(0));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Blue, 1), result.get(1));
        }

        @Test
        @DisplayName("[Game, Game, Blue] -> [L: [Game, Game], L: [Blue]]")
        void testTwoGamesOneBlue() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("Game", "Game", "Blue"));
            assertEquals(2, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 2), result.get(0));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Blue, 1), result.get(1));
        }

        @Test
        @DisplayName("[Cam, Cam, Game, Game] -> [L: [Cam, Cam], L: [Game, Game]]")
        void testTwoCamsTwoGames() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(
                    Arrays.asList("Cam", "Cam", "Game", "Game"));
            assertEquals(2, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Cam, 2), result.get(0));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 2), result.get(1));
        }

        @Test
        @DisplayName("4 Cam, 3 Game, 1 Blue -> 2L Cam, 1L Game*2, 1L Game*1, 1L Blue")
        void testMixedLargeOrder() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(
                    Arrays.asList("Cam", "Cam", "Cam", "Game", "Game", "Game", "Cam", "Blue"));
            assertEquals(5, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Cam, 2), result.get(0));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Cam, 2), result.get(1));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 2), result.get(2));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 1), result.get(3));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Blue, 1), result.get(4));
        }

        @Test
        @DisplayName("5 Cam, 2 Game, 2 Blue -> 2L Cam, 1M Cam, 1L Game, 2L Blue")
        void testMixedWithMedium() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(
                    Arrays.asList("Cam", "Cam", "Cam", "Game", "Game", "Cam", "Cam", "Blue", "Blue"));
            assertEquals(6, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Cam, 2), result.get(0));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Cam, 2), result.get(1));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.M, BoxMatcher.Item.Cam, 1), result.get(2));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 2), result.get(3));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Blue, 1), result.get(4));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Blue, 1), result.get(5));
        }
    }

    // =========================================================================
    // 4. matchBoxes — Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("matchBoxes — Edge Cases")
    class EdgeCaseTests {

        private final BoxMatcher matcher = new BoxMatcher();

        @Test
        @DisplayName("null input -> empty result")
        void testNullInput() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("unknown item throws IllegalArgumentException")
        void testUnknownItem() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                matcher.matchBoxes(Arrays.asList("Phone"));
            });
            assertTrue(ex.getMessage().contains("Phone"));
        }

        @Test
        @DisplayName("single Game -> 1 large box")
        void testSingleGame() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("Game"));
            assertEquals(1, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 1), result.get(0));
        }

        @Test
        @DisplayName("single Blue -> 1 large box")
        void testSingleBlue() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("Blue"));
            assertEquals(1, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Blue, 1), result.get(0));
        }

        @Test
        @DisplayName("many Cams: 7 -> 3L(2) + 1M(1)")
        void testSevenCams() {
            List<String> input = new ArrayList<String>();
            for (int i = 0; i < 7; i++) input.add("Cam");
            List<BoxMatcher.Box> result = matcher.matchBoxes(input);
            assertEquals(4, result.size());
            // 3 large boxes of 2
            for (int i = 0; i < 3; i++) {
                assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Cam, 2), result.get(i));
            }
            // 1 medium box of 1
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.M, BoxMatcher.Item.Cam, 1), result.get(3));
        }

        @Test
        @DisplayName("many Games: 5 -> 2L(2) + 1L(1)")
        void testFiveGames() {
            List<String> input = new ArrayList<String>();
            for (int i = 0; i < 5; i++) input.add("Game");
            List<BoxMatcher.Box> result = matcher.matchBoxes(input);
            assertEquals(3, result.size());
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 2), result.get(0));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 2), result.get(1));
            assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Game, 1), result.get(2));
        }

        @Test
        @DisplayName("many Blues: 3 -> 3L(1)")
        void testThreeBlues() {
            List<String> input = Arrays.asList("Blue", "Blue", "Blue");
            List<BoxMatcher.Box> result = matcher.matchBoxes(input);
            assertEquals(3, result.size());
            for (BoxMatcher.Box box : result) {
                assertEquals(new BoxMatcher.Box(BoxMatcher.BoxSize.L, BoxMatcher.Item.Blue, 1), box);
            }
        }

        @Test
        @DisplayName("order of item types in output follows first appearance in input")
        void testOutputOrder() {
            // Blue appears first, then Cam
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("Blue", "Cam"));
            assertEquals(2, result.size());
            assertEquals(BoxMatcher.Item.Blue, result.get(0).getItemType());
            assertEquals(BoxMatcher.Item.Cam, result.get(1).getItemType());
        }

        @Test
        @DisplayName("case-insensitive item codes in input")
        void testCaseInsensitiveInput() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("cam", "GAME"));
            assertEquals(2, result.size());
            assertEquals(BoxMatcher.Item.Cam, result.get(0).getItemType());
            assertEquals(BoxMatcher.Item.Game, result.get(1).getItemType());
        }

        @Test
        @DisplayName("even number of Cams uses only large boxes")
        void testEvenCamsNoMedium() {
            List<BoxMatcher.Box> result = matcher.matchBoxes(Arrays.asList("Cam", "Cam", "Cam", "Cam"));
            assertEquals(2, result.size());
            for (BoxMatcher.Box box : result) {
                assertEquals(BoxMatcher.BoxSize.L, box.getSize());
                assertEquals(2, box.getCount());
            }
        }
    }
}
