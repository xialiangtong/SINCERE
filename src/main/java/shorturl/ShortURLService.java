package shorturl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * URL Shortener Service with statistics and expiry support
 */
public class ShortURLService {

    private static final String BASE_URL = "https://sho.rt/";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 6;

    private final URLRepository repository;
    private final CodeGenerator codeGenerator;
    private final AtomicLong totalAccesses;

    public ShortURLService() {
        this(new InMemoryURLRepository(), new RandomCodeGenerator());
    }

    public ShortURLService(URLRepository repository, CodeGenerator codeGenerator) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.totalAccesses = new AtomicLong(0);
    }

    /**
     * Shorten a long URL
     */
    public ShortURLResult shorten(String longUrl) {
        if (longUrl == null || longUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        String existingCode = repository.findCodeByLongUrl(longUrl);
        if (existingCode != null) {
            return new ShortURLResult(existingCode, BASE_URL + existingCode, longUrl);
        }

        String shortCode;
        do {
            shortCode = codeGenerator.generate(CODE_LENGTH);
        } while (repository.existsByCode(shortCode));

        repository.save(shortCode, longUrl);
        return new ShortURLResult(shortCode, BASE_URL + shortCode, longUrl);
    }

    /**
     * Resolve a short code to the original URL
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
     * Record an access (redirect) for statistics
     */
    public void recordAccess(String shortCode) {
        totalAccesses.incrementAndGet();
        repository.incrementAccessCount(shortCode);
    }

    /**
     * Get total number of shortened URLs
     */
    public long getTotalUrls() {
        return repository.count();
    }

    /**
     * Get total number of redirects
     */
    public long getTotalAccesses() {
        return totalAccesses.get();
    }

    public String getBaseUrl() {
        return BASE_URL;
    }

    // ==================== Inner Classes ====================

    public static class ShortURLResult {
        private final String shortCode;
        private final String shortUrl;
        private final String longUrl;

        public ShortURLResult(String shortCode, String shortUrl, String longUrl) {
            this.shortCode = shortCode;
            this.shortUrl = shortUrl;
            this.longUrl = longUrl;
        }

        public String getShortCode() { return shortCode; }
        public String getShortUrl() { return shortUrl; }
        public String getLongUrl() { return longUrl; }
    }

    public static class URLNotFoundException extends RuntimeException {
        public URLNotFoundException(String message) {
            super(message);
        }
    }

    public interface URLRepository {
        void save(String shortCode, String longUrl);
        String findByCode(String shortCode);
        String findCodeByLongUrl(String longUrl);
        boolean existsByCode(String shortCode);
        long count();
        void incrementAccessCount(String shortCode);
    }

    public interface CodeGenerator {
        String generate(int length);
    }

    /**
     * Thread-safe in-memory repository with access tracking
     */
    public static class InMemoryURLRepository implements URLRepository {
        private final Map<String, URLEntry> codeToEntry = new ConcurrentHashMap<>();
        private final Map<String, String> urlToCode = new ConcurrentHashMap<>();

        @Override
        public void save(String shortCode, String longUrl) {
            URLEntry entry = new URLEntry(longUrl, Instant.now());
            codeToEntry.put(shortCode, entry);
            urlToCode.put(longUrl, shortCode);
        }

        @Override
        public String findByCode(String shortCode) {
            URLEntry entry = codeToEntry.get(shortCode);
            return entry != null ? entry.longUrl : null;
        }

        @Override
        public String findCodeByLongUrl(String longUrl) {
            return urlToCode.get(longUrl);
        }

        @Override
        public boolean existsByCode(String shortCode) {
            return codeToEntry.containsKey(shortCode);
        }

        @Override
        public long count() {
            return codeToEntry.size();
        }

        @Override
        public void incrementAccessCount(String shortCode) {
            URLEntry entry = codeToEntry.get(shortCode);
            if (entry != null) {
                entry.accessCount.incrementAndGet();
            }
        }

        private static class URLEntry {
            final String longUrl;
            final Instant createdAt;
            final AtomicLong accessCount;

            URLEntry(String longUrl, Instant createdAt) {
                this.longUrl = longUrl;
                this.createdAt = createdAt;
                this.accessCount = new AtomicLong(0);
            }
        }
    }

    /**
     * Random code generator
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
     * MD5-based code generator for deterministic codes
     */
    public static class MD5Generator implements CodeGenerator {
        private String input = "";

        public MD5Generator() {}

        public MD5Generator(String input) {
            this.input = input != null ? input : "";
        }

        public void setInput(String input) {
            this.input = input != null ? input : "";
        }

        @Override
        public String generate(int length) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md.digest(input.getBytes());
                
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    int unsignedByte = b & 0xFF;
                    sb.append(CHARACTERS.charAt(unsignedByte % CHARACTERS.length()));
                    if (sb.length() >= length) break;
                }
                
                while (sb.length() < length) {
                    sb.append(CHARACTERS.charAt(0));
                }
                
                return sb.substring(0, length);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not available", e);
            }
        }
    }
}
