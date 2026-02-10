import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 * Bulls and Cows — a 4-digit number guessing CLI game.
 *
 * Rules:
 *  - The secret is a 4-digit number with no repeating digits and no leading zero.
 *  - The player has up to {@value #MAX_ATTEMPTS} attempts to guess it.
 *  - After each guess the player receives feedback:
 *      bulls = digits in the correct position
 *      cows  = correct digits in the wrong position
 *  - "Gutter ball!!!" is printed when bulls == 0 and cows == 0.
 *  - Across multiple games the same secret is never reused.
 */
public class BullsAndCows {

    public static final int DIGIT_COUNT = 4;
    public static final int MAX_ATTEMPTS = 7;

    // ── Inner classes ──────────────────────────────────────────

    /** Feedback for a single guess. */
    public static class Hint {
        private final int bulls;
        private final int cows;

        public Hint(int bulls, int cows) {
            this.bulls = bulls;
            this.cows = cows;
        }

        public int getBulls() { return bulls; }
        public int getCows()  { return cows; }

        public boolean isWin() {
            return bulls == DIGIT_COUNT;
        }

        public boolean isGutterBall() {
            return bulls == 0 && cows == 0;
        }

        @Override
        public String toString() {
            // TODO: implement — return e.g. "2 bulls, 1 cow"
            throw new UnsupportedOperationException("not yet implemented");
        }
    }

    /** The result of one completed game. */
    public enum GameResult { WIN, LOSE, IN_PROGRESS }

    /** A record of one guess attempt. */
    public static class Attempt {
        private final String guess;
        private final Hint hint;

        public Attempt(String guess, Hint hint) {
            this.guess = guess;
            this.hint = hint;
        }

        public String getGuess() { return guess; }
        public Hint getHint()    { return hint; }
    }

    // ── Instance fields ────────────────────────────────────────

    private final Random random;
    private final Set<String> usedSecrets;   // secrets that have been used across games
    private String secret;
    private int attemptsLeft;
    private GameResult result;
    private final List<Attempt> history;

    // ── Constructors ───────────────────────────────────────────

    public BullsAndCows() {
        this(new Random());
    }

    /** Injectable Random for deterministic testing. */
    public BullsAndCows(Random random) {
        this.random = random;
        this.usedSecrets = new HashSet<>();
        this.history = new ArrayList<>();
        this.result = GameResult.IN_PROGRESS;
        this.attemptsLeft = MAX_ATTEMPTS;
    }

    // ── Public API ─────────────────────────────────────────────

    /**
     * Start (or restart) a new game with a fresh random secret.
     * The secret will never repeat one already used.
     *
     * @throws IllegalStateException if all possible secrets have been exhausted
     */
    public void newGame() {
        // TODO: implement — generate a new unique secret, reset state
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Start a game with a specific secret (for testing purposes).
     *
     * @param secret a valid 4-digit string with unique digits, no leading zero
     * @throws IllegalArgumentException if the secret is invalid
     */
    public void newGame(String secret) {
        // TODO: implement — validate and set the secret, reset state
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Submit a guess.
     *
     * @param guess a 4-digit string
     * @return the Hint (bulls / cows) for this guess
     * @throws IllegalStateException    if no game is in progress or game is over
     * @throws IllegalArgumentException if the guess is not a valid 4-digit number
     */
    public Hint guess(String guess) {
        // TODO: implement — validate input, compute bulls & cows, update state
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Compute bulls and cows between a secret and a guess (stateless utility).
     *
     * @param secret the target number string
     * @param guess  the guessed number string
     * @return Hint with bulls and cows counts
     * @throws IllegalArgumentException if either string is not exactly 4 digits
     */
    public static Hint computeHint(String secret, String guess) {
        // TODO: implement — count bulls then cows
        throw new UnsupportedOperationException("not yet implemented");
    }

    // ── Validation ─────────────────────────────────────────────

    /**
     * Validate whether a string is a legal 4-digit guess / secret.
     *  - exactly 4 characters
     *  - all digits
     *  - first digit not '0'
     *  - no repeating digits
     *
     * @param number the string to validate
     * @return true if valid
     */
    public static boolean isValidNumber(String number) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    // ── Secret generation ──────────────────────────────────────

    /**
     * Generate a random valid 4-digit number (no repeats, no leading zero).
     *
     * @param random the Random instance to use
     * @return a valid 4-digit string
     */
    static String generateSecret(Random random) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    // ── Getters ────────────────────────────────────────────────

    public String getSecret()           { return secret; }
    public int getAttemptsLeft()        { return attemptsLeft; }
    public GameResult getResult()       { return result; }
    public List<Attempt> getHistory()   { return new ArrayList<>(history); }
    public Set<String> getUsedSecrets() { return new HashSet<>(usedSecrets); }

    // ── CLI entry point ────────────────────────────────────────

    /**
     * Run the interactive CLI game loop.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        // TODO: implement — Scanner-based CLI loop
        //   1. Print welcome message
        //   2. Call newGame()
        //   3. Loop: read guess, call guess(), print hint or win/lose
        //   4. Ask "Play again?"
        throw new UnsupportedOperationException("not yet implemented");
    }
}
