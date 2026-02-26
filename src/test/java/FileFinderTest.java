import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@DisplayName("FileFinder Tests")
public class FileFinderTest {

    // ==================== Shared Fixtures ====================

    /**
     * Tree used in most tests:
     *
     * root/
     *   readme.md          (100B, epoch 1000)
     *   src/
     *     App.java         (500B, epoch 2000)
     *     Utils.java       (200B, epoch 3000)
     *     test/
     *       AppTest.java   (300B, epoch 4000)
     *   docs/
     *     guide.md         (150B, epoch 5000)
     *     api.txt          (50B,  epoch 6000)
     *   build/
     *     app.jar          (10000B, epoch 7000)
     *     lib.jar          (8000B,  epoch 7000)
     */
    private FileFinder.Directory root;

    // individual files for direct assertions
    private FileFinder.File readmeMd;
    private FileFinder.File appJava;
    private FileFinder.File utilsJava;
    private FileFinder.File appTestJava;
    private FileFinder.File guideMd;
    private FileFinder.File apiTxt;
    private FileFinder.File appJar;
    private FileFinder.File libJar;

    @BeforeEach
    void setUp() {
        readmeMd     = new FileFinder.File("readme.md",     100,   1000);
        appJava      = new FileFinder.File("App.java",      500,   2000);
        utilsJava    = new FileFinder.File("Utils.java",    200,   3000);
        appTestJava  = new FileFinder.File("AppTest.java",  300,   4000);
        guideMd      = new FileFinder.File("guide.md",      150,   5000);
        apiTxt       = new FileFinder.File("api.txt",        50,   6000);
        appJar       = new FileFinder.File("app.jar",      10000,  7000);
        libJar       = new FileFinder.File("lib.jar",       8000,  7000);

        FileFinder.Directory test = new FileFinder.Directory("test").addChild(appTestJava);
        FileFinder.Directory src  = new FileFinder.Directory("src")
                .addChild(appJava).addChild(utilsJava).addChild(test);
        FileFinder.Directory docs = new FileFinder.Directory("docs")
                .addChild(guideMd).addChild(apiTxt);
        FileFinder.Directory build = new FileFinder.Directory("build")
                .addChild(appJar).addChild(libJar);

        root = new FileFinder.Directory("root")
                .addChild(readmeMd).addChild(src).addChild(docs).addChild(build);
    }

    // ==================== FsNode / File / Directory ====================

    @Nested
    @DisplayName("FsNode Tests")
    class FsNodeTests {

        @Test
        @DisplayName("File returns correct metadata")
        void fileMetadata() {
            assertEquals("App.java", appJava.getName());
            assertEquals(500, appJava.getSizeBytes());
            assertEquals(2000, appJava.getLastModifiedEpochMs());
            assertEquals(".java", appJava.getExtension());
            assertFalse(appJava.isDirectory());
        }

        @Test
        @DisplayName("File with no extension returns empty string")
        void fileNoExtension() {
            FileFinder.File noExt = new FileFinder.File("Makefile", 10, 100);
            assertEquals("", noExt.getExtension());
        }

        @Test
        @DisplayName("File with multiple dots returns last extension")
        void fileMultipleDots() {
            FileFinder.File multi = new FileFinder.File("archive.tar.gz", 500, 100);
            assertEquals(".gz", multi.getExtension());
        }

        @Test
        @DisplayName("Directory is recognized as directory")
        void directoryIsDirectory() {
            assertTrue(root.isDirectory());
        }

        @Test
        @DisplayName("Directory getChildren returns children")
        void directoryChildren() {
            // root has 4 children: readme.md, src/, docs/, build/
            assertEquals(4, root.getChildren().size());
        }

        @Test
        @DisplayName("Directory children list is unmodifiable")
        void directoryChildrenUnmodifiable() {
            assertThrows(UnsupportedOperationException.class,
                    () -> root.getChildren().add(new FileFinder.File("x.txt", 1, 1)));
        }

        @Test
        @DisplayName("Null or empty name throws")
        void nullOrEmptyName() {
            assertThrows(IllegalArgumentException.class, () -> new FileFinder.File(null, 1, 1));
            assertThrows(IllegalArgumentException.class, () -> new FileFinder.File("", 1, 1));
            assertThrows(IllegalArgumentException.class, () -> new FileFinder.Directory(null));
            assertThrows(IllegalArgumentException.class, () -> new FileFinder.Directory(""));
        }

        @Test
        @DisplayName("addChild with null throws")
        void addNullChild() {
            FileFinder.Directory d = new FileFinder.Directory("d");
            assertThrows(IllegalArgumentException.class, () -> d.addChild(null));
        }
    }

