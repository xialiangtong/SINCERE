import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShortURLSolution Tests")
public class ShortURLSolutionTest {

    private static final String BASE_URL = "https://short.ly/";

    // ==================== URLDataWrapper Tests ====================

    @Nested
    @DisplayName("URLDataWrapper Tests")
    class URLDataWrapperTests {

        @Test
        @DisplayName("constructor stores code, shortURL, and longURL")
        void testConstructorStoresAllFields() {
            ShortURLSolution.URLDataWrapper wrapper =
                new ShortURLSolution.URLDataWrapper("abc", "https://short.ly/abc", "https://example.com");

            assertEquals("abc", wrapper.getCode());
            assertEquals("https://short.ly/abc", wrapper.getShortURL());
            assertEquals("https://example.com", wrapper.getLongURL());
        }

        @Test
        @DisplayName("getCode returns the code")
        void testGetCode() {
            ShortURLSolution.URLDataWrapper wrapper =
                new ShortURLSolution.URLDataWrapper("xyz", "https://short.ly/xyz", "https://google.com");
            assertEquals("xyz", wrapper.getCode());
        }

        @Test
        @DisplayName("getShortURL returns the short URL")
        void testGetShortURL() {
            ShortURLSolution.URLDataWrapper wrapper =
                new ShortURLSolution.URLDataWrapper("xyz", "https://short.ly/xyz", "https://google.com");
            assertEquals("https://short.ly/xyz", wrapper.getShortURL());
        }

        @Test
        @DisplayName("getLongURL returns the long URL")
        void testGetLongURL() {
            ShortURLSolution.URLDataWrapper wrapper =
                new ShortURLSolution.URLDataWrapper("xyz", "https://short.ly/xyz", "https://google.com");
            assertEquals("https://google.com", wrapper.getLongURL());
        }

        @Test
        @DisplayName("toString contains all fields")
        void testToString() {
            ShortURLSolution.URLDataWrapper wrapper =
                new ShortURLSolution.URLDataWrapper("abc", "https://short.ly/abc", "https://example.com");
            String str = wrapper.toString();
            assertTrue(str.contains("abc"));
            assertTrue(str.contains("https://short.ly/abc"));
            assertTrue(str.contains("https://example.com"));
        }
    }

    // ==================== InMemoryStorageRepo Tests ====================

    @Nested
    @DisplayName("InMemoryStorageRepo Tests")
    class InMemoryStorageRepoTests {

        @Test
        @DisplayName("save and getLongURL retrieves the correct URL")
        void testSaveAndGetLongURL() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            repo.save("abc", "https://example.com");
            assertEquals("https://example.com", repo.getLongURL("abc"));
        }

