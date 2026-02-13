import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class Compare4CLITest {

    // ==================== Helper ====================

    private Set<String> dictOf(String... words) {
        Set<String> dict = new HashSet<>();
        for (String w : words) dict.add(w);
        return dict;
    }

    // ==================== Compare (static) ====================

    @Nested
    class CompareTests {

        @Test
        void allExactMatch() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, Compare4CLI.compare("ABCD", "ABCD"));
        }

        @Test
        void allAbsent() {
            assertArrayEquals(new int[]{-1, -1, -1, -1}, Compare4CLI.compare("ABCD", "EFGH"));
        }

        @Test
        void allMisplaced() {
            assertArrayEquals(new int[]{0, 0, 0, 0}, Compare4CLI.compare("ABCD", "DCBA"));
        }

        @Test
        void mixedExactAndMisplaced() {
            // ABCD vs ABDC → pos0,1 exact; pos2,3 swapped
            assertArrayEquals(new int[]{1, 1, 0, 0}, Compare4CLI.compare("ABCD", "ABDC"));
        }

        @Test
        void mixedExactMisplacedAbsent() {
            // ABCD vs AXDB → pos0 exact, pos1 X absent, pos2 D misplaced, pos3 B misplaced
            assertArrayEquals(new int[]{1, -1, 0, 0}, Compare4CLI.compare("ABCD", "AXDB"));
        }

        @Test
        void singleExactAtStart() {
            assertArrayEquals(new int[]{1, -1, -1, -1}, Compare4CLI.compare("AXYZ", "ABCD"));
        }

        @Test
        void singleExactAtEnd() {
            assertArrayEquals(new int[]{-1, -1, -1, 1}, Compare4CLI.compare("XYZD", "ABCD"));
        }

        @Test
        void specExample_AABC_ADAA() {
            // given=AABC guess=ADAA
            // pos0 A==A exact, pos1 D not in remaining, pos2 A misplaced (1 A left), pos3 A exhausted
            assertArrayEquals(new int[]{1, -1, 0, -1}, Compare4CLI.compare("AABC", "ADAA"));
        }
    }

    // ==================== Duplicate Letter Handling ====================

    @Nested
    class DuplicateLetterTests {

        @Test
        void duplicateInGuess_oneMatchOneAbsent() {
            // ABCD vs AAXZ → pos0 exact, pos1 A no more → -1
            assertArrayEquals(new int[]{1, -1, -1, -1}, Compare4CLI.compare("ABCD", "AAXZ"));
        }

        @Test
        void duplicateInGiven_oneExactOneMisplaced() {
            // AAXZ vs XAAB → pos1 exact, remaining {A:1,X:1,Z:1}
            // pos0 X→misplaced, pos2 A→misplaced, pos3 B→absent
            assertArrayEquals(new int[]{0, 1, 0, -1}, Compare4CLI.compare("AAXZ", "XAAB"));
        }

        @Test
        void duplicateInBoth_allExact() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, Compare4CLI.compare("AABB", "AABB"));
        }

        @Test
        void duplicateInBoth_allMisplaced() {
            assertArrayEquals(new int[]{0, 0, 0, 0}, Compare4CLI.compare("AABB", "BBAA"));
        }

        @Test
        void tripleDuplicateInGuess_onlyOneInGiven() {
            // AXYZ vs AAAZ → pos0 exact, pos3 exact, remaining {X:1,Y:1}
            // pos1 A→absent, pos2 A→absent
            assertArrayEquals(new int[]{1, -1, -1, 1}, Compare4CLI.compare("AXYZ", "AAAZ"));
        }

        @Test
        void allSameLetterGuess_oneInGiven() {
            // AXYZ vs AAAA → pos0 exact, remaining {X,Y,Z}
            assertArrayEquals(new int[]{1, -1, -1, -1}, Compare4CLI.compare("AXYZ", "AAAA"));
        }

        @Test
        void allSameLetterGiven_oneInGuess() {
            // AAAA vs AXYZ → pos0 exact, remaining {A:3}
            assertArrayEquals(new int[]{1, -1, -1, -1}, Compare4CLI.compare("AAAA", "AXYZ"));
        }

        @Test
        void misplacedLimitedByGivenCount() {
            // ABCC vs CCBA → no exact, remaining {A:1,B:1,C:2}
            // pos0 C→misplaced(C:1), pos1 C→misplaced(C:0), pos2 B→misplaced, pos3 A→misplaced
            assertArrayEquals(new int[]{0, 0, 0, 0}, Compare4CLI.compare("ABCC", "CCBA"));
        }

        @Test
        void exactMatchReducesAvailableCount() {
            // ABCA vs CXCA → pos2 C==C exact, pos3 A==A exact
            // remaining {A:1, B:1}, pos0 C→absent, pos1 X→absent
            assertArrayEquals(new int[]{-1, -1, 1, 1}, Compare4CLI.compare("ABCA", "CXCA"));
        }
    }

    // ==================== Case Insensitivity ====================

    @Nested
    class CaseInsensitivityTests {

        @Test
        void lowerCaseGiven() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, Compare4CLI.compare("abcd", "ABCD"));
        }

        @Test
        void lowerCaseGuess() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, Compare4CLI.compare("ABCD", "abcd"));
        }

        @Test
        void mixedCase() {
            assertArrayEquals(new int[]{1, 1, 1, 1}, Compare4CLI.compare("AbCd", "aBcD"));
        }
    }

    // ==================== Compare Validation ====================

    @Nested
    class CompareValidationTests {

        @Test
        void nullGivenThrows() {
            assertThrows(IllegalArgumentException.class, () -> Compare4CLI.compare(null, "ABCD"));
        }

        @Test
        void nullGuessThrows() {
            assertThrows(IllegalArgumentException.class, () -> Compare4CLI.compare("ABCD", null));
        }

        @Test
        void givenTooShortThrows() {
            assertThrows(IllegalArgumentException.class, () -> Compare4CLI.compare("ABC", "ABCD"));
        }

        @Test
        void guessTooLongThrows() {
            assertThrows(IllegalArgumentException.class, () -> Compare4CLI.compare("ABCD", "ABCDE"));
        }

        @Test
        void emptyGivenThrows() {
            assertThrows(IllegalArgumentException.class, () -> Compare4CLI.compare("", "ABCD"));
        }

        @Test
        void emptyGuessThrows() {
            assertThrows(IllegalArgumentException.class, () -> Compare4CLI.compare("ABCD", ""));
        }
    }

    // ==================== Constructor ====================

    @Nested
    class ConstructorTests {

        @Test
        void validConstruction() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            assertEquals(0, game.getAttempts());
            assertFalse(game.isSolved());
            assertTrue(game.getHistory().isEmpty());
        }

        @Test
        void constructionNormalizesCase() {
            Compare4CLI game = new Compare4CLI(dictOf("abcd", "efgh"), "abcd");
            assertNotNull(game);
        }

        @Test
        void nullDictionaryThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4CLI(null, "ABCD"));
        }

        @Test
        void emptyDictionaryThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4CLI(new HashSet<>(), "ABCD"));
        }

        @Test
        void nullTargetThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4CLI(dictOf("ABCD"), null));
        }

        @Test
        void targetWrongLengthThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4CLI(dictOf("ABCD"), "ABC"));
        }

        @Test
        void targetNotInDictionaryThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Compare4CLI(dictOf("ABCD", "EFGH"), "WXYZ"));
        }

        @Test
        void dictionaryFiltersInvalidWords() {
            Set<String> dict = new HashSet<>();
            dict.add("ABCD");
            dict.add("AB");     // too short — filtered
            dict.add(null);     // null — filtered
            dict.add("EFGH");
            Compare4CLI game = new Compare4CLI(dict, "ABCD");
            // Should still work; invalid entries are silently dropped
            assertNotNull(game);
        }
    }

    // ==================== Guess ====================

    @Nested
    class GuessTests {

        @Test
        void correctGuessOnFirstTry() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            Compare4CLI.GuessResult result = game.guess("ABCD");

            assertArrayEquals(new int[]{1, 1, 1, 1}, result.getFeedback());
            assertTrue(result.isSolved());
            assertTrue(game.isSolved());
            assertEquals(1, game.getAttempts());
        }

        @Test
        void wrongGuessThenCorrect() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH", "WXYZ"), "ABCD");

            Compare4CLI.GuessResult r1 = game.guess("EFGH");
            assertArrayEquals(new int[]{-1, -1, -1, -1}, r1.getFeedback());
            assertFalse(r1.isSolved());
            assertEquals(1, game.getAttempts());

            Compare4CLI.GuessResult r2 = game.guess("ABCD");
            assertTrue(r2.isSolved());
            assertTrue(game.isSolved());
            assertEquals(2, game.getAttempts());
        }

        @Test
        void guessAfterSolvedThrows() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            game.guess("ABCD");
            assertThrows(IllegalStateException.class, () -> game.guess("EFGH"));
        }

        @Test
        void guessNotInDictionaryThrows() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            assertThrows(IllegalArgumentException.class, () -> game.guess("ZZZZ"));
        }

        @Test
        void guessNullThrows() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            assertThrows(IllegalArgumentException.class, () -> game.guess(null));
        }

        @Test
        void guessWrongLengthThrows() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            assertThrows(IllegalArgumentException.class, () -> game.guess("ABC"));
        }

        @Test
        void guessCaseInsensitive() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            Compare4CLI.GuessResult result = game.guess("abcd");
            assertTrue(result.isSolved());
        }

        @Test
        void guessWithDuplicateLetters() {
            Compare4CLI game = new Compare4CLI(dictOf("AABC", "ADAA", "XYZW"), "AABC");
            Compare4CLI.GuessResult result = game.guess("ADAA");
            assertArrayEquals(new int[]{1, -1, 0, -1}, result.getFeedback());
            assertFalse(result.isSolved());
        }

        @Test
        void invalidGuessDoesNotConsumeAttempt() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            assertThrows(IllegalArgumentException.class, () -> game.guess("ZZZZ"));
            assertEquals(0, game.getAttempts());
        }
    }

    // ==================== History ====================

    @Nested
    class HistoryTests {

        @Test
        void historyInitiallyEmpty() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            assertTrue(game.getHistory().isEmpty());
        }

        @Test
        void historyTracksAllGuesses() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH", "WXYZ"), "WXYZ");

            game.guess("ABCD");
            game.guess("EFGH");
            game.guess("WXYZ");

            List<Compare4CLI.GuessResult> history = game.getHistory();
            assertEquals(3, history.size());
            assertEquals("ABCD", history.get(0).getGuess());
            assertEquals("EFGH", history.get(1).getGuess());
            assertEquals("WXYZ", history.get(2).getGuess());
            assertFalse(history.get(0).isSolved());
            assertFalse(history.get(1).isSolved());
            assertTrue(history.get(2).isSolved());
        }

        @Test
        void historyIsDefensiveCopy() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            game.guess("EFGH");
            List<Compare4CLI.GuessResult> h = game.getHistory();
            h.clear();
            assertEquals(1, game.getHistory().size());
        }

        @Test
        void guessResultFeedbackIsDefensiveCopy() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH"), "ABCD");
            Compare4CLI.GuessResult result = game.guess("EFGH");
            int[] feedback = result.getFeedback();
            feedback[0] = 999;
            assertEquals(-1, result.getFeedback()[0]);
        }
    }

    // ==================== GuessResult ====================

    @Nested
    class GuessResultTests {

        @Test
        void solvedWhenAllExact() {
            Compare4CLI.GuessResult r = new Compare4CLI.GuessResult("ABCD", new int[]{1, 1, 1, 1});
            assertTrue(r.isSolved());
        }

        @Test
        void notSolvedPartial() {
            Compare4CLI.GuessResult r = new Compare4CLI.GuessResult("ABCD", new int[]{1, 0, -1, 1});
            assertFalse(r.isSolved());
        }

        @Test
        void notSolvedAllAbsent() {
            Compare4CLI.GuessResult r = new Compare4CLI.GuessResult("ABCD", new int[]{-1, -1, -1, -1});
            assertFalse(r.isSolved());
        }

        @Test
        void getGuessReturnsCorrectWord() {
            Compare4CLI.GuessResult r = new Compare4CLI.GuessResult("TEST", new int[]{1, 0, 0, -1});
            assertEquals("TEST", r.getGuess());
        }
    }

    // ==================== Game Scenarios ====================

    @Nested
    class GameScenarioTests {

        @Test
        void solveInThreeAttempts() {
            Set<String> dict = dictOf("ABCD", "ABCX", "XBCD", "ABXY");
            Compare4CLI game = new Compare4CLI(dict, "ABCD");

            Compare4CLI.GuessResult r1 = game.guess("ABXY");
            assertArrayEquals(new int[]{1, 1, -1, -1}, r1.getFeedback());

            Compare4CLI.GuessResult r2 = game.guess("ABCX");
            assertArrayEquals(new int[]{1, 1, 1, -1}, r2.getFeedback());

            Compare4CLI.GuessResult r3 = game.guess("ABCD");
            assertTrue(r3.isSolved());
            assertEquals(3, game.getAttempts());
        }

        @Test
        void attemptsCountNeverResets() {
            Compare4CLI game = new Compare4CLI(dictOf("ABCD", "EFGH", "WXYZ"), "WXYZ");
            game.guess("ABCD");
            game.guess("EFGH");
            assertEquals(2, game.getAttempts());
            game.guess("WXYZ");
            assertEquals(3, game.getAttempts());
        }
    }

    // ==================== Constants ====================

    @Test
    void constants() {
        assertEquals(4, Compare4CLI.WORD_LENGTH);
        assertEquals(1, Compare4CLI.EXACT_MATCH);
        assertEquals(0, Compare4CLI.MISPLACED);
        assertEquals(-1, Compare4CLI.ABSENT);
    }
}
