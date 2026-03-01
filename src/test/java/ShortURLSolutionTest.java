import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShortURLSolution Tests")
public class ShortURLSolutionTest {

    private static final String PREFIX = "https://sho.rt/";

    // ==================== Constructor Tests ====================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Creates service with valid arguments")
        void validConstruction() {
            var repo = new ShortURLSolution.InMemoryURLRepository();
            var gen = new ShortURLSolution.Base62CodeGenerator();
            assertDoesNotThrow(() -> new ShortURLSolution(PREFIX, repo, gen));
        }

        @Test
        @DisplayName("Throws on null prefix")
        void nullPrefix() {
            var repo = new ShortURLSolution.InMemoryURLRepository();
            var gen = new ShortURLSolution.Base62CodeGenerator();
            assertThrows(IllegalArgumentException.class, () -> new ShortURLSolution(null, repo, gen));
        }

        @Test
        @DisplayName("Throws on empty prefix")
        void emptyPrefix() {
            var repo = new ShortURLSolution.InMemoryURLRepository();
            var gen = new ShortURLSolution.Base62CodeGenerator();
            assertThrows(IllegalArgumentException.class, () -> new ShortURLSolution("", repo, gen));
        }
    }

    // ==================== ShortenURL Tests ====================

    @Nested
    @DisplayName("ShortenURL Tests")
    class ShortenURLTests {

        private ShortURLSolution service;

        @BeforeEach
        void setUp() {
            service = new ShortURLSolution(PREFIX,
                    new ShortURLSolution.InMemoryURLRepository(),
                    new ShortURLSolution.Base62CodeGenerator());
        }

        @Test
        @DisplayName("Shortens a valid URL")
        void shortenValidURL() {
            var result = service.shortenURL("https://example.com/long/path");
            assertNotNull(result);
            assertNotNull(result.getCode());
            assertFalse(result.getCode().isEmpty());
            assertEquals("https://example.com/long/path", result.getLongURL());
            assertTrue(result.getShortURL().startsWith(PREFIX));
            assertEquals(PREFIX + result.getCode(), result.getShortURL());
        }

        @Test
        @DisplayName("Different URLs produce different codes")
        void differentURLsDifferentCodes() {
            var r1 = service.shortenURL("https://example.com/page1");
            var r2 = service.shortenURL("https://example.com/page2");
            assertNotEquals(r1.getCode(), r2.getCode());
            assertNotEquals(r1.getShortURL(), r2.getShortURL());
        }

        @Test
        @DisplayName("Same URL returns same result (idempotent)")
        void sameURLSameResult() {
            var r1 = service.shortenURL("https://example.com/path");
            var r2 = service.shortenURL("https://example.com/path");
            assertEquals(r1.getCode(), r2.getCode());
            assertEquals(r1.getShortURL(), r2.getShortURL());
            assertEquals(r1.getLongURL(), r2.getLongURL());
        }

        @Test
        @DisplayName("Multiple calls to same URL don't create duplicate entries")
        void noDuplicateEntries() {
            service.shortenURL("https://example.com/path");
            service.shortenURL("https://example.com/path");
            // Resolve should still work — only one mapping exists
            var result = service.shortenURL("https://example.com/path");
            assertEquals("https://example.com/path", service.getLongURL(result.getCode()));
        }
    }

    // ==================== Input Validation Tests ====================

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        private ShortURLSolution service;

        @BeforeEach
        void setUp() {
            service = new ShortURLSolution(PREFIX,
                    new ShortURLSolution.InMemoryURLRepository(),
                    new ShortURLSolution.Base62CodeGenerator());
        }

        @Test
        @DisplayName("Throws on null URL")
        void nullURL() {
            assertThrows(IllegalArgumentException.class, () -> service.shortenURL(null));
        }

        @Test
        @DisplayName("Throws on empty URL")
        void emptyURL() {
            assertThrows(IllegalArgumentException.class, () -> service.shortenURL(""));
        }

        @Test
        @DisplayName("Throws on whitespace-only URL")
        void whitespaceURL() {
            assertThrows(IllegalArgumentException.class, () -> service.shortenURL("   "));
        }

        @Test
        @DisplayName("Throws on URL without http/https prefix")
        void invalidURLFormat() {
            assertThrows(IllegalArgumentException.class, () -> service.shortenURL("not-a-url"));
        }

        @Test
        @DisplayName("Accepts http:// URL")
        void httpURL() {
            var result = service.shortenURL("http://example.com");
            assertNotNull(result.getCode());
        }

        @Test
        @DisplayName("Accepts https:// URL")
        void httpsURL() {
            var result = service.shortenURL("https://example.com");
            assertNotNull(result.getCode());
        }

        @Test
        @DisplayName("getLongURL throws on null code")
        void getLongURLNullCode() {
            assertThrows(IllegalArgumentException.class, () -> service.getLongURL(null));
        }

        @Test
        @DisplayName("getLongURL throws on empty code")
        void getLongURLEmptyCode() {
            assertThrows(IllegalArgumentException.class, () -> service.getLongURL(""));
        }
    }

    // ==================== GetLongURL Tests ====================

    @Nested
    @DisplayName("GetLongURL Tests")
    class GetLongURLTests {

        private ShortURLSolution service;

        @BeforeEach
        void setUp() {
            service = new ShortURLSolution(PREFIX,
                    new ShortURLSolution.InMemoryURLRepository(),
                    new ShortURLSolution.Base62CodeGenerator());
        }

        @Test
        @DisplayName("Resolves a shortened URL")
        void resolveExisting() {
            var result = service.shortenURL("https://example.com/page");
            assertEquals("https://example.com/page", service.getLongURL(result.getCode()));
        }

        @Test
        @DisplayName("Returns null for unknown code")
        void resolveUnknown() {
            assertNull(service.getLongURL("nonexistent"));
        }

        @Test
        @DisplayName("Resolves multiple URLs correctly")
        void resolveMultiple() {
            var r1 = service.shortenURL("https://example.com/a");
            var r2 = service.shortenURL("https://example.com/b");
            var r3 = service.shortenURL("https://example.com/c");

            assertEquals("https://example.com/a", service.getLongURL(r1.getCode()));
            assertEquals("https://example.com/b", service.getLongURL(r2.getCode()));
            assertEquals("https://example.com/c", service.getLongURL(r3.getCode()));
        }
    }

    // ==================== Base62CodeGenerator Tests ====================

    @Nested
    @DisplayName("Base62CodeGenerator Tests")
    class Base62CodeGeneratorTests {

        @Test
        @DisplayName("Generates sequential unique codes")
        void sequentialCodes() {
            var gen = new ShortURLSolution.Base62CodeGenerator();
            String c1 = gen.generateCode("any");
            String c2 = gen.generateCode("any");
            String c3 = gen.generateCode("any");
            assertNotEquals(c1, c2);
            assertNotEquals(c2, c3);
        }

        @Test
        @DisplayName("Codes contain only base62 characters")
        void base62Chars() {
            var gen = new ShortURLSolution.Base62CodeGenerator();
            for (int i = 0; i < 100; i++) {
                String code = gen.generateCode("input");
                assertTrue(code.matches("[0-9A-Za-z]+"), "Code should be base62: " + code);
            }
        }

        @Test
        @DisplayName("Custom start value")
        void customStartValue() {
            var gen = new ShortURLSolution.Base62CodeGenerator(1000);
            String code = gen.generateCode("any");
            assertNotNull(code);
            assertFalse(code.isEmpty());
        }

        @Test
        @DisplayName("Encodes 0 correctly")
        void encodesZero() {
            var gen = new ShortURLSolution.Base62CodeGenerator(0);
            String code = gen.generateCode("any");
            assertEquals("0", code);
        }
    }

    // ==================== MD5CodeGenerator Tests ====================

    @Nested
    @DisplayName("MD5CodeGenerator Tests")
    class MD5CodeGeneratorTests {

        @Test
        @DisplayName("Generates deterministic codes")
        void deterministic() {
            var gen = new ShortURLSolution.MD5CodeGenerator();
            String c1 = gen.generateCode("https://example.com");
            String c2 = gen.generateCode("https://example.com");
            assertEquals(c1, c2);
        }

        @Test
        @DisplayName("Different inputs produce different codes (usually)")
        void differentInputs() {
            var gen = new ShortURLSolution.MD5CodeGenerator();
            String c1 = gen.generateCode("https://example.com/a");
            String c2 = gen.generateCode("https://example.com/b");
            assertNotEquals(c1, c2);
        }

        @Test
        @DisplayName("Code length is 6")
        void codeLength() {
            var gen = new ShortURLSolution.MD5CodeGenerator();
            String code = gen.generateCode("https://example.com");
            assertEquals(6, code.length());
        }

        @Test
        @DisplayName("Codes contain only base62 characters")
        void base62Chars() {
            var gen = new ShortURLSolution.MD5CodeGenerator();
            for (int i = 0; i < 100; i++) {
                String code = gen.generateCode("https://example.com/" + i);
                assertTrue(code.matches("[0-9A-Za-z]+"), "Code should be base62: " + code);
            }
        }
    }

    // ==================== InMemoryURLRepository Tests ====================

    @Nested
    @DisplayName("InMemoryURLRepository Tests")
    class InMemoryURLRepositoryTests {

        private ShortURLSolution.InMemoryURLRepository repo;

        @BeforeEach
        void setUp() {
            repo = new ShortURLSolution.InMemoryURLRepository();
        }

        @Test
        @DisplayName("Save and retrieve by code")
        void saveAndGetByCode() {
            repo.saveURL("abc", "https://example.com");
            assertEquals("https://example.com", repo.getLongURL("abc"));
        }

        @Test
        @DisplayName("Save and retrieve by longURL")
        void saveAndGetByLongURL() {
            repo.saveURL("abc", "https://example.com");
            assertEquals("abc", repo.getCode("https://example.com"));
        }

        @Test
        @DisplayName("existCode returns true for saved code")
        void existCodeTrue() {
            repo.saveURL("abc", "https://example.com");
            assertTrue(repo.existCode("abc"));
        }

        @Test
        @DisplayName("existCode returns false for unknown code")
        void existCodeFalse() {
            assertFalse(repo.existCode("unknown"));
        }

        @Test
        @DisplayName("Returns null for unknown code")
        void unknownCode() {
            assertNull(repo.getLongURL("unknown"));
        }

        @Test
        @DisplayName("Returns null for unknown longURL")
        void unknownLongURL() {
            assertNull(repo.getCode("https://unknown.com"));
        }
    }

    // ==================== Collision Handling Tests ====================

    @Nested
    @DisplayName("Collision Handling Tests")
    class CollisionHandlingTests {

        @Test
        @DisplayName("Handles collision with MD5 generator by retrying")
        void md5CollisionRetry() {
            var repo = new ShortURLSolution.InMemoryURLRepository();
            var gen = new ShortURLSolution.MD5CodeGenerator();
            var service = new ShortURLSolution(PREFIX, repo, gen);

            // Shorten two different URLs — both should succeed even if hash collides
            var r1 = service.shortenURL("https://example.com/page1");
            var r2 = service.shortenURL("https://example.com/page2");

            assertNotNull(r1.getCode());
            assertNotNull(r2.getCode());
            assertNotEquals(r1.getCode(), r2.getCode());

            // Both resolve correctly
            assertEquals("https://example.com/page1", service.getLongURL(r1.getCode()));
            assertEquals("https://example.com/page2", service.getLongURL(r2.getCode()));
        }
    }

    // ==================== End-to-End Scenario Tests ====================

    @Nested
    @DisplayName("End-to-End Scenario Tests")
    class EndToEndTests {

        @Test
        @DisplayName("Full workflow with Base62 generator")
        void fullWorkflowBase62() {
            var service = new ShortURLSolution(PREFIX,
                    new ShortURLSolution.InMemoryURLRepository(),
                    new ShortURLSolution.Base62CodeGenerator());

            // Shorten
            var result = service.shortenURL("https://example.com/very/long/path?q=hello");
            assertNotNull(result);
            assertTrue(result.getShortURL().startsWith(PREFIX));

            // Resolve
            String resolved = service.getLongURL(result.getCode());
            assertEquals("https://example.com/very/long/path?q=hello", resolved);

            // Duplicate returns same
            var duplicate = service.shortenURL("https://example.com/very/long/path?q=hello");
            assertEquals(result.getCode(), duplicate.getCode());
        }

        @Test
        @DisplayName("Full workflow with MD5 generator")
        void fullWorkflowMD5() {
            var service = new ShortURLSolution(PREFIX,
                    new ShortURLSolution.InMemoryURLRepository(),
                    new ShortURLSolution.MD5CodeGenerator());

            var result = service.shortenURL("https://example.com/page");
            assertNotNull(result);
            assertEquals(6, result.getCode().length());

            String resolved = service.getLongURL(result.getCode());
            assertEquals("https://example.com/page", resolved);
        }

        @Test
        @DisplayName("Shorten many URLs without failure")
        void shortenManyURLs() {
            var service = new ShortURLSolution(PREFIX,
                    new ShortURLSolution.InMemoryURLRepository(),
                    new ShortURLSolution.Base62CodeGenerator());

            for (int i = 0; i < 1000; i++) {
                var result = service.shortenURL("https://example.com/page/" + i);
                assertNotNull(result.getCode());
                assertEquals("https://example.com/page/" + i, service.getLongURL(result.getCode()));
            }
        }
    }

    // ==================== ShortURLResult Tests ====================

    @Nested
    @DisplayName("ShortURLResult Tests")
    class ShortURLResultTests {

        @Test
        @DisplayName("Constructor sets all fields")
        void constructorSetsFields() {
            var result = new ShortURLSolution.ShortURLResult("https://example.com", "Ab3k", "https://sho.rt/Ab3k");
            assertEquals("https://example.com", result.getLongURL());
            assertEquals("Ab3k", result.getCode());
            assertEquals("https://sho.rt/Ab3k", result.getShortURL());
        }

        @Test
        @DisplayName("toString includes all fields")
        void toStringContainsFields() {
            var result = new ShortURLSolution.ShortURLResult("https://example.com", "Ab3k", "https://sho.rt/Ab3k");
            String str = result.toString();
            assertTrue(str.contains("https://example.com"));
            assertTrue(str.contains("Ab3k"));
            assertTrue(str.contains("https://sho.rt/Ab3k"));
        }

        @Test
        @DisplayName("Fields are immutable via getters")
        void fieldsImmutable() {
            var result = new ShortURLSolution.ShortURLResult("https://a.com", "x", "https://sho.rt/x");
            // getters return same value on multiple calls
            assertEquals(result.getLongURL(), result.getLongURL());
            assertEquals(result.getCode(), result.getCode());
            assertEquals(result.getShortURL(), result.getShortURL());
        }
    }

    // ==================== URLRepository Interface Contract Tests ====================

    @Nested
    @DisplayName("URLRepository Contract Tests")
    class URLRepositoryContractTests {

        @Test
        @DisplayName("InMemoryURLRepository implements URLRepository")
        void implementsInterface() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            assertNotNull(repo);
        }

        @Test
        @DisplayName("saveURL and getLongURL round-trip")
        void saveAndGetLongURL() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            repo.saveURL("code1", "https://example.com/a");
            assertEquals("https://example.com/a", repo.getLongURL("code1"));
        }

        @Test
        @DisplayName("saveURL and getCode round-trip")
        void saveAndGetCode() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            repo.saveURL("code1", "https://example.com/a");
            assertEquals("code1", repo.getCode("https://example.com/a"));
        }

        @Test
        @DisplayName("existCode true after save")
        void existCodeAfterSave() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            repo.saveURL("code1", "https://example.com/a");
            assertTrue(repo.existCode("code1"));
        }

        @Test
        @DisplayName("existCode false before save")
        void existCodeBeforeSave() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            assertFalse(repo.existCode("code1"));
        }

        @Test
        @DisplayName("getLongURL returns null for unknown code")
        void getLongURLUnknown() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            assertNull(repo.getLongURL("unknown"));
        }

        @Test
        @DisplayName("getCode returns null for unknown longURL")
        void getCodeUnknown() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            assertNull(repo.getCode("https://unknown.com"));
        }

        @Test
        @DisplayName("Multiple entries stored independently")
        void multipleEntries() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            repo.saveURL("c1", "https://a.com");
            repo.saveURL("c2", "https://b.com");
            repo.saveURL("c3", "https://c.com");

            assertEquals("https://a.com", repo.getLongURL("c1"));
            assertEquals("https://b.com", repo.getLongURL("c2"));
            assertEquals("https://c.com", repo.getLongURL("c3"));
            assertEquals("c1", repo.getCode("https://a.com"));
            assertEquals("c2", repo.getCode("https://b.com"));
            assertEquals("c3", repo.getCode("https://c.com"));
        }

        @Test
        @DisplayName("Overwriting same code replaces longURL")
        void overwriteCode() {
            ShortURLSolution.URLRepository repo = new ShortURLSolution.InMemoryURLRepository();
            repo.saveURL("c1", "https://old.com");
            repo.saveURL("c1", "https://new.com");
            assertEquals("https://new.com", repo.getLongURL("c1"));
        }
    }

    // ==================== CodeGenerator Interface Contract Tests ====================

    @Nested
    @DisplayName("CodeGenerator Contract Tests")
    class CodeGeneratorContractTests {

        @Test
        @DisplayName("Base62CodeGenerator implements CodeGenerator")
        void base62ImplementsInterface() {
            ShortURLSolution.CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            assertNotNull(gen);
        }

        @Test
        @DisplayName("MD5CodeGenerator implements CodeGenerator")
        void md5ImplementsInterface() {
            ShortURLSolution.CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            assertNotNull(gen);
        }

        @Test
        @DisplayName("Both generators return non-null, non-empty codes")
        void bothReturnValidCodes() {
            ShortURLSolution.CodeGenerator base62 = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution.CodeGenerator md5 = new ShortURLSolution.MD5CodeGenerator();

            String c1 = base62.generateCode("https://example.com");
            String c2 = md5.generateCode("https://example.com");

            assertNotNull(c1);
            assertFalse(c1.isEmpty());
            assertNotNull(c2);
            assertFalse(c2.isEmpty());
        }

        @Test
        @DisplayName("Service works with any CodeGenerator implementation")
        void serviceWorksWithAnyGenerator() {
            // Custom inline implementation
            ShortURLSolution.CodeGenerator customGen = input -> "custom_" + input.hashCode();
            var service = new ShortURLSolution(PREFIX,
                    new ShortURLSolution.InMemoryURLRepository(),
                    customGen);

            var result = service.shortenURL("https://example.com");
            assertNotNull(result.getCode());
            assertTrue(result.getCode().startsWith("custom_"));
            assertEquals("https://example.com", service.getLongURL(result.getCode()));
        }

        @Test
        @DisplayName("Service works with custom URLRepository implementation")
        void serviceWorksWithCustomRepo() {
            // Custom inline implementation
            ShortURLSolution.URLRepository customRepo = new ShortURLSolution.URLRepository() {
                private final java.util.Map<String, String> codes = new java.util.HashMap<>();
                private final java.util.Map<String, String> urls = new java.util.HashMap<>();

                @Override public void saveURL(String code, String longURL) {
                    codes.put(code, longURL);
                    urls.put(longURL, code);
                }
                @Override public String getLongURL(String code) { return codes.get(code); }
                @Override public String getCode(String longURL) { return urls.get(longURL); }
                @Override public boolean existCode(String code) { return codes.containsKey(code); }
            };

            var service = new ShortURLSolution(PREFIX, customRepo, new ShortURLSolution.Base62CodeGenerator());
            var result = service.shortenURL("https://example.com");
            assertEquals("https://example.com", service.getLongURL(result.getCode()));
        }
    }
}
