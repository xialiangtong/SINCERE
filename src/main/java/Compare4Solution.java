import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compare4 - A 4-letter Wordle-style guessing game.
 *
 * A dictionary (set of 4-letter words) is provided. A target word is chosen
 * from that dictionary. The player guesses words and receives feedback:
 *   1  = exact match (correct letter, correct position)
 *   0  = misplaced   (letter exists in word but in a different position)
 *  -1  = absent      (letter does not exist or has already been accounted for)
 *
 * The goal is to find the target word in as few guesses as possible.
 *
 * Duplicate-letter rule:
 *   - Mark exact matches first.
 *   - Track remaining letter counts from the target word.
 *   - For each remaining position in the guess, credit as misplaced (0) only
 *     if that letter still has remaining count; otherwise mark absent (-1).
 */
public class Compare4Solution {

    public static final int WORD_LENGTH = 4;

    public static final int EXACT_MATCH = 1;
    public static final int MISPLACED = 0;
    public static final int ABSENT = -1;

    private final Set<String> dictionary;
    private String targetWord;
    private int attempts;
    private boolean solved;
    private final List<GuessResult> history;

    /**
     * A record of a single guess attempt and its feedback.
     */
    public static class GuessResult {
        private final String guess;
        private final int[] feedback;

        public GuessResult(String guess, int[] feedback) {
            this.guess = guess;
            this.feedback = Arrays.copyOf(feedback, feedback.length);
        }

        public String getGuess() { return guess; }
        public int[] getFeedback() { return Arrays.copyOf(feedback, feedback.length); }

        public boolean isSolved() {
            for (int f : feedback) {
                if (f != EXACT_MATCH) return false;
            }
            return true;
        }
    }

    /**
     * Create a game with a dictionary and a target word.
     *
     * @param dictionary set of valid 4-letter words
     * @param targetWord the word the player must guess (must be in the dictionary)
     * @throws IllegalArgumentException if dictionary is null/empty, targetWord is invalid,
     *                                  or targetWord is not in the dictionary
     */
    public Compare4Solution(Set<String> dictionary, String targetWord) {
        if (dictionary == null || dictionary.isEmpty()) {
            throw new IllegalArgumentException("dictionary must not be null or empty");
        }
        validateWord(targetWord, "targetWord");

        // Normalize dictionary to uppercase
        this.dictionary = new HashSet<>();
        for (String word : dictionary) {
            if (word != null && word.length() == WORD_LENGTH) {
                this.dictionary.add(word.toUpperCase());
            }
        }

        String targetUpper = targetWord.toUpperCase();
        if (!this.dictionary.contains(targetUpper)) {
            throw new IllegalArgumentException("targetWord must be in the dictionary");
        }

        this.targetWord = targetUpper;
        this.attempts = 0;
        this.solved = false;
        this.history = new ArrayList<>();
    }

    /**
     * Create a standalone instance for direct compare calls (no game state).
     */
    public Compare4Solution() {
        this.dictionary = null;
        this.targetWord = null;
        this.attempts = 0;
        this.solved = false;
        this.history = new ArrayList<>();
    }

    /**
     * Make a guess against the target word. The guess must be a valid word from the dictionary.
     *
     * @param guess the guessed word
     * @return GuessResult containing the guess and its feedback
     * @throws IllegalStateException if the game is already solved
     * @throws IllegalArgumentException if guess is invalid or not in the dictionary
     */
    public GuessResult guess(String guess) {
        if (targetWord == null) {
            throw new IllegalStateException("No target word set. Use the constructor with dictionary and target.");
        }
        if (solved) {
            throw new IllegalStateException("Game is already solved in " + attempts + " attempt(s)");
        }
        validateWord(guess, "guess");

        String guessUpper = guess.toUpperCase();
        if (!dictionary.contains(guessUpper)) {
            throw new IllegalArgumentException("guess must be a word in the dictionary");
        }

        int[] feedback = compare(targetWord, guessUpper);
        attempts++;

        GuessResult result = new GuessResult(guessUpper, feedback);
        history.add(result);

        if (result.isSolved()) {
            solved = true;
        }

        return result;
    }

    /**
     * Compare a 4-letter guess against a 4-letter given word (stateless utility).
     *
     * @param given the target word (must be exactly 4 characters)
     * @param guess the guessed word (must be exactly 4 characters)
     * @return int array of length 4 with values 1, 0, or -1
     * @throws IllegalArgumentException if either argument is null or not exactly 4 characters
     */
    public int[] compare(String given, String guess) {
        validateWord(given, "given");
        validateWord(guess, "guess");

        String givenUpper = given.toUpperCase();
        String guessUpper = guess.toUpperCase();

        int[] result = new int[WORD_LENGTH];

        // Remaining letter counts from the given word (excludes exact-match positions)
        Map<Character, Integer> remainingCounts = new HashMap<>();

        // --- Pass 1: mark exact matches ---
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guessUpper.charAt(i) == givenUpper.charAt(i)) {
                result[i] = EXACT_MATCH;
            } else {
                result[i] = ABSENT; // tentatively absent; may upgrade in pass 2
                // Count this given-word letter as available for misplaced matching
                remainingCounts.merge(givenUpper.charAt(i), 1, Integer::sum);
            }
        }

        // --- Pass 2: mark misplaced letters ---
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (result[i] != EXACT_MATCH) {
                char c = guessUpper.charAt(i);
                int count = remainingCounts.getOrDefault(c, 0);
                if (count > 0) {
                    result[i] = MISPLACED;
                    remainingCounts.put(c, count - 1);
                }
                // else stays ABSENT
            }
        }

        return result;
    }

    public Set<String> getDictionary() {
        return dictionary == null ? null : new HashSet<>(dictionary);
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isSolved() {
        return solved;
    }

    public List<GuessResult> getHistory() {
        return new ArrayList<>(history);
    }

    private void validateWord(String word, String paramName) {
        if (word == null) {
            throw new IllegalArgumentException(paramName + " must not be null");
        }
        if (word.length() != WORD_LENGTH) {
            throw new IllegalArgumentException(
                    paramName + " must be exactly " + WORD_LENGTH + " characters, got " + word.length());
        }
    }
}
