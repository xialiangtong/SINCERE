import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Compare4Game Tests")
public class Compare4GameTest {

    private Set<String> defaultDic() {
        return new HashSet<>(Arrays.asList(
                "AABC", "ABCD", "ADAA", "BARK", "BIKE", "BOOK",
                "CARD", "CODE", "DEMO", "JAVA", "WORD", "TEST"
        ));
    }

    private Compare4Game createGame(String target) {
        return new Compare4Game(defaultDic(), 7, target);
    }

    private Compare4Game createGame(String target, int maxTries) {
        return new Compare4Game(defaultDic(), maxTries, target);
    }

    // ==================== 1. Tests for inner classes / Enums ====================

    @Nested
    @DisplayName("CompareStatus Enum Tests")
    class CompareStatusTests {

        @Test
        @DisplayName("MATCH has value 1")
        void matchValue() {
            assertEquals(1, Compare4Game.CompareStatus.MATCH.getValue());
        }

        @Test
        @DisplayName("MISPLACED has value 0")
        void misplacedValue() {
            assertEquals(0, Compare4Game.CompareStatus.MISPLACED.getValue());
        }

        @Test
        @DisplayName("MISMATCH has value -1")
        void mismatchValue() {
            assertEquals(-1, Compare4Game.CompareStatus.MISMATCH.getValue());
        }

        @Test
        @DisplayName("Enum has exactly 3 values")
        void enumSize() {
            assertEquals(3, Compare4Game.CompareStatus.values().length);
        }
    }

    @Nested
    @DisplayName("GameState Enum Tests")
    class GameStateTests {

        @Test
        @DisplayName("GameState has exactly 3 values")
        void enumSize() {
            assertEquals(3, Compare4Game.GameState.values().length);
        }

        @Test
        @DisplayName("GameState values are IN_PROGRESS, WIN, LOSE")
        void enumValues() {
            assertNotNull(Compare4Game.GameState.valueOf("IN_PROGRESS"));
            assertNotNull(Compare4Game.GameState.valueOf("WIN"));
            assertNotNull(Compare4Game.GameState.valueOf("LOSE"));
        }
    }

    @Nested
    @DisplayName("CompareResult Tests")
    class CompareResultTests {

        @Test
        @DisplayName("isExactMatch returns true when all MATCH")
        void testIsExactMatchAllMatch() {
            Compare4Game.CompareStatus[] statuses = {
                Compare4Game.CompareStatus.MATCH, Compare4Game.CompareStatus.MATCH,
                Compare4Game.CompareStatus.MATCH, Compare4Game.CompareStatus.MATCH
            };
            Compare4Game.CompareResult result = new Compare4Game.CompareResult("JAVA", "JAVA", statuses);
            assertTrue(result.isExactMatch());
        }

        @Test
        @DisplayName("isExactMatch returns false when not all MATCH")
        void testIsExactMatchNotAllMatch() {
            Compare4Game.CompareStatus[] statuses = {
                Compare4Game.CompareStatus.MATCH, Compare4Game.CompareStatus.MISPLACED,
                Compare4Game.CompareStatus.MISMATCH, Compare4Game.CompareStatus.MATCH
            };
            Compare4Game.CompareResult result = new Compare4Game.CompareResult("CARD", "JAVA", statuses);
            assertFalse(result.isExactMatch());
        }

        @Test
        @DisplayName("getGuess returns the guess string")
        void testGetGuess() {
            Compare4Game.CompareStatus[] statuses = new Compare4Game.CompareStatus[4];
            Compare4Game.CompareResult result = new Compare4Game.CompareResult("TEST", "JAVA", statuses);
            assertEquals("TEST", result.getGuess());
        }

        @Test
        @DisplayName("getTarget returns the target string")
        void testGetTarget() {
            Compare4Game.CompareStatus[] statuses = new Compare4Game.CompareStatus[4];
            Compare4Game.CompareResult result = new Compare4Game.CompareResult("TEST", "JAVA", statuses);
            assertEquals("JAVA", result.getTarget());
        }