        @Test
        @DisplayName("save and getCode retrieves the correct code")
        void testSaveAndGetCode() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            repo.save("abc", "https://example.com");
            assertEquals("abc", repo.getCode("https://example.com"));
        }

        @Test
        @DisplayName("getLongURL returns null for unknown code")
        void testGetLongURLReturnsNullForUnknown() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            assertNull(repo.getLongURL("nonexistent"));
        }

        @Test
        @DisplayName("getCode returns null for unknown URL")
        void testGetCodeReturnsNullForUnknown() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            assertNull(repo.getCode("https://unknown.com"));
        }

        @Test
        @DisplayName("isCodeUsed returns true for saved code")
        void testIsCodeUsedTrue() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            repo.save("abc", "https://example.com");
            assertTrue(repo.isCodeUsed("abc"));
        }

        @Test
        @DisplayName("isCodeUsed returns false for unsaved code")
        void testIsCodeUsedFalse() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            assertFalse(repo.isCodeUsed("abc"));
        }

        @Test
        @DisplayName("save multiple entries maintains all mappings")
        void testSaveMultipleEntries() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            repo.save("a1", "https://example.com");
            repo.save("b2", "https://google.com");
            repo.save("c3", "https://github.com");

            assertEquals("https://example.com", repo.getLongURL("a1"));
            assertEquals("https://google.com", repo.getLongURL("b2"));
            assertEquals("https://github.com", repo.getLongURL("c3"));
            assertEquals("a1", repo.getCode("https://example.com"));
            assertEquals("b2", repo.getCode("https://google.com"));
            assertEquals("c3", repo.getCode("https://github.com"));
        }

        @Test
        @DisplayName("save overwrites existing code mapping")
        void testSaveOverwritesExistingCode() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            repo.save("abc", "https://old.com");
            repo.save("abc", "https://new.com");
            assertEquals("https://new.com", repo.getLongURL("abc"));
        }
    }

    // ==================== Base62CodeGenerator Tests ====================

    @Nested
    @DisplayName("Base62CodeGenerator Tests")
    class Base62CodeGeneratorTests {

        @Test
        @DisplayName("generates sequential unique codes")
        void testGeneratesSequentialCodes() {
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            String code1 = gen.generateCode("anything");
            String code2 = gen.generateCode("anything");
            String code3 = gen.generateCode("anything");

            assertNotEquals(code1, code2);
            assertNotEquals(code2, code3);
            assertNotEquals(code1, code3);
        }

        @Test
        @DisplayName("code is independent of input")
        void testCodeIsIndependentOfInput() {
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            String code1 = gen.generateCode("https://example.com");
            String code2 = gen.generateCode("https://different.com");

            // Codes are sequential, not derived from input
            assertNotEquals(code1, code2);
        }

        @Test
        @DisplayName("default constructor starts counter at 1")
        void testDefaultStartsAtOne() {
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            String code = gen.generateCode("input");
            assertEquals("1", code); // Base62 of 1 is "1"
        }

        @Test
        @DisplayName("custom start value constructor")
        void testCustomStartValue() {
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator(100);
            String code = gen.generateCode("input");
            // Base62 of 100: 100 / 62 = 1 rem 38 → chars[1]='1', chars[38]='c' → "1c"
            assertEquals("1c", code);
        }

        @Test
        @DisplayName("encodes zero correctly")
        void testEncodesZero() {
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator(0);
            String code = gen.generateCode("input");
            assertEquals("0", code); // Base62 of 0 is "0"
        }

        @Test
        @DisplayName("encodes large value correctly")
        void testEncodesLargeValue() {
            // 62 in base62 = "10"
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator(62);
            String code = gen.generateCode("input");
            assertEquals("10", code);
        }

        @Test
        @DisplayName("generated codes contain only Base62 characters")
        void testCodesContainOnlyBase62Chars() {
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            for (int i = 0; i < 100; i++) {
                String code = gen.generateCode("input");
                assertTrue(code.matches("[0-9A-Za-z]+"),
                    "Code should only contain Base62 characters: " + code);
            }
        }
    }

    // ==================== MD5CodeGenerator Tests ====================

    @Nested
    @DisplayName("MD5CodeGenerator Tests")
    class MD5CodeGeneratorTests {

        @Test
        @DisplayName("generates a 6-character code")
        void testGeneratesSixCharCode() {
            ShortURLSolution.MD5CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            String code = gen.generateCode("https://example.com");
            assertEquals(6, code.length());
        }

        @Test
        @DisplayName("same input produces same code (deterministic)")
        void testSameInputSameCode() {
            ShortURLSolution.MD5CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            String code1 = gen.generateCode("https://example.com");
            String code2 = gen.generateCode("https://example.com");
            assertEquals(code1, code2);
        }

        @Test
        @DisplayName("different inputs produce different codes")
        void testDifferentInputsDifferentCodes() {
            ShortURLSolution.MD5CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            String code1 = gen.generateCode("https://example.com");
            String code2 = gen.generateCode("https://different.com");
            assertNotEquals(code1, code2);
        }

        @Test
        @DisplayName("generated code contains only Base62 characters")
        void testCodeContainsOnlyBase62Chars() {
            ShortURLSolution.MD5CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            String code = gen.generateCode("https://example.com");
            assertTrue(code.matches("[0-9A-Za-z]+"),
                "Code should only contain Base62 characters: " + code);
        }

        @Test
        @DisplayName("works with empty string input")
        void testWorksWithEmptyString() {
            ShortURLSolution.MD5CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            String code = gen.generateCode("");
            assertNotNull(code);
            assertEquals(6, code.length());
        }

        @Test
        @DisplayName("works with very long input")
        void testWorksWithLongInput() {
            ShortURLSolution.MD5CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            String longInput = "https://example.com/" + "a".repeat(10000);
            String code = gen.generateCode(longInput);
            assertNotNull(code);
            assertEquals(6, code.length());
        }
    }

    // ==================== CodeGenerator Interface Tests ====================

    @Nested
    @DisplayName("CodeGenerator Interface Tests")
    class CodeGeneratorInterfaceTests {

        @Test
        @DisplayName("Base62CodeGenerator implements CodeGenerator")
        void testBase62ImplementsInterface() {
            ShortURLSolution.CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            assertNotNull(gen.generateCode("test"));
        }

        @Test
        @DisplayName("MD5CodeGenerator implements CodeGenerator")
        void testMD5ImplementsInterface() {
            ShortURLSolution.CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            assertNotNull(gen.generateCode("test"));
        }

        @Test
        @DisplayName("custom CodeGenerator implementation works")
        void testCustomCodeGeneratorImplementation() {
            ShortURLSolution.CodeGenerator gen = input -> "FIXED_CODE";
            assertEquals("FIXED_CODE", gen.generateCode("anything"));
        }
    }

    // ==================== URLStorageRepo Interface Tests ====================

    @Nested
    @DisplayName("URLStorageRepo Interface Tests")
    class URLStorageRepoInterfaceTests {

        @Test
        @DisplayName("InMemoryStorageRepo implements URLStorageRepo")
        void testInMemoryImplementsInterface() {
            ShortURLSolution.URLStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            repo.save("code1", "https://example.com");
            assertEquals("https://example.com", repo.getLongURL("code1"));
        }

        @Test
        @DisplayName("custom URLStorageRepo implementation works")
        void testCustomStorageRepoImplementation() {
            ShortURLSolution.URLStorageRepo repo = new ShortURLSolution.URLStorageRepo() {
                private String savedCode;
                private String savedURL;

                @Override public void save(String code, String longURL) {
                    savedCode = code; savedURL = longURL;
                }
                @Override public String getLongURL(String code) {
                    return code.equals(savedCode) ? savedURL : null;
                }
                @Override public String getCode(String longURL) {
                    return longURL.equals(savedURL) ? savedCode : null;
                }
                @Override public boolean isCodeUsed(String code) {
                    return code.equals(savedCode);
                }
            };

            repo.save("x", "https://example.com");
            assertEquals("https://example.com", repo.getLongURL("x"));
            assertEquals("x", repo.getCode("https://example.com"));
            assertTrue(repo.isCodeUsed("x"));
        }
    }

    // ==================== Top-Level: getShortURL Tests ====================

    @Nested
    @DisplayName("getShortURL Tests")
    class GetShortURLTests {

        @Test
        @DisplayName("creates new short URL for new long URL")
        void testCreatesNewShortURL() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            ShortURLSolution.URLDataWrapper result = solution.getShortURL("https://example.com");

            assertNotNull(result);
            assertNotNull(result.getCode());
            assertEquals(BASE_URL + result.getCode(), result.getShortURL());
            assertEquals("https://example.com", result.getLongURL());
        }

        @Test
        @DisplayName("returns existing mapping when URL is already shortened")
        void testReturnsExistingMapping() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            ShortURLSolution.URLDataWrapper first = solution.getShortURL("https://example.com");
            ShortURLSolution.URLDataWrapper second = solution.getShortURL("https://example.com");

            // Same code returned both times
            assertEquals(first.getCode(), second.getCode());
            assertEquals(first.getShortURL(), second.getShortURL());
            assertEquals(first.getLongURL(), second.getLongURL());
        }

        @Test
        @DisplayName("existing URL does not increment counter")
        void testExistingURLDoesNotIncrementCounter() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            solution.getShortURL("https://example.com");     // counter → 1
            solution.getShortURL("https://example.com");     // existing → no counter change
            ShortURLSolution.URLDataWrapper third = solution.getShortURL("https://other.com"); // counter → 2

            assertEquals("2", third.getCode()); // confirms counter was 2, not 3
        }

        @Test
        @DisplayName("different URLs get different codes")
        void testDifferentURLsGetDifferentCodes() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            ShortURLSolution.URLDataWrapper r1 = solution.getShortURL("https://example.com");
            ShortURLSolution.URLDataWrapper r2 = solution.getShortURL("https://google.com");

            assertNotEquals(r1.getCode(), r2.getCode());
            assertNotEquals(r1.getShortURL(), r2.getShortURL());
        }

        @Test
        @DisplayName("short URL includes the base URL")
        void testShortURLIncludesBaseURL() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, "https://t.co/");

            ShortURLSolution.URLDataWrapper result = solution.getShortURL("https://example.com");
            assertTrue(result.getShortURL().startsWith("https://t.co/"));
        }

        @Test
        @DisplayName("saves to storage on new URL")
        void testSavesToStorageOnNewURL() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            ShortURLSolution.URLDataWrapper result = solution.getShortURL("https://example.com");

            // Verify it was persisted in storage
            assertEquals("https://example.com", repo.getLongURL(result.getCode()));
            assertEquals(result.getCode(), repo.getCode("https://example.com"));
        }

        @Test
        @DisplayName("works with MD5CodeGenerator")
        void testWorksWithMD5Generator() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.MD5CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            ShortURLSolution.URLDataWrapper result = solution.getShortURL("https://example.com");

            assertNotNull(result.getCode());
            assertEquals(6, result.getCode().length());
            assertEquals(BASE_URL + result.getCode(), result.getShortURL());
        }
    }

    // ==================== Top-Level: getLongURL Tests ====================

    @Nested
    @DisplayName("getLongURL Tests")
    class GetLongURLTests {

        @Test
        @DisplayName("resolves code to original URL after shortening")
        void testResolvesAfterShortening() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            ShortURLSolution.URLDataWrapper shortened = solution.getShortURL("https://example.com");
            String resolved = solution.getLongURL(shortened.getCode());

            assertEquals("https://example.com", resolved);
        }

        @Test
        @DisplayName("returns null for unknown code")
        void testReturnsNullForUnknownCode() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            assertNull(solution.getLongURL("nonexistent"));
        }

        @Test
        @DisplayName("resolves multiple shortened URLs correctly")
        void testResolvesMultipleURLs() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.Base62CodeGenerator gen = new ShortURLSolution.Base62CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            ShortURLSolution.URLDataWrapper r1 = solution.getShortURL("https://example.com");
            ShortURLSolution.URLDataWrapper r2 = solution.getShortURL("https://google.com");
            ShortURLSolution.URLDataWrapper r3 = solution.getShortURL("https://github.com");

            assertEquals("https://example.com", solution.getLongURL(r1.getCode()));
            assertEquals("https://google.com", solution.getLongURL(r2.getCode()));
            assertEquals("https://github.com", solution.getLongURL(r3.getCode()));
        }

        @Test
        @DisplayName("round-trip: shorten then resolve returns original URL")
        void testRoundTrip() {
            ShortURLSolution.InMemoryStorageRepo repo = new ShortURLSolution.InMemoryStorageRepo();
            ShortURLSolution.MD5CodeGenerator gen = new ShortURLSolution.MD5CodeGenerator();
            ShortURLSolution solution = new ShortURLSolution(repo, gen, BASE_URL);

            String originalURL = "https://www.example.com/very/long/path?query=param&foo=bar";
            ShortURLSolution.URLDataWrapper shortened = solution.getShortURL(originalURL);
            String resolved = solution.getLongURL(shortened.getCode());

            assertEquals(originalURL, resolved);
        }
    }
}
