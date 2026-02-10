import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class Compare4SolutionTest {

    private Compare4Solution solution;

    @BeforeEach
    void setUp() {
        solution = new Compare4Solution();
    }

    // ==================== Helper ====================

    private Set<String> dictOf(String... words) {
        Set<String> dict = new HashSet<>();
        for (String w : words) dict.add(w);
        return dict;
    }

    // ==================== Example from spec ====================

    @Test
    void testSpecExample_AABC_ADAA() {
        // given = "AABC", guess = "ADAA"
        // pos0 A==A → 1
        // pos1 D not in remaining → -1
        // pos2 A exists (remaining A count=1) → 0
        // pos3 A no longer available → -1
        assertArrayEquals(new int[]{1, -1, 0, -1}, solution.compare("AABC", "ADAA"));
    }

    // ==================== Exact matches ====================

    @Nested
    class ExactMatchTests {

        @Test
        void testAllExactMatch() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, solution.compare("ABCD", "ABCD"));
        }

        @Test
        void testAllExactMatchSameLetters() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, solution.compare("AAAA", "AAAA"));
        }

        @Test
        void testSingleExactMatchAtStart() {
            assertArrayEquals(new int[]{1, -1, -1, -1}, solution.compare("AXYZ", "ABCD"));
        }

        @Test
        void testSingleExactMatchAtEnd() {
            assertArrayEquals(new int[]{-1, -1, -1, 1}, solution.compare("XYZD", "ABCD"));
        }
    }

    // ==================== All absent ====================

    @Nested
    class AbsentTests {

        @Test
        void testAllAbsent() {
            assertArrayEquals(new int[]{-1, -1, -1, -1}, solution.compare("ABCD", "EFGH"));
        }

        @Test
        void testNoOverlapCompletely() {
            assertArrayEquals(new int[]{-1, -1, -1, -1}, solution.compare("WXYZ", "ABCD"));
        }
    }

    // ==================== Misplaced ====================

    @Nested
    class MisplacedTests {

        @Test
        void testAllMisplaced() {
            // given=ABCD guess=DCBA → every letter exists but in wrong position
            assertArrayEquals(new int[]{0, 0, 0, 0}, solution.compare("ABCD", "DCBA"));
        }

        @Test
        void testTwoMisplaced() {
            // given=ABCD guess=BAXY → B misplaced, A misplaced, X absent, Y absent
            assertArrayEquals(new int[]{0, 0, -1, -1}, solution.compare("ABCD", "BAXY"));
        }

        @Test
        void testOneMisplacedRestAbsent() {
            // given=ABCD guess=XAXZ → pos0 X absent, pos1 A misplaced (A in pos0), pos2 X absent, pos3 Z absent
            assertArrayEquals(new int[]{-1, 0, -1, -1}, solution.compare("ABCD", "XAXZ"));
        }
    }

    // ==================== Duplicate letter handling ====================

    @Nested
    class DuplicateLetterTests {

        @Test
        void testDuplicateInGuess_oneMatchOneAbsent() {
            // given=ABCD guess=AAXZ → pos0 A exact, pos1 A no more A's available → -1
            assertArrayEquals(new int[]{1, -1, -1, -1}, solution.compare("ABCD", "AAXZ"));
        }

        @Test
        void testDuplicateInGiven_oneExactOneMisplaced() {
            // given=AAXZ guess=XAAB
            // Pass1: pos0 X!=A, pos1 A==A exact, pos2 X!=A, pos3 Z!=B
            // remaining from given: A(pos0), X(pos2), Z(pos3) → {A:1, X:1, Z:1}
            // Pass2: pos0 X → count X=1 → misplaced 0, pos2 A → count A=1 → misplaced 0, pos3 B → absent
            assertArrayEquals(new int[]{0, 1, 0, -1}, solution.compare("AAXZ", "XAAB"));
        }

        @Test
        void testDuplicateInBoth_twoExact() {
            // given=AABB guess=AABB → all exact
            assertArrayEquals(new int[]{1, 1, 1, 1}, solution.compare("AABB", "AABB"));
        }

        @Test
        void testDuplicateInBoth_swapped() {
            // given=AABB guess=BBAA → all misplaced
            assertArrayEquals(new int[]{0, 0, 0, 0}, solution.compare("AABB", "BBAA"));
        }

        @Test
        void testTripleDuplicateInGuess_onlyOneInGiven() {
            // given=AXYZ guess=AAAZ
            // Pass1: pos0 A==A exact, pos1 X!=A, pos2 Y!=A, pos3 Z==Z exact? no wait
            // given=AXYZ guess=AAAZ
            // pos0 A==A → exact
            // pos1 X!=A
            // pos2 Y!=A
            // pos3 Z==Z → exact
            // remaining: {X:1, Y:1}
            // pass2: pos1 A → no A remaining → -1, pos2 A → no A remaining → -1
            assertArrayEquals(new int[]{1, -1, -1, 1}, solution.compare("AXYZ", "AAAZ"));
        }

        @Test
        void testDuplicateExactPlusMisplaced() {
            // given=ABBA guess=ABXX
            // pos0 A==A exact, pos1 B==B exact, pos2 B!=X, pos3 A!=X
            // remaining: {B:1, A:1}
            // pos2 X → absent, pos3 X → absent
            assertArrayEquals(new int[]{1, 1, -1, -1}, solution.compare("ABBA", "ABXX"));
        }

        @Test
        void testAllSameLetterGuess_oneInGiven() {
            // given=AXYZ guess=AAAA
            // pos0 A==A exact
            // remaining: {X:1, Y:1, Z:1}
            // pos1 A → no A remaining → -1
            // pos2 A → -1
            // pos3 A → -1
            assertArrayEquals(new int[]{1, -1, -1, -1}, solution.compare("AXYZ", "AAAA"));
        }

        @Test
        void testAllSameLetterGiven_oneInGuess() {
            // given=AAAA guess=AXYZ
            // pos0 A==A exact
            // remaining: {A:3}
            // pos1 X → absent, pos2 Y → absent, pos3 Z → absent
            assertArrayEquals(new int[]{1, -1, -1, -1}, solution.compare("AAAA", "AXYZ"));
        }

        @Test
        void testMisplacedLimitedByGivenCount() {
            // given=ABCC guess=CCBA
            // pos0 A!=C, pos1 B!=C, pos2 C==B? no → C!=B, pos3 C!=A
            // exact: none
            // remaining from given: {A:1, B:1, C:2}
            // pos0 C → count C=2 → misplaced, C→1
            // pos1 C → count C=1 → misplaced, C→0
            // pos2 B → count B=1 → misplaced, B→0
            // pos3 A → count A=1 → misplaced, A→0
            assertArrayEquals(new int[]{0, 0, 0, 0}, solution.compare("ABCC", "CCBA"));
        }

        @Test
        void testExactMatchReducesAvailableCount() {
            // given=ABCA guess=CXCA
            // pos0 A!=C, pos1 B!=X, pos2 C==C exact, pos3 A==A exact
            // remaining: {A:1, B:1} (C at pos2 used for exact, A at pos3 used for exact)
            // wait, given is A B C A. Exact at pos2 (C==C), pos3 (A==A)
            // remaining from given non-exact: pos0 A, pos1 B → {A:1, B:1}
            // pos0 C → no C remaining → -1
            // pos1 X → absent → -1
            assertArrayEquals(new int[]{-1, -1, 1, 1}, solution.compare("ABCA", "CXCA"));
        }
    }

    // ==================== Mixed results ====================

    @Nested
    class MixedResultTests {

        @Test
        void testExactAndMisplaced() {
            // given=ABCD guess=ABDC → pos0 exact, pos1 exact, pos2 D misplaced, pos3 C misplaced
            assertArrayEquals(new int[]{1, 1, 0, 0}, solution.compare("ABCD", "ABDC"));
        }

        @Test
        void testExactMisplacedAbsent() {
            // given=ABCD guess=AXDB → pos0 exact, pos1 X absent, pos2 D misplaced? no
            // given=ABCD guess=AXDB
            // pos0 A==A exact
            // pos1 B!=X
            // pos2 C!=D
            // pos3 D!=B
            // remaining: {B:1, C:1, D:1}
            // pos1 X → absent, pos2 D → D remaining → misplaced, pos3 B → B remaining → misplaced
            assertArrayEquals(new int[]{1, -1, 0, 0}, solution.compare("ABCD", "AXDB"));
        }

        @Test
        void testFirstAbsentRestExact() {
            assertArrayEquals(new int[]{-1, 1, 1, 1}, solution.compare("XBCD", "ABCD"));
        }
    }

    // ==================== Case insensitivity ====================

    @Nested
    class CaseInsensitivityTests {

        @Test
        void testLowerCaseGiven() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, solution.compare("abcd", "ABCD"));
        }

        @Test
        void testLowerCaseGuess() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, solution.compare("ABCD", "abcd"));
        }

        @Test
        void testMixedCase() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, solution.compare("AbCd", "aBcD"));
        }
    }

    // ==================== Input validation ====================

    @Nested
    class InputValidationTests {

        @Test
        void testNullGivenThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> solution.compare(null, "ABCD"));
            assertTrue(ex.getMessage().contains("given"));
        }

        @Test
        void testNullGuessThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> solution.compare("ABCD", null));
            assertTrue(ex.getMessage().contains("guess"));
        }

        @Test
        void testGivenTooShortThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> solution.compare("ABC", "ABCD"));
            assertTrue(ex.getMessage().contains("4"));
        }

        @Test
        void testGuessTooLongThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> solution.compare("ABCD", "ABCDE"));
            assertTrue(ex.getMessage().contains("4"));
        }

        @Test
        void testEmptyGivenThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> solution.compare("", "ABCD"));
        }

        @Test
        void testEmptyGuessThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> solution.compare("ABCD", ""));
        }
    }

    // ==================== Constants ====================

    @Test
    void testConstants() {
        assertEquals(4, Compare4Solution.WORD_LENGTH);
        assertEquals(1, Compare4Solution.EXACT_MATCH);
        assertEquals(0, Compare4Solution.MISPLACED);
        assertEquals(-1, Compare4Solution.ABSENT);
    }

    // ==================== Dictionary / Game constructor ====================

    @Nested
    class ConstructorTests {

        @Test
        void testValidConstruction() {
            Set<String> dict = dictOf("ABCD", "EFGH", "WXYZ");
            Compare4Solution game = new Compare4Solution(dict, "ABCD");
            assertNotNull(game);
            assertEquals(0, game.getAttempts());
            assertFalse(game.isSolved());
        }

        @Test
        void testConstructionNormalizesCaseInDictAndTarget() {
            Set<String> dict = dictOf("abcd", "efgh");
            Compare4Solution game = new Compare4Solution(dict, "abcd");
            // Should work — both normalized to uppercase internally
            assertNotNull(game.getDictionary());
            assertTrue(game.getDictionary().contains("ABCD"));
        }

        @Test
        void testNullDictionaryThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(null, "ABCD"));
        }

        @Test
        void testEmptyDictionaryThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(new HashSet<>(), "ABCD"));
        }

        @Test
        void testNullTargetWordThrows() {
            Set<String> dict = dictOf("ABCD");
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(dict, null));
        }

        @Test
        void testTargetWordWrongLengthThrows() {
            Set<String> dict = dictOf("ABCD");
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(dict, "ABC"));
        }

        @Test
        void testTargetWordNotInDictionaryThrows() {
            Set<String> dict = dictOf("ABCD", "EFGH");
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4Solution(dict, "WXYZ"));
        }

        @Test
        void testDictionaryFiltersInvalidWords() {
            Set<String> dict = new HashSet<>();
            dict.add("ABCD");
            dict.add("AB"); // too short — should be filtered
            dict.add(null); // null — should be filtered
            dict.add("EFGH");
            Compare4Solution game = new Compare4Solution(dict, "ABCD");
            // Only valid 4-letter words kept
            assertEquals(2, game.getDictionary().size());
        }

        @Test
        void testDefaultConstructorNoDictionary() {
            Compare4Solution s = new Compare4Solution();
            assertNull(s.getDictionary());
            assertEquals(0, s.getAttempts());
            assertFalse(s.isSolved());
        }
    }

    // ==================== guess() method ====================

    @Nested
    class GuessTests {

        @Test
        void testCorrectGuessOnFirstTry() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");
            Compare4Solution.GuessResult result = game.guess("ABCD");

            assertArrayEquals(new int[]{1, 1, 1, 1}, result.getFeedback());
            assertTrue(result.isSolved());
            assertTrue(game.isSolved());
            assertEquals(1, game.getAttempts());
        }

        @Test
        void testWrongGuessThenCorrect() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH", "WXYZ"), "ABCD");

            Compare4Solution.GuessResult r1 = game.guess("EFGH");
            assertArrayEquals(new int[]{-1, -1, -1, -1}, r1.getFeedback());
            assertFalse(r1.isSolved());
            assertFalse(game.isSolved());
            assertEquals(1, game.getAttempts());

            Compare4Solution.GuessResult r2 = game.guess("ABCD");
            assertTrue(r2.isSolved());
            assertTrue(game.isSolved());
            assertEquals(2, game.getAttempts());
        }

        @Test
        void testGuessAfterSolvedThrows() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");
            game.guess("ABCD");

            assertThrows(IllegalStateException.class, () -> game.guess("EFGH"));
        }

        @Test
        void testGuessNotInDictionaryThrows() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");

            assertThrows(IllegalArgumentException.class, () -> game.guess("ZZZZ"));
        }

        @Test
        void testGuessNullThrows() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");

            assertThrows(IllegalArgumentException.class, () -> game.guess(null));
        }

        @Test
        void testGuessWrongLengthThrows() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");

            assertThrows(IllegalArgumentException.class, () -> game.guess("ABC"));
        }

        @Test
        void testGuessWithNoTargetSetThrows() {
            Compare4Solution s = new Compare4Solution();
            assertThrows(IllegalStateException.class, () -> s.guess("ABCD"));
        }

        @Test
        void testGuessCaseInsensitive() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");
            Compare4Solution.GuessResult result = game.guess("abcd");
            assertTrue(result.isSolved());
        }

        @Test
        void testGuessWithDuplicateLetters() {
            // target=AABC, dict includes ADAA
            Compare4Solution game = new Compare4Solution(dictOf("AABC", "ADAA", "XYZW"), "AABC");
            Compare4Solution.GuessResult result = game.guess("ADAA");
            assertArrayEquals(new int[]{1, -1, 0, -1}, result.getFeedback());
            assertFalse(result.isSolved());
        }
    }

    // ==================== History ====================

    @Nested
    class HistoryTests {

        @Test
        void testHistoryInitiallyEmpty() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");
            assertTrue(game.getHistory().isEmpty());
        }

        @Test
        void testHistoryTracksAllGuesses() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH", "WXYZ"), "WXYZ");

            game.guess("ABCD");
            game.guess("EFGH");
            game.guess("WXYZ");

            List<Compare4Solution.GuessResult> history = game.getHistory();
            assertEquals(3, history.size());
            assertEquals("ABCD", history.get(0).getGuess());
            assertEquals("EFGH", history.get(1).getGuess());
            assertEquals("WXYZ", history.get(2).getGuess());
            assertFalse(history.get(0).isSolved());
            assertFalse(history.get(1).isSolved());
            assertTrue(history.get(2).isSolved());
        }

        @Test
        void testHistoryIsDefensiveCopy() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");
            game.guess("EFGH");
            List<Compare4Solution.GuessResult> h1 = game.getHistory();
            h1.clear(); // mutate the returned list
            // Original history should be unaffected
            assertEquals(1, game.getHistory().size());
        }

        @Test
        void testGuessResultFeedbackIsDefensiveCopy() {
            Compare4Solution game = new Compare4Solution(dictOf("ABCD", "EFGH"), "ABCD");
            Compare4Solution.GuessResult result = game.guess("EFGH");
            int[] feedback = result.getFeedback();
            feedback[0] = 999; // mutate
            // Original feedback should be unaffected
            assertEquals(-1, result.getFeedback()[0]);
        }
    }

    // ==================== GuessResult ====================

    @Nested
    class GuessResultTests {

        @Test
        void testGuessResultSolvedAllExact() {
            Compare4Solution.GuessResult r = new Compare4Solution.GuessResult("ABCD", new int[]{1, 1, 1, 1});
            assertTrue(r.isSolved());
        }

        @Test
        void testGuessResultNotSolvedPartial() {
            Compare4Solution.GuessResult r = new Compare4Solution.GuessResult("ABCD", new int[]{1, 0, -1, 1});
            assertFalse(r.isSolved());
        }

        @Test
        void testGuessResultNotSolvedAllAbsent() {
            Compare4Solution.GuessResult r = new Compare4Solution.GuessResult("ABCD", new int[]{-1, -1, -1, -1});
            assertFalse(r.isSolved());
        }

        @Test
        void testGuessResultGetGuess() {
            Compare4Solution.GuessResult r = new Compare4Solution.GuessResult("TEST", new int[]{1, 0, 0, -1});
            assertEquals("TEST", r.getGuess());
        }
    }

    // ==================== Multi-guess game scenarios ====================

    @Nested
    class GameScenarioTests {

        @Test
        void testSolveInThreeAttempts() {
            Set<String> dict = dictOf("ABCD", "ABCX", "XBCD", "ABXY");
            Compare4Solution game = new Compare4Solution(dict, "ABCD");

            Compare4Solution.GuessResult r1 = game.guess("ABXY");
            assertArrayEquals(new int[]{1, 1, -1, -1}, r1.getFeedback());

            Compare4Solution.GuessResult r2 = game.guess("ABCX");
            assertArrayEquals(new int[]{1, 1, 1, -1}, r2.getFeedback());

            Compare4Solution.GuessResult r3 = game.guess("ABCD");
            assertTrue(r3.isSolved());
            assertEquals(3, game.getAttempts());
        }

        @Test
        void testAttemptsCountNeverResets() {
            Set<String> dict = dictOf("ABCD", "EFGH", "WXYZ");
            Compare4Solution game = new Compare4Solution(dict, "WXYZ");

            game.guess("ABCD");
            game.guess("EFGH");
            assertEquals(2, game.getAttempts());

            game.guess("WXYZ");
            assertEquals(3, game.getAttempts());
        }

        @Test
        void testDictionaryReturnedIsDefensiveCopy() {
            Set<String> dict = dictOf("ABCD", "EFGH");
            Compare4Solution game = new Compare4Solution(dict, "ABCD");
            Set<String> returned = game.getDictionary();
            returned.clear(); // mutate returned set
            // Internal dictionary unaffected
            assertEquals(2, game.getDictionary().size());
        }
    }
}
