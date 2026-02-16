import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Compare4Solution Tests")
public class Compare4SolutionTest {

    private Set<String> defaultDic() {
        return new HashSet<>(Arrays.asList(
                "AABC", "ABCD", "ADAA", "BARK", "BIKE", "BOOK",
                "CARD", "CODE", "DEMO", "JAVA", "WORD", "TEST"
        ));
    }

    // ==================== CompareResult Tests ====================

    @Nested
    @DisplayName("CompareResult Tests")
    class CompareResultTests {

        @Test
        @DisplayName("isCorrect returns true when all EXACT")
        void testIsCorrectAllExact() {
            int[] result = {1, 1, 1, 1};
            Compare4Solution.CompareResult cr = new Compare4Solution.CompareResult("JAVA", result);
            assertTrue(cr.isCorrect());
        }

        @Test
        @DisplayName("isCorrect returns false when not all EXACT")
        void testIsCorrectNotAllExact() {
            int[] result = {1, 0, -1, 1};
            Compare4Solution.CompareResult cr = new Compare4Solution.CompareResult("JAVA", result);
            assertFalse(cr.isCorrect());
        }

        @Test
        @DisplayName("isCorrect returns false when all ABSENT")
        void testIsCorrectAllAbsent() {
            int[] result = {-1, -1, -1, -1};
            Compare4Solution.CompareResult cr = new Compare4Solution.CompareResult("XXXX", result);
            assertFalse(cr.isCorrect());
        }

        @Test
        @DisplayName("getGuess returns the guess string")
        void testGetGuess() {
            Compare4Solution.CompareResult cr = new Compare4Solution.CompareResult("JAVA", new int[]{1, 1, 1, 1});
            assertEquals("JAVA", cr.getGuess());
        }

        @Test
        @DisplayName("getResult returns a copy of the result array")
        void testGetResultCopy() {
            int[] result = {1, 0, -1, 1};
            Compare4Solution.CompareResult cr = new Compare4Solution.CompareResult("JAVA", result);
            int[] returned = cr.getResult();
            assertArrayEquals(result, returned);
            // Modifying returned copy should not affect internal state
            returned[0] = -1;
            assertEquals(1, cr.getResult()[0]);
        }

        @Test
        @DisplayName("constructor copies the result array")
        void testConstructorCopiesArray() {
            int[] result = {1, 0, -1, 1};
            Compare4Solution.CompareResult cr = new Compare4Solution.CompareResult("JAVA", result);
            result[0] = -1; // mutate original
            assertEquals(1, cr.getResult()[0]); // internal not affected
        }

        @Test
        @DisplayName("toString produces readable output")
        void testToString() {
            int[] result = {1, 0, -1, 1};
            Compare4Solution.CompareResult cr = new Compare4Solution.CompareResult("JAVA", result);
            String str = cr.toString();
            assertTrue(str.contains("JAVA"));
            assertNotNull(str);
        }
    }

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("valid construction succeeds")
        void testValidConstruction() {
            Set<String> dic = defaultDic();
            Compare4Solution game = new Compare4Solution(dic, "JAVA");
            assertEquals(0, game.getGuessTimes());
            assertFalse(game.isGameOver());
            assertFalse(game.isSolved());
        }

