import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Compare4Game {

    // ======================== Enums ========================

    public enum CompareStatus {
        MATCH(1),
        MISPLACED(0),
        MISMATCH(-1);

        private final int value;

        CompareStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum GameState {
        IN_PROGRESS,
        WIN,
        LOSE
    }

    // ======================== Inner Class ========================

    public static class CompareResult {
        private final String guess;
        private final String target;
        private final CompareStatus[] statuses;

        public CompareResult(String guess, String target, CompareStatus[] statuses) {
            this.guess = guess;
            this.target = target;
            this.statuses = statuses;
        }

        public boolean isExactMatch() {
            for (CompareStatus status : statuses) {
                if (status != CompareStatus.MATCH) {
                    return false;
                }
            }
            return true;
        }

        public String getGuess() {
            return guess;
        }

        public String getTarget() {
            return target;
        }

        public CompareStatus[] getStatuses() {
            return statuses;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Guess: ").append(guess).append(" -> [");
            for (int i = 0; i < statuses.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(statuses[i].getValue());
            }
            sb.append("]");
            return sb.toString();
        }
    }

    // ======================== Fields ========================

    private static final int WORD_LENGTH = 4;
    private final int maxTries;
    private final Set<String> dictionary;
    private final String target;
    private int attemptCount;
    private GameState gameState;
    private final List<CompareResult> guessHistory;

    // ======================== Constructor ========================

    public Compare4Game(Set<String> dictionary) {
        this(dictionary, 7);
    }

    public Compare4Game(Set<String> dictionary, int maxTries) {
        this(dictionary, maxTries, null);
    }

    /**
     * Constructor that allows specifying a target (for testing).
     * If target is null, a random word is picked from the dictionary.
     */
    Compare4Game(Set<String> dictionary, int maxTries, String target) {
        if (dictionary == null || dictionary.isEmpty()) {
            throw new IllegalArgumentException("Dictionary cannot be null or empty");
        }
        if (maxTries <= 0) {
            throw new IllegalArgumentException("maxTries must be positive");
        }

        this.maxTries = maxTries;

        // Normalize dictionary to uppercase, validating each word
        Set<String> normalizedDictionary = new HashSet<>();
        for (String word : dictionary) {
            if (validateWord(word)) {
                normalizedDictionary.add(word.toUpperCase());
            }
        }
        if (normalizedDictionary.isEmpty()) {
            throw new IllegalArgumentException("Dictionary contains no valid " + WORD_LENGTH + "-letter words");
        }
        this.dictionary = normalizedDictionary;

        if (target != null) {
            String normalizedTarget = target.toUpperCase();
            if (!this.dictionary.contains(normalizedTarget)) {
                throw new IllegalArgumentException("Target must be a valid word in the dictionary");
            }
            this.target = normalizedTarget;
        } else {
            // Randomly pick a target from the dictionary
            List<String> wordList = new ArrayList<>(this.dictionary);
            this.target = wordList.get(new java.util.Random().nextInt(wordList.size()));
        }

        this.attemptCount = 0;
        this.gameState = GameState.IN_PROGRESS;
        this.guessHistory = new ArrayList<>();
    }

    // ======================== Validation ========================

    /**
     * Validates that a word is exactly WORD_LENGTH alphabetic characters.
     */
    private boolean validateWord(String word) {
        if (word == null || word.length() != WORD_LENGTH) {
            return false;
        }
        for (char c : word.toCharArray()) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates a guess: must be a valid word AND present in the dictionary.
     */
    public boolean validateGuess(String guess) {
        if (!validateWord(guess)) {
            return false;
        }
        return dictionary.contains(guess.toUpperCase());
    }

    // ======================== Core Logic ========================

    /**
     * Judges a guess against the target using the two-pass algorithm.
     * Increments attemptCount, appends to guessHistory, and updates gameState.
     *
     * @param guess a validated 4-letter guess (must be in the dictionary)
     * @return CompareResult with per-position statuses
     * @throws IllegalStateException if game is already over
     * @throws IllegalArgumentException if guess is not valid
     */
    public CompareResult judgeGuess(String guess) {
        if (gameState != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Game is already over");
        }
        if (!validateGuess(guess)) {
            throw new IllegalArgumentException("Guess is not a valid dictionary word");
        }

        String normalizedGuess = guess.toUpperCase();
        CompareStatus[] statuses = new CompareStatus[WORD_LENGTH];
        Map<Character, Integer> letterCounts = new HashMap<>();

        // Pass 1: find exact matches, build frequency map of unmatched target letters
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (normalizedGuess.charAt(i) == target.charAt(i)) {
                statuses[i] = CompareStatus.MATCH;
            } else {
                char c = target.charAt(i);
                letterCounts.put(c, letterCounts.getOrDefault(c, 0) + 1);
            }
        }

        // Pass 2: find misplaced, remaining are mismatches
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (statuses[i] == CompareStatus.MATCH) {
                continue;
            }
            char c = normalizedGuess.charAt(i);
            int count = letterCounts.getOrDefault(c, 0);
            if (count > 0) {
                statuses[i] = CompareStatus.MISPLACED;
                letterCounts.put(c, count - 1);
            } else {
                statuses[i] = CompareStatus.MISMATCH;
            }
        }

        CompareResult result = new CompareResult(normalizedGuess, target, statuses);
        guessHistory.add(result);
        attemptCount++;

        if (result.isExactMatch()) {
            gameState = GameState.WIN;
        } else if (attemptCount >= maxTries) {
            gameState = GameState.LOSE;
        }

        return result;
    }

    // ======================== Getters ========================

    public GameState getGameState() {
        return gameState;
    }

    public int getRemainingAttempts() {
        return maxTries - attemptCount;
    }

    public List<CompareResult> getGuessHistory() {
        return new ArrayList<>(guessHistory);
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxTries() {
        return maxTries;
    }

    // ======================== CLI Main ========================

    public static void main(String[] args) {
        // Example dictionary
        Set<String> dictionary = new HashSet<>();
        dictionary.add("AABC");
        dictionary.add("ADAA");
        dictionary.add("ABCD");
        dictionary.add("WXYZ");
        dictionary.add("TEST");
        dictionary.add("JAVA");
        dictionary.add("CODE");
        dictionary.add("WORD");

        Compare4Game game = new Compare4Game(dictionary);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Compare4 Game ===");
        System.out.println("Guess the 4-letter word! You have " + game.getMaxTries() + " attempts.");
        System.out.println("Type 'QUIT' to exit.\n");

        while (game.getGameState() == GameState.IN_PROGRESS) {
            System.out.print("Enter your guess (" + game.getRemainingAttempts() + " attempts left): ");
            String input = scanner.nextLine().trim().toUpperCase();

            if ("QUIT".equals(input)) {
                System.out.println("Thanks for playing! The word was: " + game.target);
                break;
            }

            if (!game.validateGuess(input)) {
                System.out.println("Invalid guess. Must be a 4-letter word from the dictionary. Try again.");
                continue;
            }

            CompareResult result = game.judgeGuess(input);
            System.out.println(result);

            if (game.getGameState() == GameState.WIN) {
                System.out.println("Congratulations! You guessed the word!");
            } else if (game.getGameState() == GameState.LOSE) {
                System.out.println("Game over! The word was: " + game.target);
            }
        }

        scanner.close();
    }
}
