import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Compare4Solution {

    public static final int EXACT = 1;
    public static final int MISPLACED = 0;
    public static final int ABSENT = -1;
    public static final int MAX_GUESSES = 8;
    public static final int WORD_LENGTH = 4;

    private final Set<String> dictionary;
    private final String target;
    private int guessTimes;
    private boolean solved;

    // ======================== Constructor ========================

    public Compare4Solution(Set<String> dictionary, String target) {
        if (dictionary == null || dictionary.isEmpty()) {
            throw new IllegalArgumentException("Dictionary cannot be null or empty");
        }
        // Normalize dictionary to uppercase
        Set<String> upperDic = new HashSet<>();
        for (String word : dictionary) {
            upperDic.add(word.toUpperCase());
        }
        String normalizedTarget = target == null ? null : target.toUpperCase();
        if (normalizedTarget == null || normalizedTarget.length() != WORD_LENGTH || !upperDic.contains(normalizedTarget)) {
            throw new IllegalArgumentException("Target must be a " + WORD_LENGTH + "-letter word in the dictionary");
        }
        this.dictionary = upperDic;
        this.target = normalizedTarget;
        this.guessTimes = 0;
        this.solved = false;
    }

    // ======================== Public Methods ========================

    public CompareResult guess(String input) {
        if (isGameOver()) {
            throw new IllegalStateException("Game is over");
        }
        if (input == null || input.length() != WORD_LENGTH) {
            throw new IllegalArgumentException("Guess must be exactly " + WORD_LENGTH + " letters");
        }
        String normalized = input.toUpperCase();
        if (!dictionary.contains(normalized)) {
            throw new IllegalArgumentException("Guess must be a word in the dictionary");
        }

        guessTimes++;

        int[] result = compare(normalized);

        CompareResult compareResult = new CompareResult(normalized, result);
        if (compareResult.isCorrect()) {
            solved = true;
        }
        return compareResult;
    }

    public int getGuessTimes() {
        return guessTimes;
    }

    public boolean isGameOver() {
        return solved || guessTimes >= MAX_GUESSES;
    }

    public boolean isSolved() {
        return solved;
    }

    // ======================== Private Helpers ========================

    private int[] compare(String guess) {
        int[] result = new int[WORD_LENGTH];
        Arrays.fill(result, ABSENT);
        Map<Character, Integer> remaining = new HashMap<>();

        // 1st loop: mark exact matches, collect unmatched target letters
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guess.charAt(i) == target.charAt(i)) {
                result[i] = EXACT;
            } else {
                remaining.merge(target.charAt(i), 1, Integer::sum);
            }
        }

        // 2nd loop: mark misplaced or absent for non-exact positions
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (result[i] == EXACT) continue;
            char c = guess.charAt(i);
            int count = remaining.getOrDefault(c, 0);
            if (count > 0) {
                result[i] = MISPLACED;
                remaining.put(c, count - 1);
            } else {
                result[i] = ABSENT;
            }
        }

        return result;
    }

    // ======================== Inner Class: CompareResult ========================

    public static class CompareResult {
        private final String guess;
        private final int[] result;

        public CompareResult(String guess, int[] result) {
            this.guess = guess;
            this.result = Arrays.copyOf(result, result.length);
        }

        public String getGuess() {
            return guess;
        }

        public int[] getResult() {
            return Arrays.copyOf(result, result.length);
        }

        public boolean isCorrect() {
            for (int r : result) {
                if (r != EXACT) return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(guess).append(" → [");
            for (int i = 0; i < result.length; i++) {
                if (i > 0) sb.append(", ");
                switch (result[i]) {
                    case EXACT -> sb.append("✓");
                    case MISPLACED -> sb.append("?");
                    case ABSENT -> sb.append("✗");
                    default -> sb.append(result[i]);
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    // ======================== Main ========================

    public static void main(String[] args) {
        Set<String> dic = new HashSet<>(Arrays.asList(
                "AABC", "ABCD", "ADAA", "BARK", "BIKE", "BOOK",
                "CARD", "CODE", "DEMO", "JAVA", "WORD", "TEST"
        ));

        List<String> dicList = new ArrayList<>(dic);
        String target = dicList.get((int) (Math.random() * dicList.size()));

        Compare4Solution game = new Compare4Solution(dic, target);
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to Compare4! Guess the 4-letter word.");
        System.out.println("You have " + MAX_GUESSES + " attempts. Type 'quit' to exit.");

        while (!game.isGameOver()) {
            System.out.print("Guess #" + (game.getGuessTimes() + 1) + ": ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                System.out.println("You quit. The word was: " + target);
                break;
            }

            try {
                CompareResult result = game.guess(input);
                System.out.println(result);

                if (game.isSolved()) {
                    System.out.println("Congratulations! You solved it in " + game.getGuessTimes() + " guesses.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
        }

        if (!game.isSolved()) {
            System.out.println("Game over! The word was: " + target);
        }

        scanner.close();
    }
}
