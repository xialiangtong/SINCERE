import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * URL Shortener Service
 * Creates short codes for long URLs and resolves them back.
 */
public class ShortURLService {

    private static final String BASE_URL = "https://sho.rt/";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 6;

    private final URLRepository repository;
    private final CodeGenerator codeGenerator;

    // Default constructor with in-memory storage
    public ShortURLService() {
        this(new InMemoryURLRepository(), new RandomCodeGenerator());
    }

    // Constructor for dependency injection
    public ShortURLService(URLRepository repository, CodeGenerator codeGenerator) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
    }

    /**
     * Shorten a long URL
     * @param longUrl the original URL to shorten
     * @return ShortURLResult containing the short code and full short URL
     */
    public ShortURLResult shorten(String longUrl) {
        if (longUrl == null || longUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        // Check if URL already exists
        String existingCode = repository.findCodeByLongUrl(longUrl);
        if (existingCode != null) {
            return new ShortURLResult(existingCode, BASE_URL + existingCode, longUrl);
        }

        // Generate unique short code
        String shortCode;
        do {
            shortCode = codeGenerator.generate(CODE_LENGTH);
        } while (repository.existsByCode(shortCode));

        // Store the mapping
        repository.save(shortCode, longUrl);

        return new ShortURLResult(shortCode, BASE_URL + shortCode, longUrl);
    }

    /**
     * Resolve a short code to the original URL
     * @param shortCode the short code to resolve
     * @return the original long URL
     * @throws URLNotFoundException if the code doesn't exist
     */
    public String resolve(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }

        String longUrl = repository.findByCode(shortCode);
        if (longUrl == null) {
            throw new URLNotFoundException("Short code not found: " + shortCode);
        }

        return longUrl;
    }

    /**
     * Get the base URL used for short links
     */
    public String getBaseUrl() {
        return BASE_URL;
    }

    // ==================== Inner Classes ====================

    /**
     * Result of shortening a URL
     */
    public static class ShortURLResult {
        private final String shortCode;
        private final String shortUrl;
        private final String longUrl;

        public ShortURLResult(String shortCode, String shortUrl, String longUrl) {
            this.shortCode = shortCode;
            this.shortUrl = shortUrl;
            this.longUrl = longUrl;
        }

        public String getShortCode() {
            return shortCode;
        }

        public String getShortUrl() {
            return shortUrl;
        }

        public String getLongUrl() {
            return longUrl;
        }
    }

    /**
     * Exception for when a URL is not found
     */
    public static class URLNotFoundException extends RuntimeException {
        public URLNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Interface for URL storage
     */
    public interface URLRepository {
        void save(String shortCode, String longUrl);
        String findByCode(String shortCode);
        String findCodeByLongUrl(String longUrl);
        boolean existsByCode(String shortCode);
    }

    /**
     * Interface for code generation
     */
    public interface CodeGenerator {
        String generate(int length);
    }

    /**
     * In-memory implementation of URLRepository
     */
    public static class InMemoryURLRepository implements URLRepository {
        private final Map<String, String> codeToUrl = new HashMap<>();
        private final Map<String, String> urlToCode = new HashMap<>();

        @Override
        public void save(String shortCode, String longUrl) {
            codeToUrl.put(shortCode, longUrl);
            urlToCode.put(longUrl, shortCode);
        }

        @Override
        public String findByCode(String shortCode) {
            return codeToUrl.get(shortCode);
        }

        @Override
        public String findCodeByLongUrl(String longUrl) {
            return urlToCode.get(longUrl);
        }

        @Override
        public boolean existsByCode(String shortCode) {
            return codeToUrl.containsKey(shortCode);
        }
    }

    /**
     * Random code generator using SecureRandom
     */
    public static class RandomCodeGenerator implements CodeGenerator {
        private final SecureRandom random = new SecureRandom();

        @Override
        public String generate(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                int index = random.nextInt(CHARACTERS.length());
                sb.append(CHARACTERS.charAt(index));
            }
            return sb.toString();
        }
    }

    /**
     * MD5-based code generator
     * Generates deterministic codes based on input string's MD5 hash
     */
    public static class MD5Generator implements CodeGenerator {
        private String input;

        public MD5Generator() {
            this.input = "";
        }

        public MD5Generator(String input) {
            this.input = input != null ? input : "";
        }

        public void setInput(String input) {
            this.input = input != null ? input : "";
        }

        public String getInput() {
            return input;
        }

        @Override
        public String generate(int length) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md.digest(input.getBytes());
                
                // Convert hash bytes to base62 string
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    int unsignedByte = b & 0xFF;
                    sb.append(CHARACTERS.charAt(unsignedByte % CHARACTERS.length()));
                    if (sb.length() >= length) {
                        break;
                    }
                }
                
                // Ensure we have enough characters
                while (sb.length() < length) {
                    sb.append(CHARACTERS.charAt(0));
                }
                
                return sb.substring(0, length);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not available", e);
            }
        }

        /**
         * Generate code directly from a URL string
         */
        public String generateFromUrl(String url) {
            setInput(url);
            return generate(CODE_LENGTH);
        }
    }
}