    // ==================== Leaf Filter Tests ====================

    @Nested
    @DisplayName("SizeFilter Tests")
    class SizeFilterTests {

        @Test
        @DisplayName("Matches files larger than threshold")
        void matchesLarger() {
            FileFinder.FileFilter f = new FileFinder.SizeFilter(200);
            assertTrue(f.matches(appJava));    // 500 > 200
            assertFalse(f.matches(utilsJava)); // 200 > 200 → false (strictly greater)
            assertFalse(f.matches(readmeMd));  // 100 > 200 → false
        }

        @Test
        @DisplayName("Threshold 0 matches all positive-sized files")
        void thresholdZero() {
            FileFinder.FileFilter f = new FileFinder.SizeFilter(0);
            assertTrue(f.matches(apiTxt)); // 50 > 0
        }
    }

    @Nested
    @DisplayName("ExtFilter Tests")
    class ExtFilterTests {

        @Test
        @DisplayName("Matches by extension case-insensitively")
        void matchesExtension() {
            FileFinder.FileFilter f = new FileFinder.ExtFilter(".java");
            assertTrue(f.matches(appJava));
            assertFalse(f.matches(readmeMd));
        }

        @Test
        @DisplayName("Case insensitive matching")
        void caseInsensitive() {
            FileFinder.FileFilter f = new FileFinder.ExtFilter(".JAVA");
            assertTrue(f.matches(appJava));
        }

        @Test
        @DisplayName("Extension without dot doesn't match")
        void withoutDot() {
            FileFinder.FileFilter f = new FileFinder.ExtFilter("java");
            assertFalse(f.matches(appJava)); // extension is ".java", not "java"
        }
    }

    @Nested
    @DisplayName("NameContainsFilter Tests")
    class NameContainsFilterTests {

        @Test
        @DisplayName("Matches substring in file name")
        void matchesSubstring() {
            FileFinder.FileFilter f = new FileFinder.NameContainsFilter("app");
            assertTrue(f.matches(appJava));
            assertTrue(f.matches(appTestJava));
            assertTrue(f.matches(appJar));
            assertFalse(f.matches(readmeMd));
        }

        @Test
        @DisplayName("Case insensitive")
        void caseInsensitive() {
            FileFinder.FileFilter f = new FileFinder.NameContainsFilter("APP");
            assertTrue(f.matches(appJava)); // "App.java" contains "app" ignoring case
        }
    }

    @Nested
    @DisplayName("ModifiedAfterFilter Tests")
    class ModifiedAfterFilterTests {

        @Test
        @DisplayName("Matches files modified after threshold")
        void matchesAfter() {
            FileFinder.FileFilter f = new FileFinder.ModifiedAfterFilter(4000);
            assertTrue(f.matches(guideMd));    // 5000 > 4000
            assertFalse(f.matches(appTestJava)); // 4000 > 4000 → false (strictly after)
            assertFalse(f.matches(readmeMd));  // 1000 > 4000 → false
        }
    }

    // ==================== Composite Filter Tests ====================

    @Nested
    @DisplayName("AndFilter Tests")
    class AndFilterTests {

        @Test
        @DisplayName("All filters must match")
        void allMustMatch() {
            // Java files AND size > 200
            FileFinder.FileFilter f = new FileFinder.AndFilter(
                    new FileFinder.ExtFilter(".java"),
                    new FileFinder.SizeFilter(200)
            );
            assertTrue(f.matches(appJava));     // .java & 500 > 200
            assertFalse(f.matches(utilsJava));  // .java & 200 > 200 → false
            assertFalse(f.matches(appJar));     // .jar, not .java
        }

        @Test
        @DisplayName("Empty AndFilter matches everything")
        void emptyMatches() {
            FileFinder.FileFilter f = new FileFinder.AndFilter();
            assertTrue(f.matches(readmeMd));
        }
    }

    @Nested
    @DisplayName("OrFilter Tests")
    class OrFilterTests {

        @Test
        @DisplayName("Any filter can match")
        void anyCanMatch() {
            // .md OR .txt
            FileFinder.FileFilter f = new FileFinder.OrFilter(
                    new FileFinder.ExtFilter(".md"),
                    new FileFinder.ExtFilter(".txt")
            );
            assertTrue(f.matches(readmeMd));
            assertTrue(f.matches(apiTxt));
            assertFalse(f.matches(appJava));
        }

        @Test
        @DisplayName("Empty OrFilter matches nothing")
        void emptyMatchesNothing() {
            FileFinder.FileFilter f = new FileFinder.OrFilter();
            assertFalse(f.matches(readmeMd));
        }
    }

