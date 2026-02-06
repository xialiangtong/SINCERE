import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Short URL Solution
 *
 * A URL shortener service that creates short codes for long URLs
 * and resolves short codes back to the original URLs.
 */
public class ShortURLSolution {

    private final URLStorageRepo storage;
    private final CodeGenerator codeGenerator;
    private final String baseURL;

    public ShortURLSolution(URLStorageRepo storage, CodeGenerator codeGenerator, String baseURL) {
        this.storage = storage;
        this.codeGenerator = codeGenerator;
        this.baseURL = baseURL;
    }

    /**
     * Shorten a long URL. Returns existing mapping if already shortened.
     */
    public URLDataWrapper getShortURL(String longURL) {
        // Check if already shortened
        String existingCode = storage.getCode(longURL);
        if (existingCode != null) {
            return new URLDataWrapper(existingCode, baseURL + existingCode, longURL);
        }

        // Generate a new code
        String code = codeGenerator.generateCode(longURL);
        storage.save(code, longURL);
        return new URLDataWrapper(code, baseURL + code, longURL);
    }

    /**
     * Resolve a short code to the original long URL.
     */
    public String getLongURL(String code) {
        return storage.getLongURL(code);
    }

    // ==================== URLDataWrapper ====================

    /**
     * Bundles the short code, full short URL, and original long URL.
     */
    public static class URLDataWrapper {
        private final String code;
        private final String shortURL;
        private final String longURL;

        public URLDataWrapper(String code, String shortURL, String longURL) {
            this.code = code;
            this.shortURL = shortURL;
            this.longURL = longURL;
        }

        public String getCode() {
            return code;
        }

        public String getShortURL() {
            return shortURL;
        }

        public String getLongURL() {
            return longURL;
        }

        @Override
        public String toString() {
            return "URLDataWrapper{code='" + code + "', shortURL='" + shortURL + "', longURL='" + longURL + "'}";
        }
    }

    // ==================== URLStorageRepo Interface ====================

    /**
     * Interface for URL storage operations.
     */
    public interface URLStorageRepo {
        void save(String code, String longURL);
        String getLongURL(String code);
        String getCode(String longURL);
        boolean isCodeUsed(String code);
    }

    // ==================== InMemoryStorageRepo ====================

    /**
     * In-memory implementation of URLStorageRepo using two-way maps.
     */
    public static class InMemoryStorageRepo implements URLStorageRepo {

        private final Map<String, String> codeToURL; // code → longURL
        private final Map<String, String> urlToCode; // longURL → code

        public InMemoryStorageRepo() {
            this.codeToURL = new HashMap<>();
            this.urlToCode = new HashMap<>();
        }

        @Override
        public void save(String code, String longURL) {
            codeToURL.put(code, longURL);
            urlToCode.put(longURL, code);
        }

        @Override
        public String getLongURL(String code) {
            return codeToURL.get(code);
        }

        @Override
        public String getCode(String longURL) {
            return urlToCode.get(longURL);
        }

        @Override
        public boolean isCodeUsed(String code) {
            return codeToURL.containsKey(code);
        }
    }

    // ==================== CodeGenerator Interface ====================

    /**
     * Interface for generating short codes.
     */
    public interface CodeGenerator {
        String generateCode(String input);
    }

    // ==================== Base62CodeGenerator ====================

    /**
     * Generates short codes using a counter encoded in Base62.
     * Guarantees uniqueness without collision.
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

                // Encode to Base62 and take first CODE_LENGTH characters
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
}
