import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

public class ShortURLServiceTest {

    private ShortURLService service;

    @BeforeEach
    public void setUp() {
        service = new ShortURLService();
    }

    // ==================== Shorten Tests ====================

    @Test
    public void testShortenUrl() {
        ShortURLService.ShortURLResult result = service.shorten("https://www.example.com/very/long/path");

        assertNotNull(result);
        assertNotNull(result.getShortCode());
        assertEquals(6, result.getShortCode().length());
        assertTrue(result.getShortUrl().startsWith("https://sho.rt/"));
        assertEquals("https://www.example.com/very/long/path", result.getLongUrl());
    }

    @Test
    public void testShortenSameUrlReturnsSameCode() {
        String longUrl = "https://www.example.com/same/url";

        ShortURLService.ShortURLResult result1 = service.shorten(longUrl);
        ShortURLService.ShortURLResult result2 = service.shorten(longUrl);

        assertEquals(result1.getShortCode(), result2.getShortCode());
        assertEquals(result1.getShortUrl(), result2.getShortUrl());
    }

    @Test
    public void testShortenDifferentUrlsReturnsDifferentCodes() {
        ShortURLService.ShortURLResult result1 = service.shorten("https://www.example1.com");
        ShortURLService.ShortURLResult result2 = service.shorten("https://www.example2.com");

        assertNotEquals(result1.getShortCode(), result2.getShortCode());
    }

