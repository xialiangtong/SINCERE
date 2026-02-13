import java.util.*;

/**
 * Compare4 CLI – 4-letter Wordle-style guessing game played in the terminal.
 *
 * Self-contained: all game logic, comparison algorithm, and CLI are in this single class.
 *
 * A random word is chosen from the dictionary. The player guesses 4-letter words
 * and receives feedback per position:
 *   [1]  = exact match (correct letter, correct position)  → shown as GREEN/🟩
 *   [0]  = misplaced   (letter in word, wrong position)    → shown as YELLOW/🟨
 *   [-1] = absent       (letter not in word)                → shown as GRAY/⬜
 *
 * Duplicate-letter rule:
 *   - Mark exact matches (1) first.
 *   - Track remaining letter counts from the target word.
 *   - For each remaining position, credit as misplaced (0) only if that letter
 *     still has remaining count; otherwise mark absent (-1).
 *
 * Usage:
 *   java Compare4CLI                  → plays with a built-in default dictionary
 *   java Compare4CLI word1 word2 ...  → plays with the provided dictionary
 */
public class Compare4CLI {

    public static final int WORD_LENGTH = 4;
    public static final int EXACT_MATCH = 1;
    public static final int MISPLACED = 0;
    public static final int ABSENT = -1;

    // ==================== GuessResult ====================

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

    // ==================== Game State ====================

    private final Set<String> dictionary;
    private final String targetWord;
    private int attempts;
    private boolean solved;
    private final List<GuessResult> history;

    public Compare4CLI(Set<String> dictionary, String targetWord) {
        if (dictionary == null || dictionary.isEmpty()) {
            throw new IllegalArgumentException("dictionary must not be null or empty");
        }
        validateWord(targetWord, "targetWord");

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

    // ==================== Core Logic ====================

    public GuessResult guess(String guess) {
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
     * Stateless comparison: compute feedback between a given word and a guess.
     *
     * Pass 1: mark exact matches (1).
     * Pass 2: for remaining positions, mark misplaced (0) if the letter still
     *         has available count in the given word, otherwise absent (-1).
     */
    public static int[] compare(String given, String guess) {
        if (given == null || given.length() != WORD_LENGTH) {
            throw new IllegalArgumentException("given must be exactly " + WORD_LENGTH + " characters");
        }
        if (guess == null || guess.length() != WORD_LENGTH) {
            throw new IllegalArgumentException("guess must be exactly " + WORD_LENGTH + " characters");
        }

        String givenUpper = given.toUpperCase();
        String guessUpper = guess.toUpperCase();

        int[] result = new int[WORD_LENGTH];
        Map<Character, Integer> remainingCounts = new HashMap<>();

        // Pass 1: exact matches
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guessUpper.charAt(i) == givenUpper.charAt(i)) {
                result[i] = EXACT_MATCH;
            } else {
                result[i] = ABSENT;
                remainingCounts.merge(givenUpper.charAt(i), 1, Integer::sum);
            }
        }

        // Pass 2: misplaced
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (result[i] != EXACT_MATCH) {
                char c = guessUpper.charAt(i);
                int count = remainingCounts.getOrDefault(c, 0);
                if (count > 0) {
                    result[i] = MISPLACED;
                    remainingCounts.put(c, count - 1);
                }
            }
        }

