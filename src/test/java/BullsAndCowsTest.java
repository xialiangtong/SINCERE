import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BullsAndCowsTest {

    private BullsAndCows game;

    @BeforeEach
    void setUp() {
        game = new BullsAndCows();
    }

    // ================================================================
    //  1. Input validation  — isValidNumber()
    // ================================================================

    @Nested
    class InputValidationTests {

        @Test
        void validFourDigitNumber() {
            assertTrue(BullsAndCows.isValidNumber("1234"));
        }

        @Test
        void validNumberStartingWith9() {
            assertTrue(BullsAndCows.isValidNumber("9876"));
        }

        @Test
        void validNumberWith1023() {
            assertTrue(BullsAndCows.isValidNumber("1023"));
        }

        @Test
        void rejectsNull() {
            assertFalse(BullsAndCows.isValidNumber(null));
        }

        @Test
        void rejectsEmptyString() {
            assertFalse(BullsAndCows.isValidNumber(""));
        }

        @Test
        void rejectsTooShort() {
            assertFalse(BullsAndCows.isValidNumber("123"));
        }

        @Test
        void rejectsTooLong() {
            assertFalse(BullsAndCows.isValidNumber("12345"));
        }

        @Test
        void rejectsLeadingZero() {
            // "0123" starts with 0
            assertFalse(BullsAndCows.isValidNumber("0123"));
        }

        @Test
        void rejectsRepeatingDigits() {
            // "1123" has two 1s
            assertFalse(BullsAndCows.isValidNumber("1123"));
        }

        @Test
        void rejectsAllSameDigits() {
            assertFalse(BullsAndCows.isValidNumber("1111"));
        }

        @Test
        void rejectsNonDigitCharacters() {
            assertFalse(BullsAndCows.isValidNumber("12ab"));
        }

        @Test
        void rejectsWithSpaces() {
            assertFalse(BullsAndCows.isValidNumber("1 23"));
        }

        @Test
        void rejectsNegativeSign() {
            assertFalse(BullsAndCows.isValidNumber("-123"));
        }
    }

    // ================================================================
    //  2. Hint computation — computeHint() (stateless)
    // ================================================================

    @Nested
    class ComputeHintTests {

        @Test
        void allBulls() {
            BullsAndCows.Hint hint = BullsAndCows.computeHint("1234", "1234");
            assertEquals(4, hint.getBulls());
            assertEquals(0, hint.getCows());
            assertTrue(hint.isWin());
        }

        @Test
        void twoBullsZeroCows() {
            // secret=1234, guess=1290 → 1,2 match; 9,0 absent
            BullsAndCows.Hint hint = BullsAndCows.computeHint("1234", "1290");
            assertEquals(2, hint.getBulls());
            assertEquals(0, hint.getCows());
        }

        @Test
        void oneBullOneCow() {
            // secret=1234, guess=1562 → pos0 '1' bull; '2' exists but at pos3 not pos2→cow
            BullsAndCows.Hint hint = BullsAndCows.computeHint("1234", "1562");
            assertEquals(1, hint.getBulls());
            assertEquals(1, hint.getCows());
        }

        @Test
        void zeroBullsFourCows() {
            // secret=1234, guess=4321 → all digits present, none in right position
            BullsAndCows.Hint hint = BullsAndCows.computeHint("1234", "4321");
            assertEquals(0, hint.getBulls());
            assertEquals(4, hint.getCows());
        }

        @Test
        void zeroBullsZeroCows_gutterBall() {
            BullsAndCows.Hint hint = BullsAndCows.computeHint("1234", "5678");
            assertEquals(0, hint.getBulls());
            assertEquals(0, hint.getCows());
            assertTrue(hint.isGutterBall());
        }

        @Test
        void zeroBullsTwoCows() {
            // secret=1234, guess=5612 → '1' cow (pos2→exists at pos0), '2' cow (pos3→exists at pos1)
            BullsAndCows.Hint hint = BullsAndCows.computeHint("1234", "5612");
            assertEquals(0, hint.getBulls());
            assertEquals(2, hint.getCows());
        }

        @Test
        void threeBullsZeroCows() {
            // secret=1234, guess=1235 → first 3 match
            BullsAndCows.Hint hint = BullsAndCows.computeHint("1234", "1235");
            assertEquals(3, hint.getBulls());
            assertEquals(0, hint.getCows());
        }

        @Test
        void computeHintThrowsForInvalidSecret() {
            assertThrows(IllegalArgumentException.class,
                    () -> BullsAndCows.computeHint("0123", "1234"));
        }

        @Test
        void computeHintThrowsForInvalidGuess() {
            assertThrows(IllegalArgumentException.class,
                    () -> BullsAndCows.computeHint("1234", "1123"));
        }
    }

    // ================================================================
    //  3. Hint inner class
    // ================================================================

    @Nested
    class HintTests {

        @Test
        void isWinOnlyWhenFourBulls() {
            assertTrue(new BullsAndCows.Hint(4, 0).isWin());
            assertFalse(new BullsAndCows.Hint(3, 1).isWin());
            assertFalse(new BullsAndCows.Hint(0, 4).isWin());
        }

        @Test
        void isGutterBallWhenZeroBoth() {
            assertTrue(new BullsAndCows.Hint(0, 0).isGutterBall());
            assertFalse(new BullsAndCows.Hint(0, 1).isGutterBall());
            assertFalse(new BullsAndCows.Hint(1, 0).isGutterBall());
        }

        @Test
        void toStringContainsBullsAndCows() {
            String str = new BullsAndCows.Hint(2, 1).toString();
            assertTrue(str.contains("2"));
            assertTrue(str.contains("1"));
        }
    }

    // ================================================================
    //  4. Game flow — guess() with newGame(secret)
    // ================================================================

    @Nested
    class GameFlowTests {

        @Test
        void winOnFirstAttempt() {
            game.newGame("1234");
            BullsAndCows.Hint hint = game.guess("1234");
            assertTrue(hint.isWin());
            assertEquals(BullsAndCows.GameResult.WIN, game.getResult());
            assertEquals(6, game.getAttemptsLeft()); // 7 - 1
        }

        @Test
        void winOnLastAttempt() {
            game.newGame("1234");
            // 6 wrong guesses
            game.guess("5678");
            game.guess("5679");
            game.guess("5687");
            game.guess("5689");
            game.guess("5697");
            game.guess("5698");
            // 7th guess is correct
            BullsAndCows.Hint hint = game.guess("1234");
            assertTrue(hint.isWin());
            assertEquals(BullsAndCows.GameResult.WIN, game.getResult());
            assertEquals(0, game.getAttemptsLeft());
        }

        @Test
        void loseAfterSevenWrongGuesses() {
            game.newGame("1234");
            game.guess("5678");
            game.guess("5679");
            game.guess("5687");
            game.guess("5689");
            game.guess("5697");
            game.guess("5698");
            game.guess("5693"); // 7th wrong guess
            assertEquals(BullsAndCows.GameResult.LOSE, game.getResult());
            assertEquals(0, game.getAttemptsLeft());
        }

        @Test
        void cannotGuessAfterWin() {
            game.newGame("1234");
            game.guess("1234");
            assertThrows(IllegalStateException.class, () -> game.guess("5678"));
        }

        @Test
        void cannotGuessAfterLose() {
            game.newGame("1234");
            for (int i = 0; i < 7; i++) {
                game.guess("5678");
            }
            assertThrows(IllegalStateException.class, () -> game.guess("1234"));
        }

        @Test
        void cannotGuessBeforeNewGame() {
            assertThrows(IllegalStateException.class, () -> game.guess("1234"));
        }

        @Test
        void attemptsDecrementEachGuess() {
            game.newGame("1234");
            assertEquals(7, game.getAttemptsLeft());
            game.guess("5678");
            assertEquals(6, game.getAttemptsLeft());
            game.guess("5679");
            assertEquals(5, game.getAttemptsLeft());
        }

        @Test
        void resultIsInProgressDuringGame() {
            game.newGame("1234");
            game.guess("5678");
            assertEquals(BullsAndCows.GameResult.IN_PROGRESS, game.getResult());
        }
    }

    // ================================================================
    //  5. Invalid guess during game
    // ================================================================

    @Nested
    class InvalidGuessTests {

        @BeforeEach
        void startGame() {
            game.newGame("1234");
        }

        @Test
        void rejectsNullGuess() {
            assertThrows(IllegalArgumentException.class, () -> game.guess(null));
        }

        @Test
        void rejectsShortGuess() {
            assertThrows(IllegalArgumentException.class, () -> game.guess("123"));
        }

        @Test
        void rejectsLongGuess() {
            assertThrows(IllegalArgumentException.class, () -> game.guess("12345"));
        }

        @Test
        void rejectsLeadingZeroGuess() {
            assertThrows(IllegalArgumentException.class, () -> game.guess("0123"));
        }

        @Test
        void rejectsRepeatingDigitsGuess() {
            assertThrows(IllegalArgumentException.class, () -> game.guess("1123"));
        }

        @Test
        void rejectsNonNumericGuess() {
            assertThrows(IllegalArgumentException.class, () -> game.guess("abcd"));
        }

        @Test
        void invalidGuessDoesNotConsumeAttempt() {
            int before = game.getAttemptsLeft();
            assertThrows(IllegalArgumentException.class, () -> game.guess("0000"));
            assertEquals(before, game.getAttemptsLeft());
        }
    }

    // ================================================================
    //  6. newGame() validation
    // ================================================================

    @Nested
    class NewGameValidationTests {

        @Test
        void rejectsInvalidSecret() {
            assertThrows(IllegalArgumentException.class, () -> game.newGame("0123"));
        }

        @Test
        void rejectsRepeatingDigitSecret() {
            assertThrows(IllegalArgumentException.class, () -> game.newGame("1123"));
        }

        @Test
        void rejectsNullSecret() {
            assertThrows(IllegalArgumentException.class, () -> game.newGame(null));
        }

        @Test
        void newGameResetsState() {
            game.newGame("1234");
            game.guess("5678"); // consume one attempt
            game.newGame("5678"); // start fresh game
            assertEquals(7, game.getAttemptsLeft());
            assertEquals(BullsAndCows.GameResult.IN_PROGRESS, game.getResult());
            assertTrue(game.getHistory().isEmpty());
        }
    }

    // ================================================================
    //  7. History tracking
    // ================================================================

    @Nested
    class HistoryTests {

        @Test
        void historyEmptyAtStart() {
            game.newGame("1234");
            assertTrue(game.getHistory().isEmpty());
        }

        @Test
        void historyRecordsEveryGuess() {
            game.newGame("1234");
            game.guess("5678");
            game.guess("1243");
            List<BullsAndCows.Attempt> history = game.getHistory();
            assertEquals(2, history.size());
            assertEquals("5678", history.get(0).getGuess());
            assertEquals("1243", history.get(1).getGuess());
        }

        @Test
        void historyIsDefensiveCopy() {
            game.newGame("1234");
            game.guess("5678");
            List<BullsAndCows.Attempt> h = game.getHistory();
            h.clear();
            assertEquals(1, game.getHistory().size());
        }

        @Test
        void historyContainsCorrectHints() {
            game.newGame("1234");
            game.guess("1290"); // 2 bulls, 0 cows
            BullsAndCows.Attempt attempt = game.getHistory().get(0);
            assertEquals(2, attempt.getHint().getBulls());
            assertEquals(0, attempt.getHint().getCows());
        }
    }

    // ================================================================
    //  8. Gutter ball
    // ================================================================

    @Nested
    class GutterBallTests {

        @Test
        void gutterBallWhenNoBullsNoCows() {
            game.newGame("1234");
            BullsAndCows.Hint hint = game.guess("5678");
            assertTrue(hint.isGutterBall());
        }

        @Test
        void notGutterBallWhenOneBull() {
            game.newGame("1234");
            BullsAndCows.Hint hint = game.guess("1567");
            assertFalse(hint.isGutterBall());
        }

        @Test
        void notGutterBallWhenOneCow() {
            game.newGame("1234");
            BullsAndCows.Hint hint = game.guess("5618");
            // '1' is a cow (exists but wrong position)
            assertFalse(hint.isGutterBall());
        }
    }

    // ================================================================
    //  9. Secret generation & no-repeat across games
    // ================================================================

    @Nested
    class SecretGenerationTests {

        @Test
        void generatedSecretIsValid() {
            String secret = BullsAndCows.generateSecret(new Random());
            assertTrue(BullsAndCows.isValidNumber(secret));
        }

        @Test
        void generatedSecretsAreNotAllTheSame() {
            // Generate many secrets and verify we get more than one unique value
            Set<String> secrets = new HashSet<>();
            Random rng = new Random(42);
            for (int i = 0; i < 50; i++) {
                secrets.add(BullsAndCows.generateSecret(rng));
            }
            assertTrue(secrets.size() > 1);
        }

        @Test
        void newGameRandomNeverRepeatsSecret() {
            BullsAndCows g = new BullsAndCows(new Random(99));
            Set<String> secrets = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                g.newGame();
                String secret = g.getSecret();
                assertFalse(secrets.contains(secret), "Repeated secret: " + secret);
                secrets.add(secret);
                // finish the game so we can start a new one
                g.guess(secret); // instant win
            }
        }

        @Test
        void usedSecretsTracked() {
            game.newGame("1234");
            game.guess("1234"); // win
            game.newGame("5678");
            Set<String> used = game.getUsedSecrets();
            assertTrue(used.contains("1234"));
            assertTrue(used.contains("5678"));
        }
    }

    // ================================================================
    //  10. Multiple games on the same instance
    // ================================================================

    @Nested
    class MultipleGameTests {

        @Test
        void canPlayMultipleGamesSequentially() {
            game.newGame("1234");
            game.guess("1234"); // win game 1

            game.newGame("5678");
            game.guess("5678"); // win game 2

            assertEquals(BullsAndCows.GameResult.WIN, game.getResult());
        }

        @Test
        void canStartNewGameAfterLoss() {
            game.newGame("1234");
            for (int i = 0; i < 7; i++) game.guess("5678");
            assertEquals(BullsAndCows.GameResult.LOSE, game.getResult());

            game.newGame("5678"); // should work fine
            assertEquals(BullsAndCows.GameResult.IN_PROGRESS, game.getResult());
            assertEquals(7, game.getAttemptsLeft());
        }
    }

    // ================================================================
    //  11. Edge case: secret is revealed via getSecret()
    // ================================================================

    @Nested
    class SecretAccessTests {

        @Test
        void getSecretReturnsSetSecret() {
            game.newGame("1234");
            assertEquals("1234", game.getSecret());
        }

        @Test
        void getSecretIsNullBeforeNewGame() {
            assertNull(game.getSecret());
        }
    }
}