        @Test
        @DisplayName("toString formats correctly")
        void testToString() {
            Compare4Game.CompareStatus[] statuses = {
                Compare4Game.CompareStatus.MATCH, Compare4Game.CompareStatus.MISMATCH,
                Compare4Game.CompareStatus.MISPLACED, Compare4Game.CompareStatus.MISMATCH
            };
            Compare4Game.CompareResult result = new Compare4Game.CompareResult("ADAA", "AABC", statuses);
            assertEquals("Guess: ADAA -> [1, -1, 0, -1]", result.toString());
        }
    }

    // ==================== 2. Tests for invalid input / dictionary init ====================

    @Nested
    @DisplayName("Invalid Input Tests")
    class InvalidInputTests {

        @Test
        @DisplayName("Null dictionary throws exception")
        void nullDictionary() {
            assertThrows(IllegalArgumentException.class, () -> new Compare4Game(null));
        }

        @Test
        @DisplayName("Empty dictionary throws exception")
        void emptyDictionary() {
            assertThrows(IllegalArgumentException.class, () -> new Compare4Game(new HashSet<>()));
        }

        @Test
        @DisplayName("Dictionary with only invalid words throws exception")
        void dictionaryAllInvalidWords() {
            Set<String> dic = new HashSet<>(Arrays.asList("AB", "TOOLONG", "12AB", ""));
            assertThrows(IllegalArgumentException.class, () -> new Compare4Game(dic));
        }

        @Test
        @DisplayName("Dictionary filters out invalid words but keeps valid ones")
        void dictionaryFiltersInvalidWords() {
            Set<String> dic = new HashSet<>(Arrays.asList("JAVA", "AB", "TOOLONG", "1234"));
            Compare4Game game = new Compare4Game(dic, 7, "JAVA");
            // Should not throw — JAVA is valid
            assertEquals(Compare4Game.GameState.IN_PROGRESS, game.getGameState());
        }

        @Test
        @DisplayName("maxTries of 0 throws exception")
        void zeroMaxTries() {
            assertThrows(IllegalArgumentException.class, () -> new Compare4Game(defaultDic(), 0));
        }

        @Test
        @DisplayName("Negative maxTries throws exception")
        void negativeMaxTries() {
            assertThrows(IllegalArgumentException.class, () -> new Compare4Game(defaultDic(), -1));
        }

        @Test
        @DisplayName("Dictionary normalizes words to uppercase")
        void dictionaryNormalizesToUpperCase() {
            Set<String> dic = new HashSet<>(Arrays.asList("java", "Code"));
            Compare4Game game = new Compare4Game(dic, 7, "java");
            assertTrue(game.validateGuess("JAVA"));
            assertTrue(game.validateGuess("code"));
        }
    }

    // ==================== 3. Tests for validateGuess ====================

    @Nested
    @DisplayName("Validate Guess Tests")
    class ValidateGuessTests {

        @Test
        @DisplayName("Null guess is invalid")
        void nullGuess() {
            Compare4Game game = createGame("JAVA");
            assertFalse(game.validateGuess(null));
        }

        @Test
        @DisplayName("Empty string guess is invalid")
        void emptyGuess() {
            Compare4Game game = createGame("JAVA");
            assertFalse(game.validateGuess(""));
        }

        @Test
        @DisplayName("Too short guess is invalid")
        void tooShortGuess() {
            Compare4Game game = createGame("JAVA");
            assertFalse(game.validateGuess("JAV"));
        }

        @Test
        @DisplayName("Too long guess is invalid")
        void tooLongGuess() {
            Compare4Game game = createGame("JAVA");
            assertFalse(game.validateGuess("JAVAR"));
        }

        @Test
        @DisplayName("Guess with digits is invalid")
        void guessWithDigits() {
            Compare4Game game = createGame("JAVA");
            assertFalse(game.validateGuess("JA1A"));
        }

        @Test
        @DisplayName("Valid word not in dictionary is invalid")
        void wordNotInDictionary() {
            Compare4Game game = createGame("JAVA");
            assertFalse(game.validateGuess("ZZZZ"));
        }

        @Test
        @DisplayName("Valid word in dictionary is valid")
        void wordInDictionary() {
            Compare4Game game = createGame("JAVA");
            assertTrue(game.validateGuess("CODE"));
        }

        @Test
        @DisplayName("Lowercase guess is valid (case insensitive)")
        void lowercaseGuessIsValid() {
            Compare4Game game = createGame("JAVA");
            assertTrue(game.validateGuess("code"));
        }
    }