    @Nested
    @DisplayName("NotFilter Tests")
    class NotFilterTests {

        @Test
        @DisplayName("Inverts inner filter")
        void inverts() {
            FileFinder.FileFilter f = new FileFinder.NotFilter(new FileFinder.ExtFilter(".java"));
            assertFalse(f.matches(appJava));
            assertTrue(f.matches(readmeMd));
        }

        @Test
        @DisplayName("Null inner filter throws")
        void nullInner() {
            assertThrows(IllegalArgumentException.class, () -> new FileFinder.NotFilter(null));
        }
    }

    // ==================== Search / Find Tests ====================

    @Nested
    @DisplayName("Find Tests")
    class FindTests {

        @Test
        @DisplayName("Find all .java files")
        void findJavaFiles() {
            List<FileFinder.File> results = FileFinder.find(root, new FileFinder.ExtFilter(".java"));
            assertEquals(3, results.size());
            assertTrue(results.contains(appJava));
            assertTrue(results.contains(utilsJava));
            assertTrue(results.contains(appTestJava));
        }

        @Test
        @DisplayName("Find all .md files")
        void findMdFiles() {
            List<FileFinder.File> results = FileFinder.find(root, new FileFinder.ExtFilter(".md"));
            assertEquals(2, results.size());
            assertTrue(results.contains(readmeMd));
            assertTrue(results.contains(guideMd));
        }

        @Test
        @DisplayName("Find files with name containing 'app' and .java extension")
        void findAppJavaFiles() {
            FileFinder.FileFilter f = new FileFinder.AndFilter(
                    new FileFinder.NameContainsFilter("app"),
                    new FileFinder.ExtFilter(".java")
            );
            List<FileFinder.File> results = FileFinder.find(root, f);
            assertEquals(2, results.size());
            assertTrue(results.contains(appJava));
            assertTrue(results.contains(appTestJava));
        }

        @Test
        @DisplayName("Find all files (match-all filter)")
        void findAllFiles() {
            List<FileFinder.File> results = FileFinder.find(root, file -> true);
            assertEquals(8, results.size());
        }