        return result;
    }

    // ==================== Accessors ====================

    public int getAttempts() { return attempts; }
    public boolean isSolved() { return solved; }
    public List<GuessResult> getHistory() { return new ArrayList<>(history); }

    // ==================== Validation ====================

    private static void validateWord(String word, String paramName) {
        if (word == null) {
            throw new IllegalArgumentException(paramName + " must not be null");
        }
        if (word.length() != WORD_LENGTH) {
            throw new IllegalArgumentException(
                    paramName + " must be exactly " + WORD_LENGTH + " characters, got " + word.length());
        }
    }

    // ==================== Built-in Dictionary ====================

    private static final String[] DEFAULT_DICTIONARY = {
        "BARK", "BARN", "BASE", "BEAD", "BEAM", "BEAR", "BEAT", "BEND",
        "BEST", "BIKE", "BIND", "BIRD", "BITE", "BLOW", "BLUE", "BOAT",
        "BOLD", "BOLT", "BOMB", "BOND", "BONE", "BOOK", "BORN", "BOWL",
        "BULK", "BURN", "BUST", "CAFE", "CAGE", "CAKE", "CALM", "CAME",
        "CAMP", "CARD", "CARE", "CART", "CASE", "CASH", "CAST", "CAVE",
        "CHAR", "CHIP", "CHOP", "CITE", "CLAD", "CLAM", "CLAP", "CLAY",
        "CLIP", "CLUB", "CLUE", "COAL", "COAT", "CODE", "COIL", "COIN",
        "COLD", "COME", "CONE", "COOK", "COPE", "CORD", "CORE", "CORK",
        "CORN", "COST", "COZY", "CREW", "CROP", "CROW", "CUBE", "CULT",
        "CURB", "CURE", "CURL", "CUTE", "DAME", "DAMP", "DARE", "DARK",
        "DART", "DASH", "DATE", "DAWN", "DEAL", "DEAR", "DECK", "DEFT",
        "DEMO", "DENY", "DESK", "DIAL", "DICE", "DIME", "DINE", "DIRE",
        "DIRT", "DISC", "DISH", "DOCK", "DOME", "DONE", "DOSE", "DOWN",
        "DRAG", "DRAW", "DROP", "DRUM", "DUAL", "DUEL", "DUKE", "DUMB",
        "DUMP", "DUNE", "DUSK", "DUST", "DUTY", "EACH", "EARL", "EARN",
        "EASE", "EAST", "EDGE", "EPIC", "EVEN", "EVIL", "EXAM", "FACE",
        "FACT", "FADE", "FAIL", "FAIR", "FAKE", "FALL", "FAME", "FANG",
        "FARE", "FARM", "FAST", "FATE", "FEAR", "FEAT", "FEED", "FEEL",
        "FELT", "FERN", "FILM", "FIND", "FINE", "FIRE", "FIRM", "FISH",
        "FIST", "FLAG", "FLAT", "FLAW", "FLED", "FLEW", "FLIP", "FLOW",
        "FOAM", "FOIL", "FOLD", "FOLK", "FOND", "FONT", "FOOL", "FORK",
        "FORM", "FORT", "FOUL", "FOUR", "FREE", "FROM", "FUEL", "FULL",
        "FUND", "FURY", "FUSE", "GAIN", "GALE", "GAME", "GANG", "GAPE",
        "GARB", "GATE", "GAVE", "GAZE", "GEAR", "GIFT", "GLAD", "GLOW",
        "GLUE", "GOAT", "GOLD", "GOLF", "GONE", "GORE", "GRAB", "GRAY",
        "GREW", "GRID", "GRIM", "GRIN", "GRIP", "GROW", "GULF", "GUST",
        "HACK", "HAIL", "HAIR", "HALE", "HALF", "HALT", "HAND", "HANG",
        "HARD", "HARE", "HARM", "HARP", "HATE", "HAUL", "HAVE", "HAWK",
        "HAZE", "HEAD", "HEAL", "HEAP", "HEAR", "HEAT", "HELD", "HELP",
        "HERB", "HERD", "HERE", "HERO", "HIGH", "HIKE", "HILL", "HIND",
        "HINT", "HIRE", "HOLD", "HOLE", "HOME", "HOOD", "HOOK", "HOPE",
        "HORN", "HOST", "HOUR", "HOWL", "HUGE", "HULL", "HUNG", "HUNT",
        "HURT", "HYMN", "ICON", "IDEA", "INCH", "INTO", "IRON", "ITEM",
        "JADE", "JAIL", "JERK", "JOKE", "JUMP", "JURY", "JUST", "KEEN",
        "KELP", "KEPT", "KICK", "KIDS", "KILL", "KINS", "KIND", "KING",
        "KITE", "KNIT", "KNOB", "KNOT", "KNOW", "LACE", "LACK", "LAID",
        "LAKE", "LAMB", "LAME", "LAMP", "LAND", "LANE", "LAPS", "LARD",
        "LAST", "LATE", "LAWN", "LEAD", "LEAF", "LEAN", "LEND", "LESS",
        "LIFE", "LIFT", "LIKE", "LIMB", "LIME", "LINE", "LINK", "LION",
        "LIPS", "LIST", "LIVE", "LOAD", "LOAF", "LOAN", "LOCK", "LOFT",
        "LONE", "LONG", "LOOK", "LORD", "LORE", "LOSE", "LOST", "LOUD",
        "LOVE", "LUCK", "LUMP", "LUNG", "LURE", "LURK", "MADE", "MAIL",
        "MAIN", "MAKE", "MALE", "MALT", "MANE", "MANY", "MARE", "MARK",
        "MASK", "MAST", "MATE", "MAZE", "MEAL", "MEAN", "MELT", "MENU",
        "MESH", "MILD", "MILE", "MILK", "MILL", "MIND", "MINE", "MINT",
        "MIST", "MOAT", "MODE", "MOLD", "MONK", "MOOD", "MOON", "MORE",
        "MORN", "MOST", "MOVE", "MUCH", "MULE", "MUSE", "MUST", "MYTH",
        "NAIL", "NAME", "NAVE", "NEAR", "NEAT", "NECK", "NEED", "NEST",
        "NEWS", "NEXT", "NICE", "NINE", "NODE", "NONE", "NORM", "NOSE",
        "NOTE", "NOUN", "NUDE", "OATH", "OBEY", "OMIT", "ONCE", "ONLY",
        "OPEN", "ORCA", "OVEN", "OVER", "PACE", "PACK", "PAGE", "PAID",
        "PAIL", "PAIN", "PAIR", "PALE", "PALM", "PANE", "PARK", "PART",
        "PAST", "PATH", "PEAK", "PEAR", "PEEL", "PIER", "PIKE", "PILE",
        "PINE", "PINK", "PIPE", "PLAN", "PLAY", "PLEA", "PLOT", "PLOW",
        "PLUG", "PLUM", "POEM", "POET", "POLE", "POLL", "POLO", "POND",
        "POOL", "PORE", "PORK", "PORT", "POSE", "POST", "POUR", "PRAY",
        "PREY", "PROP", "PULL", "PULP", "PUMP", "PURE", "PUSH", "QUIT",
        "RACE", "RACK", "RAFT", "RAGE", "RAID", "RAIL", "RAIN", "RAMP",
        "RANG", "RANK", "RARE", "RASH", "RATE", "RAVE", "READ", "REAL",
        "REEF", "REIN", "RELY", "RENT", "REST", "RICE", "RICH", "RIDE",
        "RIFT", "RING", "RIPE", "RISE", "RISK", "ROAD", "ROAM", "ROBE",
        "ROCK", "RODE", "ROLE", "ROLL", "ROOF", "ROOM", "ROOT", "ROPE",
        "ROSE", "RUIN", "RULE", "RUNG", "RUSH", "RUST", "SACK", "SAFE",
        "SAGE", "SAID", "SAIL", "SAKE", "SALE", "SALT", "SAME", "SAND",
        "SANE", "SANG", "SANK", "SAVE", "SEAL", "SEAM", "SEAR", "SEAT",
        "SEED", "SELF", "SEMI", "SEND", "SENT", "SHED", "SHIN", "SHIP",
        "SHOP", "SHOT", "SHOW", "SHUT", "SICK", "SIDE", "SIFT", "SIGH",
        "SIGN", "SILK", "SING", "SINK", "SITE", "SIZE", "SKIT", "SLAB",
        "SLAM", "SLAP", "SLIM", "SLIP", "SLOT", "SLOW", "SLUG", "SNAP",
        "SNIP", "SNOW", "SOAK", "SOAP", "SOAR", "SOCK", "SOFT", "SOIL",
        "SOLD", "SOLE", "SOME", "SONG", "SOON", "SORT", "SOUL", "SOUR",
        "SPAN", "SPAR", "SPIN", "SPIT", "SPOT", "SPUR", "STAB", "STAR",
        "STAY", "STEM", "STEP", "STEW", "STIR", "STOP", "STUB", "STUD",
        "SUIT", "SULK", "SURE", "SURF", "SWAN", "SWAP", "SWIM", "TABS",
        "TACK", "TAIL", "TAKE", "TALE", "TALK", "TALL", "TAME", "TANK",
        "TAPE", "TAPS", "TASK", "TAXI", "TEAM", "TEAR", "TELL", "TEND",
        "TERM", "TEST", "TEXT", "THAN", "THAT", "THEM", "THEN", "THEY",
        "THIN", "THIS", "TICK", "TIDE", "TIDY", "TIED", "TIER", "TILE",
        "TILT", "TIME", "TINY", "TIRE", "TOAD", "TOIL", "TOLD", "TOLL",
        "TOMB", "TONE", "TOOK", "TOOL", "TOPS", "TORE", "TORN", "TOUR",
        "TOWN", "TRAP", "TRAY", "TREE", "TREK", "TRIM", "TRIO", "TRIP",
        "TROT", "TRUE", "TUBE", "TUCK", "TUNA", "TUNE", "TURN", "TWIN",
        "TYPE", "UGLY", "UNDO", "UNIT", "UPON", "URGE", "USED", "USER",
        "VAIN", "VALE", "VANE", "VARY", "VAST", "VEIL", "VEIN", "VENT",
        "VERB", "VERY", "VEST", "VETO", "VICE", "VIEW", "VINE", "VOID",
        "VOLT", "VOTE", "WADE", "WAGE", "WAIT", "WAKE", "WALK", "WALL",
        "WAND", "WANT", "WARD", "WARM", "WARN", "WARP", "WARY", "WASH",
        "WAVE", "WAVY", "WAXY", "WEAK", "WEAR", "WEED", "WEEP", "WELD",
        "WELL", "WENT", "WERE", "WEST", "WHAT", "WHEN", "WHIM", "WHOM",
        "WICK", "WIDE", "WIFE", "WILD", "WILL", "WILT", "WIND", "WINE",
        "WING", "WINK", "WIPE", "WIRE", "WISE", "WISH", "WITH", "WOKE",
        "WOLF", "WOOD", "WOOL", "WORD", "WORE", "WORK", "WORM", "WORN",
        "WRAP", "WRIT", "YARD", "YARN", "YEAR", "YELL", "YOUR", "ZEAL",
        "ZERO", "ZINC", "ZONE", "ZOOM"
    };

    // ==================== ANSI Colors ====================

    private static final String ANSI_RESET  = "\033[0m";
    private static final String ANSI_GREEN  = "\033[42;30m";
    private static final String ANSI_YELLOW = "\033[43;30m";
    private static final String ANSI_GRAY   = "\033[47;30m";

    // ==================== CLI ====================

    public static void main(String[] args) {
        Set<String> dict;
        if (args.length > 0) {
            dict = new HashSet<>();
            for (String w : args) {
                if (w != null && w.length() == WORD_LENGTH) {
                    dict.add(w.toUpperCase());
                }
            }
            if (dict.isEmpty()) {
                System.out.println("No valid 4-letter words provided. Using default dictionary.");
                dict = defaultDict();
            }
        } else {
            dict = defaultDict();
        }

        Scanner scanner = new Scanner(System.in);
        boolean playAgain = true;

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║       COMPARE4 – Word Game       ║");
        System.out.println("╠══════════════════════════════════╣");
        System.out.println("║  Guess the 4-letter word!        ║");
        System.out.println("║  🟩 = correct letter & position  ║");
        System.out.println("║  🟨 = correct letter, wrong spot ║");
        System.out.println("║  ⬜ = letter not in word          ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println("Dictionary size: " + dict.size() + " words\n");

        while (playAgain) {
            String target = pickRandom(dict);
            Compare4CLI game = new Compare4CLI(dict, target);
            System.out.println("A new word has been chosen. Start guessing!\n");

            while (!game.isSolved()) {
                System.out.printf("Attempt #%d — Enter a 4-letter word: ", game.getAttempts() + 1);
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("QUIT") || input.equalsIgnoreCase("EXIT")) {
                    System.out.println("The word was: " + target);
                    System.out.println("Goodbye!");
                    scanner.close();
                    return;
                }

                if (input.equalsIgnoreCase("HINT")) {
                    printHint(game, dict);
                    continue;
                }

                try {
                    GuessResult result = game.guess(input);
                    printFeedback(input.toUpperCase(), result.getFeedback());

                    if (result.isSolved()) {
                        System.out.println("\n🎉 Solved in " + game.getAttempts() + " attempt(s)!\n");
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("  ❌ " + e.getMessage());
                }
            }

            printHistory(game);

            System.out.print("Play again? (y/n): ");
            playAgain = scanner.nextLine().trim().equalsIgnoreCase("y");
            System.out.println();
        }

        System.out.println("Thanks for playing!");
        scanner.close();
    }

    // ==================== Display Helpers ====================

    private static void printFeedback(String guess, int[] feedback) {
        StringBuilder colored = new StringBuilder("  ");
        StringBuilder legend = new StringBuilder("  ");

        for (int i = 0; i < guess.length(); i++) {
            char c = guess.charAt(i);
            switch (feedback[i]) {
                case EXACT_MATCH:
                    colored.append(ANSI_GREEN).append(" ").append(c).append(" ").append(ANSI_RESET);
                    legend.append("[1]");
                    break;
                case MISPLACED:
                    colored.append(ANSI_YELLOW).append(" ").append(c).append(" ").append(ANSI_RESET);
                    legend.append("[0]");
                    break;
                default:
                    colored.append(ANSI_GRAY).append(" ").append(c).append(" ").append(ANSI_RESET);
                    legend.append("[-1]");
                    break;
            }
            colored.append(" ");
            legend.append(" ");
        }

        System.out.println(colored);
        System.out.println(legend);
    }

    private static void printHistory(Compare4CLI game) {
        List<GuessResult> history = game.getHistory();
        if (history.isEmpty()) return;

        System.out.println("── Game Summary ──");
        for (int i = 0; i < history.size(); i++) {
            GuessResult r = history.get(i);
            System.out.printf("#%d  ", i + 1);
            int[] fb = r.getFeedback();
            for (int j = 0; j < r.getGuess().length(); j++) {
                char c = r.getGuess().charAt(j);
                switch (fb[j]) {
                    case EXACT_MATCH:
                        System.out.print(ANSI_GREEN + " " + c + " " + ANSI_RESET + " ");
                        break;
                    case MISPLACED:
                        System.out.print(ANSI_YELLOW + " " + c + " " + ANSI_RESET + " ");
                        break;
                    default:
                        System.out.print(ANSI_GRAY + " " + c + " " + ANSI_RESET + " ");
                        break;
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    private static void printHint(Compare4CLI game, Set<String> dict) {
        List<GuessResult> history = game.getHistory();
        if (history.isEmpty()) {
            System.out.println("  💡 No guesses yet — " + dict.size() + " possible words.");
            return;
        }

        List<String> candidates = new ArrayList<>();
        for (String word : dict) {
            boolean consistent = true;
            for (GuessResult r : history) {
                int[] expected = compare(word, r.getGuess());
                if (!Arrays.equals(expected, r.getFeedback())) {
                    consistent = false;
                    break;
                }
            }
            if (consistent) {
                candidates.add(word);
            }
        }

        System.out.println("  💡 " + candidates.size() + " possible word(s) remaining.");
        if (candidates.size() <= 5 && !candidates.isEmpty()) {
            System.out.print("     Candidates: ");
            for (String c : candidates) System.out.print(c + " ");
            System.out.println();
        }
    }

    // ==================== Utilities ====================

    private static Set<String> defaultDict() {
        Set<String> dict = new HashSet<>();
        for (String w : DEFAULT_DICTIONARY) {
            dict.add(w.toUpperCase());
        }
        return dict;
    }

    private static String pickRandom(Set<String> dict) {
        List<String> words = new ArrayList<>(dict);
        return words.get(new Random().nextInt(words.size()));
    }
}