    // ==================== 4. Tests for judgeGuess (valid guesses, hit target) ====================

    @Nested
    @DisplayName("JudgeGuess Tests")
    class JudgeGuessTests {

        @Test
        @DisplayName("Exact match on first guess — WIN")
        void exactMatchFirstGuess() {
            Compare4Game game = createGame("JAVA");
            Compare4Game.CompareResult result = game.judgeGuess("JAVA");
            assertTrue(result.isExactMatch());
            assertEquals(Compare4Game.GameState.WIN, game.getGameState());
            assertEquals(1, game.getAttemptCount());
            assertEquals(6, game.getRemainingAttempts());
        }

        @Test
        @DisplayName("Exact match on second guess — WIN")
        void exactMatchSecondGuess() {
            Compare4Game game = createGame("JAVA");
            game.judgeGuess("CODE");
            Compare4Game.CompareResult result = game.judgeGuess("JAVA");
            assertTrue(result.isExactMatch());
            assertEquals(Compare4Game.GameState.WIN, game.getGameState());
            assertEquals(2, game.getAttemptCount());
        }

        @Test
        @DisplayName("Exact match on last (7th) guess — WIN")
        void exactMatchOnLastGuess() {
            Compare4Game game = createGame("JAVA");
            game.judgeGuess("CODE");
            game.judgeGuess("WORD");
            game.judgeGuess("TEST");
            game.judgeGuess("BARK");
            game.judgeGuess("BIKE");
            game.judgeGuess("BOOK");
            Compare4Game.CompareResult result = game.judgeGuess("JAVA");
            assertTrue(result.isExactMatch());
            assertEquals(Compare4Game.GameState.WIN, game.getGameState());
            assertEquals(7, game.getAttemptCount());
        }

        @Test
        @DisplayName("All mismatch result")
        void allMismatch() {
            Compare4Game game = createGame("JAVA");
            Compare4Game.CompareResult result = game.judgeGuess("CODE");
            Compare4Game.CompareStatus[] statuses = result.getStatuses();
            for (Compare4Game.CompareStatus s : statuses) {
                assertEquals(Compare4Game.CompareStatus.MISMATCH, s);
            }
            assertEquals(Compare4Game.GameState.IN_PROGRESS, game.getGameState());
        }

        @Test
        @DisplayName("Duplicate letters — example from problem: given=AABC, guess=ADAA -> [1,-1,0,-1]")
        void duplicateLettersExample() {
            Compare4Game game = createGame("AABC");
            Compare4Game.CompareResult result = game.judgeGuess("ADAA");
            Compare4Game.CompareStatus[] statuses = result.getStatuses();
            assertEquals(Compare4Game.CompareStatus.MATCH, statuses[0]);
            assertEquals(Compare4Game.CompareStatus.MISMATCH, statuses[1]);
            assertEquals(Compare4Game.CompareStatus.MISPLACED, statuses[2]);
            assertEquals(Compare4Game.CompareStatus.MISMATCH, statuses[3]);
        }

        @Test
        @DisplayName("Misplaced letters — guess has letters in wrong positions")
        void misplacedLetters() {
            Compare4Game game = createGame("ABCD");
            Compare4Game.CompareResult result = game.judgeGuess("BARK");
            Compare4Game.CompareStatus[] statuses = result.getStatuses();
            // B is misplaced (target pos 1, guess pos 0)
            assertEquals(Compare4Game.CompareStatus.MISPLACED, statuses[0]);
            // A is misplaced (target pos 0, guess pos 1)
            assertEquals(Compare4Game.CompareStatus.MISPLACED, statuses[1]);
            // R not in target
            assertEquals(Compare4Game.CompareStatus.MISMATCH, statuses[2]);
            // K not in target
            assertEquals(Compare4Game.CompareStatus.MISMATCH, statuses[3]);
        }

        @Test
        @DisplayName("Case insensitive guess produces correct result")
        void caseInsensitiveGuess() {
            Compare4Game game = createGame("JAVA");
            Compare4Game.CompareResult result = game.judgeGuess("java");
            assertTrue(result.isExactMatch());
            assertEquals(Compare4Game.GameState.WIN, game.getGameState());
        }