        @Test
        @DisplayName("null dictionary throws")
        void testNullDictionary() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(null, "JAVA"));
        }

        @Test
        @DisplayName("empty dictionary throws")
        void testEmptyDictionary() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(new HashSet<>(), "JAVA"));
        }

        @Test
        @DisplayName("null target throws")
        void testNullTarget() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(defaultDic(), null));
        }

        @Test
        @DisplayName("target not in dictionary throws")
        void testTargetNotInDic() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(defaultDic(), "ZZZZ"));
        }

        @Test
        @DisplayName("target wrong length throws")
        void testTargetWrongLength() {
            Set<String> dic = new HashSet<>(Arrays.asList("HELLO"));
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(dic, "HELLO"));
        }

        @Test
        @DisplayName("target with 3 letters throws")
        void testTargetTooShort() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(defaultDic(), "JAV"));
        }

        @Test
        @DisplayName("case insensitive target accepted")
        void testTargetCaseInsensitive() {
            Set<String> dic = defaultDic();
            dic.add("java"); // add lowercase version
            // Constructor uppercases target, so "java" should work if "JAVA" is in dic
            // Actually the dic contains "JAVA", and target is uppercased
            Compare4Solution game = new Compare4Solution(dic, "java");
            assertNotNull(game);
        }
    }

    // ==================== Guess Validation Tests ====================

    @Nested
    @DisplayName("Guess Validation Tests")
    class GuessValidationTests {

        @Test
        @DisplayName("null input throws without incrementing guess count")
        void testNullInput() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            assertThrows(IllegalArgumentException.class, () -> game.guess(null));
            assertEquals(0, game.getGuessTimes());
        }

        @Test
        @DisplayName("empty input throws without incrementing guess count")
        void testEmptyInput() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            assertThrows(IllegalArgumentException.class, () -> game.guess(""));
            assertEquals(0, game.getGuessTimes());
        }

        @Test
        @DisplayName("input too short throws without incrementing")
        void testInputTooShort() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            assertThrows(IllegalArgumentException.class, () -> game.guess("JAV"));
            assertEquals(0, game.getGuessTimes());
        }

        @Test
        @DisplayName("input too long throws without incrementing")
        void testInputTooLong() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            assertThrows(IllegalArgumentException.class, () -> game.guess("JAVAS"));
            assertEquals(0, game.getGuessTimes());
        }

        @Test
        @DisplayName("input not in dictionary throws without incrementing")
        void testInputNotInDic() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            assertThrows(IllegalArgumentException.class, () -> game.guess("ZZZZ"));
            assertEquals(0, game.getGuessTimes());
        }

        @Test
        @DisplayName("valid input increments guess count")
        void testValidInputIncrements() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            game.guess("CODE");
            assertEquals(1, game.getGuessTimes());
        }

        @Test
        @DisplayName("case insensitive guess accepted")
        void testCaseInsensitiveGuess() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            Compare4Solution.CompareResult result = game.guess("code");
            assertNotNull(result);
            assertEquals("CODE", result.getGuess());
            assertEquals(1, game.getGuessTimes());
        }
    }

    // ==================== Game State Tests ====================

    @Nested
    @DisplayName("Game State Tests")
    class GameStateTests {

        @Test
        @DisplayName("getGuessTimes starts at 0")
        void testInitialGuessTimes() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            assertEquals(0, game.getGuessTimes());
        }

        @Test
        @DisplayName("getGuessTimes increments on valid guesses only")
        void testGuessTimesIncrement() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            game.guess("CODE");
            game.guess("BARK");
            try { game.guess("ZZZZ"); } catch (IllegalArgumentException e) { /* invalid */ }
            assertEquals(2, game.getGuessTimes());
        }

        @Test
        @DisplayName("isSolved is false before correct guess")
        void testNotSolvedBefore() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            game.guess("CODE");
            assertFalse(game.isSolved());
        }

        @Test
        @DisplayName("isGameOver is false before max guesses")
        void testNotGameOverBefore() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            game.guess("CODE");
            assertFalse(game.isGameOver());
        }
    }

    // ==================== Functional Tests: Success ====================

    @Nested
    @DisplayName("Functional Tests: Success")
    class SuccessTests {

        @Test
        @DisplayName("correct guess on first try")
        void testCorrectFirstGuess() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            Compare4Solution.CompareResult result = game.guess("JAVA");
            assertTrue(result.isCorrect());
            assertTrue(game.isSolved());
            assertTrue(game.isGameOver());
            assertEquals(1, game.getGuessTimes());
        }

        @Test
        @DisplayName("correct guess on last allowed attempt")
        void testCorrectOnLastAttempt() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            // Use 7 wrong guesses first
            for (int i = 0; i < Compare4Solution.MAX_GUESSES - 1; i++) {
                game.guess("CODE");
            }
            assertFalse(game.isGameOver());
            Compare4Solution.CompareResult result = game.guess("JAVA");
            assertTrue(result.isCorrect());
            assertTrue(game.isSolved());
            assertTrue(game.isGameOver());
            assertEquals(Compare4Solution.MAX_GUESSES, game.getGuessTimes());
        }

        @Test
        @DisplayName("correct guess in the middle")
        void testCorrectMidGame() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            game.guess("CODE");
            game.guess("TEST");
            Compare4Solution.CompareResult result = game.guess("JAVA");
            assertTrue(result.isCorrect());
            assertTrue(game.isSolved());
            assertEquals(3, game.getGuessTimes());
        }
    }

    // ==================== Functional Tests: Failure ====================

    @Nested
    @DisplayName("Functional Tests: Failure")
    class FailureTests {

        @Test
        @DisplayName("game over after max guesses without solving")
        void testGameOverMaxGuesses() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            for (int i = 0; i < Compare4Solution.MAX_GUESSES; i++) {
                game.guess("CODE");
            }
            assertTrue(game.isGameOver());
            assertFalse(game.isSolved());
            assertEquals(Compare4Solution.MAX_GUESSES, game.getGuessTimes());
        }

        @Test
        @DisplayName("cannot guess after game over (max guesses)")
        void testCannotGuessAfterMaxGuesses() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            for (int i = 0; i < Compare4Solution.MAX_GUESSES; i++) {
                game.guess("CODE");
            }
            assertThrows(IllegalStateException.class, () -> game.guess("JAVA"));
        }

        @Test
        @DisplayName("cannot guess after game over (solved)")
        void testCannotGuessAfterSolved() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            game.guess("JAVA");
            assertTrue(game.isSolved());
            assertThrows(IllegalStateException.class, () -> game.guess("CODE"));
        }

        @Test
        @DisplayName("invalid guesses do not count toward max")
        void testInvalidGuessesDoNotCount() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            for (int i = 0; i < 20; i++) {
                try { game.guess("ZZZZ"); } catch (IllegalArgumentException e) { /* invalid */ }
            }
            assertEquals(0, game.getGuessTimes());
            assertFalse(game.isGameOver());
            // Can still play
            game.guess("CODE");
            assertEquals(1, game.getGuessTimes());
        }

        @Test
        @DisplayName("mix of valid and invalid guesses")
        void testMixValidInvalid() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            game.guess("CODE");
            try { game.guess("ZZZZ"); } catch (IllegalArgumentException e) { /* invalid */ }
            try { game.guess(""); } catch (IllegalArgumentException e) { /* invalid */ }
            game.guess("BARK");
            try { game.guess(null); } catch (IllegalArgumentException e) { /* invalid */ }
            game.guess("TEST");
            assertEquals(3, game.getGuessTimes());
        }
    }

    // ==================== Compare Result Integrity Tests ====================

    @Nested
    @DisplayName("Compare Result Integrity Tests")
    class CompareResultIntegrityTests {

        @Test
        @DisplayName("result array has correct length")
        void testResultLength() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            Compare4Solution.CompareResult result = game.guess("CODE");
            assertEquals(Compare4Solution.WORD_LENGTH, result.getResult().length);
        }

        @Test
        @DisplayName("result values are within valid range")
        void testResultValuesRange() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            Compare4Solution.CompareResult result = game.guess("CODE");
            for (int r : result.getResult()) {
                assertTrue(r >= -1 && r <= 1,
                        "Result value " + r + " is out of range [-1, 1]");
            }
        }

        @Test
        @DisplayName("exact match guess returns all EXACT")
        void testExactMatchAllExact() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            Compare4Solution.CompareResult result = game.guess("JAVA");
            assertArrayEquals(new int[]{1, 1, 1, 1}, result.getResult());
        }
    }

    // ==================== Compare Algorithm Tests ====================

    @Nested
    @DisplayName("Compare Algorithm Tests")
    class CompareAlgorithmTests {

        @Test
        @DisplayName("all absent — no letters in common")
        void testAllAbsent() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            Compare4Solution.CompareResult result = game.guess("BOOK");
            // J≠B, A≠O, V≠O, A≠K → all absent
            assertArrayEquals(new int[]{-1, -1, -1, -1}, result.getResult());
        }

        @Test
        @DisplayName("all misplaced — all letters present but wrong position")
        void testAllMisplaced() {
            Set<String> dic = defaultDic();
            dic.add("DCBA");
            Compare4Solution game = new Compare4Solution(dic, "ABCD");
            Compare4Solution.CompareResult result = game.guess("DCBA");
            // D≠A, C≠B, B≠C, A≠D but all exist → all misplaced
            assertArrayEquals(new int[]{0, 0, 0, 0}, result.getResult());
        }

        @Test
        @DisplayName("mix of exact, misplaced, absent")
        void testMixedResult() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "ABCD");
            Compare4Solution.CompareResult result = game.guess("ADAA");
            // pos0: A==A → 1
            // pos1: D≠B, D in target → 0
            // pos2: A≠C, A not remaining (only B,C,D left minus D used) → check
            // pos3: A≠D → -1 or 0
            // target: ABCD, guess: ADAA
            // exact: pos0 A=A
            // remaining from target: B(1), C(1), D(1)
            // pos1: D → in remaining → 0, remaining D=0
            // pos2: A → not in remaining → -1
            // pos3: A → not in remaining → -1
            assertArrayEquals(new int[]{1, 0, -1, -1}, result.getResult());
        }

        @Test
        @DisplayName("duplicate letters in target — example from problem")
        void testDuplicateLettersInTarget() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "AABC");
            Compare4Solution.CompareResult result = game.guess("ADAA");
            // pos0: A==A → 1
            // pos1: D≠A → remaining: A(1), B(1), C(1)
            // pos2: A≠B → A in remaining(count=1) → 0, decrement A to 0
            // pos3: A≠C → A not available → -1
            assertArrayEquals(new int[]{1, -1, 0, -1}, result.getResult());
        }

        @Test
        @DisplayName("duplicate letters in guess — only credit available count")
        void testDuplicateLettersInGuess() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "ABCD");
            Compare4Solution.CompareResult result = game.guess("AABC");
            // pos0: A==A → 1
            // pos1: A≠B → A not in remaining (only B,C,D) → -1
            // pos2: B≠C → B in remaining → 0
            // pos3: C≠D → C in remaining → 0
            assertArrayEquals(new int[]{1, -1, 0, 0}, result.getResult());
        }

        @Test
        @DisplayName("exact match takes priority over misplaced")
        void testExactPriorityOverMisplaced() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "BARK");
            Compare4Solution.CompareResult result = game.guess("BIKE");
            // pos0: B==B → 1
            // pos1: I≠A → remaining: A(1), R(1), K(1)
            // pos2: K≠R → K in remaining → 0
            // pos3: E≠K → E not in remaining → -1
            assertArrayEquals(new int[]{1, -1, 0, -1}, result.getResult());
        }

        @Test
        @DisplayName("same word returns all exact")
        void testSameWord() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "CODE");
            Compare4Solution.CompareResult result = game.guess("CODE");
            assertArrayEquals(new int[]{1, 1, 1, 1}, result.getResult());
        }

        @Test
        @DisplayName("completely different word returns all absent")
        void testCompletelyDifferent() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            Compare4Solution.CompareResult result = game.guess("CODE");
            // J≠C, A≠O, V≠D, A≠E → no overlap
            assertArrayEquals(new int[]{-1, -1, -1, -1}, result.getResult());
        }

        @Test
        @DisplayName("partial exact match")
        void testPartialExact() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "CARD");
            Compare4Solution.CompareResult result = game.guess("CODE");
            // pos0: C==C → 1
            // pos1: O≠A → remaining: A(1), R(1), D(1)
            // pos2: D≠R → D in remaining → 0
            // pos3: E≠D → E not in remaining → -1
            assertArrayEquals(new int[]{1, -1, 0, -1}, result.getResult());
        }

        @Test
        @DisplayName("guessing same wrong word twice gives same result")
        void testSameGuessTwice() {
            Compare4Solution game = new Compare4Solution(defaultDic(), "JAVA");
            Compare4Solution.CompareResult r1 = game.guess("CODE");
            Compare4Solution.CompareResult r2 = game.guess("CODE");
            assertArrayEquals(r1.getResult(), r2.getResult());
        }
    }
}
