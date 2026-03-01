import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Short URL Service
 *
 * A URL shortener that creates short codes for long URLs
 * and resolves short codes back to the original URLs.
 *
 * Design:
 * - ShortURLService (top-level): orchestrates shorten/resolve
 * - URLRepository (interface): storage abstraction
 * - CodeGenerator (interface): code generation abstraction
 * - ShortURLResult (inner class): bundles code + shortURL + longURL
 */
public class ShortURLSolution {

    private final String prefix;
    private final URLRepository repository;
    private final CodeGenerator codeGenerator;

    public ShortURLSolution(String prefix, URLRepository repository, CodeGenerator codeGenerator) {
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("Prefix must not be null or empty");
        }
        this.prefix = prefix;
        this.repository = repository;
        this.codeGenerator = codeGenerator;
    }

    /**
     * Shorten a long URL.
     * - Validates input
     * - Returns existing mapping if longURL was already shortened (idempotent)
     * - Generates code, handles collisions via retry loop
     */
    public ShortURLResult shortenURL(String longURL) {
        // Input validation
        if (longURL == null || longURL.trim().isEmpty()) {
            throw new IllegalArgumentException("URL must not be null or empty");
        }
        if (!isValidURL(longURL)) {
            throw new IllegalArgumentException("Invalid URL: " + longURL);
        }

        // Duplicate check: same longURL returns same short code
        String existingCode = repository.getCode(longURL);
        if (existingCode != null) {
            return new ShortURLResult(longURL, existingCode, prefix + existingCode);
        }

        // Generate code with collision handling
        String code = codeGenerator.generateCode(longURL);
        while (repository.existCode(code)) {
            code = codeGenerator.generateCode(longURL + System.nanoTime());
        }

        // Store and return
        repository.saveURL(code, longURL);
        return new ShortURLResult(longURL, code, prefix + code);
    }

    /**
     * Resolve a short code to the original long URL.
     * Returns null if code not found.
     */
    public String getLongURL(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code must not be null or empty");
        }
        return repository.getLongURL(code);
    }

    /**
     * Simple URL validation — checks for http:// or https:// prefix.
     */
    private boolean isValidURL(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    // ==================== ShortURLResult (inner class) ====================

    /**
     * Bundles the long URL, short code, and full short URL.
     */
    public static class ShortURLResult {
        private final String longURL;
        private final String code;
        private final String shortURL;

        public ShortURLResult(String longURL, String code, String shortURL) {
            this.longURL = longURL;
            this.code = code;
            this.shortURL = shortURL;
        }

        public String getLongURL() { return longURL; }
        public String getCode() { return code; }
        public String getShortURL() { return shortURL; }

        @Override
        public String toString() {
            return "ShortURLResult{longURL='" + longURL + "', code='" + code + "', shortURL='" + shortURL + "'}";
        }
    }

    // ==================== URLRepository (interface) ====================

    /**
     * Storage abstraction for URL mappings.
     */
    public interface URLRepository {
        void saveURL(String code, String longURL);
        String getLongURL(String code);
        String getCode(String longURL);
        boolean existCode(String code);
    }

    // ==================== InMemoryURLRepository ====================

    /**
     * In-memory implementation using bidirectional maps with TTL support.
     *
     * Each entry tracks lastVisitedTime (updated on both read and write).
     * Entries where now > lastVisitedTime + keepDuration are considered expired
     * and eligible for cleanup.
     *
     * Default keep duration: 30 days.
     *
     * TODO: Implement async cleanup worker that periodically scans entries
     *       and removes expired ones (where now > lastVisitedTime + keepDuration).
     *       The worker should:
     *       1. Iterate all entries in codeToEntry
     *       2. Check if entry.isExpired(keepDuration)
     *       3. Remove from both codeToEntry and longURLToCode
     *       4. Run on a scheduled interval (e.g., every hour)
     */
    public static class InMemoryURLRepository implements URLRepository {

        /** Holds the URL mapping and its last visited timestamp. */
        public static class URLEntry {
            private final String longURL;
            private Instant lastVisitedTime;

            public URLEntry(String longURL) {
                this.longURL = longURL;
                this.lastVisitedTime = Instant.now();
            }

            public String getLongURL() { return longURL; }
            public Instant getLastVisitedTime() { return lastVisitedTime; }

            /** Touch: update lastVisitedTime to now (called on read or write). */
            public void touch() { this.lastVisitedTime = Instant.now(); }

            /** Check if this entry has expired given a keep duration. */
            public boolean isExpired(Duration keepDuration) {
                return Instant.now().isAfter(lastVisitedTime.plus(keepDuration));
            }
        }

        private static final Duration DEFAULT_KEEP_DURATION = Duration.ofDays(30);

        private final Map<String, URLEntry> codeToEntry = new HashMap<>();
        private final Map<String, String> longURLToCode = new HashMap<>();
        private final Duration keepDuration;

        public InMemoryURLRepository() {
            this(DEFAULT_KEEP_DURATION);
        }

        public InMemoryURLRepository(Duration keepDuration) {
            this.keepDuration = keepDuration;
        }

        public Duration getKeepDuration() { return keepDuration; }

        @Override
        public void saveURL(String code, String longURL) {
            codeToEntry.put(code, new URLEntry(longURL));
            longURLToCode.put(longURL, code);
        }

        @Override
        public String getLongURL(String code) {
            URLEntry entry = codeToEntry.get(code);
            if (entry == null) return null;
            entry.touch(); // update lastVisitedTime on read
            return entry.getLongURL();
        }

        @Override
        public String getCode(String longURL) {
            String code = longURLToCode.get(longURL);
            if (code != null) {
                URLEntry entry = codeToEntry.get(code);
                if (entry != null) entry.touch(); // update lastVisitedTime on read
            }
            return code;
        }

        @Override
        public boolean existCode(String code) {
            return codeToEntry.containsKey(code);
        }

        /** Expose entry for testing TTL behavior. */
        public URLEntry getEntry(String code) {
            return codeToEntry.get(code);
        }

        // TODO: Add cleanup method to be called by async worker:
        // public int cleanupExpired() {
        //     List<String> expiredCodes = codeToEntry.entrySet().stream()
        //         .filter(e -> e.getValue().isExpired(keepDuration))
        //         .map(Map.Entry::getKey)
        //         .collect(Collectors.toList());
        //     for (String code : expiredCodes) {
        //         URLEntry entry = codeToEntry.remove(code);
        //         if (entry != null) longURLToCode.remove(entry.getLongURL());
        //     }
        //     return expiredCodes.size();
        // }
    }

    // ==================== CodeGenerator (interface) ====================

    /**
     * Abstraction for generating short codes from a URL string.
     */
    public interface CodeGenerator {
        String generateCode(String input);
    }

    // ==================== Base62CodeGenerator ====================

    /**
     * Generates short codes using an auto-incrementing counter encoded in Base62.
     * Guarantees uniqueness — no collisions by design.
     */
    public static class Base62CodeGenerator implements CodeGenerator {

        private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        private final AtomicLong counter;

        public Base62CodeGenerator() {
            this.counter = new AtomicLong(1);
        }

        public Base62CodeGenerator(long startValue) {
            this.counter = new AtomicLong(startValue);
        }

        @Override
        public String generateCode(String input) {
            // input is ignored — code comes from counter
            long value = counter.getAndIncrement();
            return encodeBase62(value);
        }

        private String encodeBase62(long value) {
            if (value == 0) return String.valueOf(BASE62_CHARS.charAt(0));

            StringBuilder sb = new StringBuilder();
            while (value > 0) {
                sb.append(BASE62_CHARS.charAt((int) (value % 62)));
                value /= 62;
            }
            return sb.reverse().toString();
        }
    }

    // ==================== MD5CodeGenerator ====================

    /**
     * Generates short codes by hashing the input with MD5
     * and encoding the first 6 characters in Base62.
     * Deterministic: same input → same code (collision possible with different inputs).
     */
    public static class MD5CodeGenerator implements CodeGenerator {

        private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        private static final int CODE_LENGTH = 6;

        @Override
        public String generateCode(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(input.getBytes());

                // Convert first 8 bytes to a positive long
                long value = 0;
                for (int i = 0; i < 8; i++) {
                    value = (value << 8) | (digest[i] & 0xFF);
                }
                value = Math.abs(value);

                // Encode to Base62, take first CODE_LENGTH characters
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < CODE_LENGTH; i++) {
                    sb.append(BASE62_CHARS.charAt((int) (value % 62)));
                    value /= 62;
                }
                return sb.toString();

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not available", e);
            }
        }
    }

    // ==================== Main: Interactive CLI ====================

    /**
     * Interactive command-line URL shortener.
     *
     * Commands:
     *   shorten <longURL>   — create a short URL
     *   resolve <code>      — resolve a short code to the original URL
     *   help                — show available commands
     *   quit                — exit
     */
    public static void main(String[] args) {
        ShortURLSolution service = new ShortURLSolution(
                "https://sho.rt/",
                new InMemoryURLRepository(),
                new Base62CodeGenerator()
        );

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Short URL Service ===");
        System.out.println("Commands: shorten <url> | resolve <code> | help | quit");
        System.out.println();

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 2);
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "shorten":
                        if (parts.length < 2) {
                            System.out.println("Usage: shorten <longURL>");
                            break;
                        }
                        ShortURLResult result = service.shortenURL(parts[1]);
                        System.out.println("  Code:      " + result.getCode());
                        System.out.println("  Short URL: " + result.getShortURL());
                        System.out.println("  Long URL:  " + result.getLongURL());
                        break;

                    case "resolve":
                        if (parts.length < 2) {
                            System.out.println("Usage: resolve <code>");
                            break;
                        }
                        String longURL = service.getLongURL(parts[1]);
                        if (longURL != null) {
                            System.out.println("  Long URL: " + longURL);
                        } else {
                            System.out.println("  Not found: " + parts[1]);
                        }
                        break;

                    case "help":
                        System.out.println("Commands:");
                        System.out.println("  shorten <url>   — create a short URL");
                        System.out.println("  resolve <code>  — resolve a short code");
                        System.out.println("  help            — show this help");
                        System.out.println("  quit            — exit");
                        break;

                    case "quit":
                    case "exit":
                        System.out.println("Goodbye!");
                        scanner.close();
                        return;

                    default:
                        System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("  Error: " + e.getMessage());
            }
        }
        scanner.close();
    }
}