        @Test
        @DisplayName("Guess history records all guesses")
        void guessHistoryRecordsAll() {
            Compare4Game game = createGame("JAVA");
            game.judgeGuess("CODE");
            game.judgeGuess("WORD");
            game.judgeGuess("TEST");
            List<Compare4Game.CompareResult> history = game.getGuessHistory();
            assertEquals(3, history.size());
            assertEquals("CODE", history.get(0).getGuess());
            assertEquals("WORD", history.get(1).getGuess());
            assertEquals("TEST", history.get(2).getGuess());
        }

        @Test
        @DisplayName("Invalid guess does not count as attempt")
        void invalidGuessDoesNotCount() {
            Compare4Game game = createGame("JAVA");
            assertThrows(IllegalArgumentException.class, () -> game.judgeGuess("ZZZZ"));
            assertEquals(0, game.getAttemptCount());
            assertEquals(7, game.getRemainingAttempts());
            assertEquals(0, game.getGuessHistory().size());
        }

        @Test
        @DisplayName("judgeGuess throws when game is already won")
        void cannotGuessAfterWin() {
            Compare4Game game = createGame("JAVA");
            game.judgeGuess("JAVA");
            assertEquals(Compare4Game.GameState.WIN, game.getGameState());
            assertThrows(IllegalStateException.class, () -> game.judgeGuess("CODE"));
        }
    }

    // ==================== 5. Tests for game over (exceed max attempts) ====================

    @Nested
    @DisplayName("Game Over Tests")
    class GameOverTests {

        @Test
        @DisplayName("7 wrong guesses results in LOSE")
        void sevenWrongGuessesLose() {
            Compare4Game game = createGame("JAVA");
            game.judgeGuess("CODE");
            game.judgeGuess("WORD");
            game.judgeGuess("TEST");
            game.judgeGuess("BARK");
            game.judgeGuess("BIKE");
            game.judgeGuess("BOOK");
            game.judgeGuess("DEMO");
            assertEquals(Compare4Game.GameState.LOSE, game.getGameState());
            assertEquals(0, game.getRemainingAttempts());
        }

        @Test
        @DisplayName("Cannot guess after LOSE")
        void cannotGuessAfterLose() {
            Compare4Game game = createGame("JAVA");
            game.judgeGuess("CODE");
            game.judgeGuess("WORD");
            game.judgeGuess("TEST");
            game.judgeGuess("BARK");
            game.judgeGuess("BIKE");
            game.judgeGuess("BOOK");
            game.judgeGuess("DEMO");
            assertEquals(Compare4Game.GameState.LOSE, game.getGameState());
            assertThrows(IllegalStateException.class, () -> game.judgeGuess("JAVA"));
        }

        @Test
        @DisplayName("Custom maxTries=3 — lose after 3 wrong guesses")
        void customMaxTriesLose() {
            Compare4Game game = createGame("JAVA", 3);
            game.judgeGuess("CODE");
            game.judgeGuess("WORD");
            game.judgeGuess("TEST");
            assertEquals(Compare4Game.GameState.LOSE, game.getGameState());
            assertEquals(0, game.getRemainingAttempts());
        }

        @Test
        @DisplayName("Custom maxTries=1 — win on only attempt")
        void customMaxTriesWinOnOnly() {
            Compare4Game game = createGame("JAVA", 1);
            game.judgeGuess("JAVA");
            assertEquals(Compare4Game.GameState.WIN, game.getGameState());
        }

        @Test
        @DisplayName("Custom maxTries=1 — lose on only attempt")
        void customMaxTriesLoseOnOnly() {
            Compare4Game game = createGame("JAVA", 1);
            game.judgeGuess("CODE");
            assertEquals(Compare4Game.GameState.LOSE, game.getGameState());
        }

        @Test
        @DisplayName("History is complete after LOSE")
        void historyCompleteAfterLose() {
            Compare4Game game = createGame("JAVA", 3);
            game.judgeGuess("CODE");
            game.judgeGuess("WORD");
            game.judgeGuess("TEST");
            List<Compare4Game.CompareResult> history = game.getGuessHistory();
            assertEquals(3, history.size());
            // All should have target = JAVA
            for (Compare4Game.CompareResult r : history) {
                assertEquals("JAVA", r.getTarget());
            }
        }
    }
}