        @Test
        @DisplayName("Find nothing when no match")
        void findNothing() {
            List<FileFinder.File> results = FileFinder.find(root, new FileFinder.ExtFilter(".py"));
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Find in empty directory")
        void emptyDirectory() {
            FileFinder.Directory empty = new FileFinder.Directory("empty");
            List<FileFinder.File> results = FileFinder.find(empty, file -> true);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Null root throws")
        void nullRoot() {
            assertThrows(IllegalArgumentException.class,
                    () -> FileFinder.find(null, file -> true));
        }

        @Test
        @DisplayName("Null filter throws")
        void nullFilter() {
            assertThrows(IllegalArgumentException.class,
                    () -> FileFinder.find(root, null));
        }
    }

    // ==================== SearchOptions Tests ====================

    @Nested
    @DisplayName("SearchOptions Tests")
    class SearchOptionsTests {

        @Test
        @DisplayName("maxResults limits number of results")
        void maxResults() {
            FileFinder.SearchOptions opts = new FileFinder.SearchOptions(2, 0);
            List<FileFinder.File> results = FileFinder.find(root, file -> true, opts);
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("maxDepth=1 returns only immediate children files")
        void maxDepthOne() {
            // depth 1 = immediate children of root → only readme.md
            FileFinder.SearchOptions opts = new FileFinder.SearchOptions(0, 1);
            List<FileFinder.File> results = FileFinder.find(root, file -> true, opts);
            assertEquals(1, results.size());
            assertTrue(results.contains(readmeMd));
        }

        @Test
        @DisplayName("maxDepth=2 includes files inside direct child dirs")
        void maxDepthTwo() {
            // depth 2 = root children + files in src/, docs/, build/
            // Includes: readme.md, App.java, Utils.java, guide.md, api.txt, app.jar, lib.jar
            // Excludes: AppTest.java (depth 3: root/src/test/AppTest.java)
            FileFinder.SearchOptions opts = new FileFinder.SearchOptions(0, 2);
            List<FileFinder.File> results = FileFinder.find(root, file -> true, opts);
            assertEquals(7, results.size());
            assertFalse(results.contains(appTestJava));
        }

        @Test
        @DisplayName("maxResults and maxDepth combined")
        void maxResultsAndDepth() {
            // depth 2, max 3 results
            FileFinder.SearchOptions opts = new FileFinder.SearchOptions(3, 2);
            List<FileFinder.File> results = FileFinder.find(root, file -> true, opts);
            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("Unlimited options return all matches")
        void unlimited() {
            FileFinder.SearchOptions opts = FileFinder.SearchOptions.unlimited();
            List<FileFinder.File> results = FileFinder.find(root, file -> true, opts);
            assertEquals(8, results.size());
        }
    }

    // ==================== Complex / Scenario Tests ====================

    @Nested
    @DisplayName("Scenario Tests")
    class ScenarioTests {

        @Test
        @DisplayName("Find large .jar files modified after epoch 6000")
        void findLargeRecentJars() {
            FileFinder.FileFilter f = new FileFinder.AndFilter(
                    new FileFinder.ExtFilter(".jar"),
                    new FileFinder.SizeFilter(5000),
                    new FileFinder.ModifiedAfterFilter(6000)
            );
            List<FileFinder.File> results = FileFinder.find(root, f);
            assertEquals(2, results.size());
            assertTrue(results.contains(appJar));
            assertTrue(results.contains(libJar));
        }

        @Test
        @DisplayName("Find non-java and non-jar files (NOT OR)")
        void findNonJavaNonJar() {
            FileFinder.FileFilter f = new FileFinder.NotFilter(
                    new FileFinder.OrFilter(
                            new FileFinder.ExtFilter(".java"),
                            new FileFinder.ExtFilter(".jar")
                    )
            );
            List<FileFinder.File> results = FileFinder.find(root, f);
            // Should include: readme.md, guide.md, api.txt
            assertEquals(3, results.size());
            assertTrue(results.contains(readmeMd));
            assertTrue(results.contains(guideMd));
            assertTrue(results.contains(apiTxt));
        }

        @Test
        @DisplayName("Deeply nested single file found at correct depth")
        void deeplyNested() {
            // Create a chain: d1/d2/d3/d4/deep.txt
            FileFinder.File deep = new FileFinder.File("deep.txt", 1, 1);
            FileFinder.Directory d4 = new FileFinder.Directory("d4").addChild(deep);
            FileFinder.Directory d3 = new FileFinder.Directory("d3").addChild(d4);
            FileFinder.Directory d2 = new FileFinder.Directory("d2").addChild(d3);
            FileFinder.Directory d1 = new FileFinder.Directory("d1").addChild(d2);

            // maxDepth=3 should NOT reach deep.txt (it's at depth 4)
            FileFinder.SearchOptions opts3 = new FileFinder.SearchOptions(0, 3);
            assertTrue(FileFinder.find(d1, file -> true, opts3).isEmpty());

            // maxDepth=4 should reach it
            FileFinder.SearchOptions opts4 = new FileFinder.SearchOptions(0, 4);
            assertEquals(1, FileFinder.find(d1, file -> true, opts4).size());

            // unlimited should also reach it
            assertEquals(1, FileFinder.find(d1, file -> true).size());
        }

        @Test
        @DisplayName("DFS order: files appear in left-to-right tree order")
        void dfsOrder() {
            List<FileFinder.File> results = FileFinder.find(root, file -> true);
            // Expected DFS order:
            // readme.md, App.java, Utils.java, AppTest.java, guide.md, api.txt, app.jar, lib.jar
            assertEquals(readmeMd, results.get(0));
            assertEquals(appJava, results.get(1));
            assertEquals(utilsJava, results.get(2));
            assertEquals(appTestJava, results.get(3));
            assertEquals(guideMd, results.get(4));
            assertEquals(apiTxt, results.get(5));
            assertEquals(appJar, results.get(6));
            assertEquals(libJar, results.get(7));
        }

        @Test
        @DisplayName("Lambda filter works as FileFilter")
        void lambdaFilter() {
            List<FileFinder.File> results = FileFinder.find(root,
                    file -> file.getSizeBytes() >= 100 && file.getSizeBytes() <= 300);
            // 100 (readme), 200 (utils), 300 (appTest), 150 (guide) → 4 files
            assertEquals(4, results.size());
        }

        @Test
        @DisplayName("Directory with only subdirectories, no files")
        void directoriesOnly() {
            FileFinder.Directory inner = new FileFinder.Directory("a");
            FileFinder.Directory outer = new FileFinder.Directory("b").addChild(inner);
            assertTrue(FileFinder.find(outer, file -> true).isEmpty());
        }

        @Test
        @DisplayName("addChild returns this for fluent usage")
        void fluentApi() {
            FileFinder.Directory d = new FileFinder.Directory("x")
                    .addChild(new FileFinder.File("a.txt", 1, 1))
                    .addChild(new FileFinder.File("b.txt", 2, 2));
            assertEquals(2, d.getChildren().size());
        }
    }
}