    @Test
    public void testShortenNullUrlThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.shorten(null));
    }

    @Test
    public void testShortenEmptyUrlThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.shorten(""));
    }

    @Test
    public void testShortenWhitespaceUrlThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.shorten("   "));
    }

    // ==================== Resolve Tests ====================

    @Test
    public void testResolveShortCode() {
        String longUrl = "https://www.example.com/test";
        ShortURLService.ShortURLResult result = service.shorten(longUrl);

        String resolvedUrl = service.resolve(result.getShortCode());

        assertEquals(longUrl, resolvedUrl);
    }

    @Test
    public void testResolveNonExistentCodeThrowsException() {
        assertThrows(ShortURLService.URLNotFoundException.class, () -> service.resolve("nonexistent"));
    }

    @Test
    public void testResolveNullCodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.resolve(null));
    }

    @Test
    public void testResolveEmptyCodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> service.resolve(""));
    }

    // ==================== ShortURLResult Tests ====================

    @Test
    public void testShortURLResultGetters() {
        ShortURLService.ShortURLResult result = new ShortURLService.ShortURLResult(
            "abc123", "https://sho.rt/abc123", "https://www.example.com"
        );

        assertEquals("abc123", result.getShortCode());
        assertEquals("https://sho.rt/abc123", result.getShortUrl());
        assertEquals("https://www.example.com", result.getLongUrl());
    }

    // ==================== URLNotFoundException Tests ====================

    @Test
    public void testURLNotFoundExceptionMessage() {
        ShortURLService.URLNotFoundException ex = new ShortURLService.URLNotFoundException("Test message");
        assertEquals("Test message", ex.getMessage());
    }

    // ==================== InMemoryURLRepository Tests ====================

    @Test
    public void testInMemoryRepositorySaveAndFind() {
        ShortURLService.InMemoryURLRepository repo = new ShortURLService.InMemoryURLRepository();

        repo.save("abc123", "https://www.example.com");

        assertEquals("https://www.example.com", repo.findByCode("abc123"));
        assertEquals("abc123", repo.findCodeByLongUrl("https://www.example.com"));
        assertTrue(repo.existsByCode("abc123"));
    }

    @Test
    public void testInMemoryRepositoryFindNonExistent() {
        ShortURLService.InMemoryURLRepository repo = new ShortURLService.InMemoryURLRepository();

        assertNull(repo.findByCode("nonexistent"));
        assertNull(repo.findCodeByLongUrl("https://nonexistent.com"));
        assertFalse(repo.existsByCode("nonexistent"));
    }

    // ==================== RandomCodeGenerator Tests ====================

    @Test
    public void testRandomCodeGeneratorLength() {
        ShortURLService.RandomCodeGenerator generator = new ShortURLService.RandomCodeGenerator();

        String code = generator.generate(6);
        assertEquals(6, code.length());

        String code10 = generator.generate(10);
        assertEquals(10, code10.length());
    }

    @Test
    public void testRandomCodeGeneratorUsesValidCharacters() {
        ShortURLService.RandomCodeGenerator generator = new ShortURLService.RandomCodeGenerator();
        String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < 100; i++) {
            String code = generator.generate(6);
            for (char c : code.toCharArray()) {
                assertTrue(validChars.indexOf(c) >= 0, "Invalid character: " + c);
            }
        }
    }

    @Test
    public void testRandomCodeGeneratorProducesUniqueResults() {
        ShortURLService.RandomCodeGenerator generator = new ShortURLService.RandomCodeGenerator();
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            codes.add(generator.generate(6));
        }

        // With 62^6 possible combinations, 100 codes should all be unique
        assertEquals(100, codes.size());
    }

    // ==================== Integration Tests ====================

    @Test
    public void testFullWorkflow() {
        // Shorten multiple URLs
        ShortURLService.ShortURLResult result1 = service.shorten("https://www.google.com");
        ShortURLService.ShortURLResult result2 = service.shorten("https://www.github.com");
        ShortURLService.ShortURLResult result3 = service.shorten("https://www.stackoverflow.com");

        // Resolve them back
        assertEquals("https://www.google.com", service.resolve(result1.getShortCode()));
        assertEquals("https://www.github.com", service.resolve(result2.getShortCode()));
        assertEquals("https://www.stackoverflow.com", service.resolve(result3.getShortCode()));
    }

    @Test
    public void testGetBaseUrl() {
        assertEquals("https://sho.rt/", service.getBaseUrl());
    }

    // ==================== Custom Repository/Generator Tests ====================

    @Test
    public void testCustomCodeGenerator() {
        // Create a predictable code generator for testing
        ShortURLService.CodeGenerator fixedGenerator = length -> "FIXED1";
        ShortURLService customService = new ShortURLService(
            new ShortURLService.InMemoryURLRepository(),
            fixedGenerator
        );

        ShortURLService.ShortURLResult result = customService.shorten("https://www.example.com");

        assertEquals("FIXED1", result.getShortCode());
        assertEquals("https://sho.rt/FIXED1", result.getShortUrl());
    }

    @Test
    public void testManyUrls() {
        // Test with many URLs to ensure no collisions
        for (int i = 0; i < 100; i++) {
            String longUrl = "https://www.example.com/page/" + i;
            ShortURLService.ShortURLResult result = service.shorten(longUrl);

            assertNotNull(result.getShortCode());
            assertEquals(longUrl, service.resolve(result.getShortCode()));
        }
    }

    // ==================== MD5Generator Tests ====================

    @Test
    public void testMD5GeneratorDefaultConstructor() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator();
        
        assertEquals("", generator.getInput());
        String code = generator.generate(6);
        assertEquals(6, code.length());
    }

    @Test
    public void testMD5GeneratorWithInput() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator("https://www.example.com");
        
        assertEquals("https://www.example.com", generator.getInput());
        String code = generator.generate(6);
        assertEquals(6, code.length());
    }

    @Test
    public void testMD5GeneratorSetInput() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator();
        generator.setInput("https://www.test.com");
        
        assertEquals("https://www.test.com", generator.getInput());
    }

    @Test
    public void testMD5GeneratorSetInputNull() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator();
        generator.setInput(null);
        
        assertEquals("", generator.getInput());
    }

    @Test
    public void testMD5GeneratorConstructorWithNull() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator(null);
        
        assertEquals("", generator.getInput());
    }

    @Test
    public void testMD5GeneratorDeterministic() {
        ShortURLService.MD5Generator generator1 = new ShortURLService.MD5Generator("https://www.example.com");
        ShortURLService.MD5Generator generator2 = new ShortURLService.MD5Generator("https://www.example.com");
        
        String code1 = generator1.generate(6);
        String code2 = generator2.generate(6);
        
        assertEquals(code1, code2, "Same input should produce same code");
    }

    @Test
    public void testMD5GeneratorDifferentInputsDifferentCodes() {
        ShortURLService.MD5Generator generator1 = new ShortURLService.MD5Generator("https://www.example1.com");
        ShortURLService.MD5Generator generator2 = new ShortURLService.MD5Generator("https://www.example2.com");
        
        String code1 = generator1.generate(6);
        String code2 = generator2.generate(6);
        
        assertNotEquals(code1, code2, "Different inputs should produce different codes");
    }

    @Test
    public void testMD5GeneratorUsesValidCharacters() {
        String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator("test input");
        
        String code = generator.generate(10);
        for (char c : code.toCharArray()) {
            assertTrue(validChars.indexOf(c) >= 0, "Invalid character: " + c);
        }
    }

    @Test
    public void testMD5GeneratorDifferentLengths() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator("test");
        
        assertEquals(4, generator.generate(4).length());
        assertEquals(6, generator.generate(6).length());
        assertEquals(8, generator.generate(8).length());
        assertEquals(10, generator.generate(10).length());
    }

    @Test
    public void testMD5GeneratorGenerateFromUrl() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator();
        
        String code = generator.generateFromUrl("https://www.example.com");
        
        assertEquals(6, code.length());
        assertEquals("https://www.example.com", generator.getInput());
    }

    @Test
    public void testMD5GeneratorGenerateFromUrlDeterministic() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator();
        
        String code1 = generator.generateFromUrl("https://www.example.com");
        String code2 = generator.generateFromUrl("https://www.example.com");
        
        assertEquals(code1, code2);
    }

    @Test
    public void testServiceWithMD5Generator() {
        // Create a service that updates MD5Generator input before generating
        ShortURLService.MD5Generator md5Generator = new ShortURLService.MD5Generator();
        
        // Custom service using MD5 generator with wrapper to set input
        ShortURLService.CodeGenerator wrapperGenerator = length -> {
            // Note: In real usage, you'd need access to the URL being shortened
            return md5Generator.generate(length);
        };
        
        ShortURLService md5Service = new ShortURLService(
            new ShortURLService.InMemoryURLRepository(),
            wrapperGenerator
        );
        
        ShortURLService.ShortURLResult result = md5Service.shorten("https://www.example.com");
        
        assertNotNull(result.getShortCode());
        assertEquals(6, result.getShortCode().length());
    }

    @Test
    public void testMD5GeneratorLongLength() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator("test");
        
        // MD5 produces 16 bytes, so up to 16 characters from hash
        // Beyond that, it should pad with first character
        String code = generator.generate(20);
        assertEquals(20, code.length());
    }

    @Test
    public void testMD5GeneratorEmptyInput() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator("");
        
        String code = generator.generate(6);
        assertEquals(6, code.length());
    }

    @Test
    public void testMD5GeneratorSpecialCharacters() {
        ShortURLService.MD5Generator generator = new ShortURLService.MD5Generator("https://example.com/path?query=value&foo=bar#anchor");
        
        String code = generator.generate(6);
        assertEquals(6, code.length());
    }
}
