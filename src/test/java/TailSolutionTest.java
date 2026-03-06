import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@DisplayName("TailSolution Tests")
public class TailSolutionTest {

    // ==================== Helpers ====================

    /**
     * Creates a StringLineSource from numbered lines: "Line 1", "Line 2", …
     */
    private TailSolution.StringLineSource numberedSource(int totalLines) {
        String content = IntStream.rangeClosed(1, totalLines)
                .mapToObj(i -> "Line " + i)
                .collect(Collectors.joining("\n"));
        return new TailSolution.StringLineSource(content);
    }

    /**
     * Returns expected lines: "Line (totalLines - n + 1)" … "Line totalLines"
     */
    private List<String> expectedLastN(int totalLines, int n) {
        int start = Math.max(1, totalLines - n + 1);
        return IntStream.rangeClosed(start, totalLines)
                .mapToObj(i -> "Line " + i)
                .collect(Collectors.toList());
    }

    // ==================== StringLineSource ====================

    @Nested
    @DisplayName("StringLineSource Tests")
    class StringLineSourceTests {

        @Test
        @DisplayName("reads all lines from an in-memory string")
        void readsAllLines() throws IOException {
            var source = new TailSolution.StringLineSource("a\nb\nc");
            var reader = source.open();
            assertEquals("a", reader.readLine());
            assertEquals("b", reader.readLine());
            assertEquals("c", reader.readLine());
            assertNull(reader.readLine());
            reader.close();
        }

        @Test
        @DisplayName("handles empty string")
        void emptyString() throws IOException {
            var source = new TailSolution.StringLineSource("");
            var reader = source.open();
            assertEquals("", reader.readLine());
            assertNull(reader.readLine());
            reader.close();
        }

        @Test
        @DisplayName("rejects null content")
        void nullContent() {
            assertThrows(NullPointerException.class,
                    () -> new TailSolution.StringLineSource(null));
        }
    }

    // ==================== FileLineSource ====================

    @Nested
    @DisplayName("FileLineSource Tests")
    class FileLineSourceTests {

        @Test
        @DisplayName("reads lines from a real temp file")
        void readsFromFile() throws IOException {
            Path tmp = Files.createTempFile("tail-test-", ".txt");
            try {
                Files.writeString(tmp, "alpha\nbeta\ngamma");
                var source = new TailSolution.FileLineSource(tmp);
                var reader = source.open();
                assertEquals("alpha", reader.readLine());
                assertEquals("beta", reader.readLine());
                assertEquals("gamma", reader.readLine());
                assertNull(reader.readLine());
                reader.close();
            } finally {
                Files.deleteIfExists(tmp);
            }
        }

        @Test
        @DisplayName("rejects non-existent path")
        void nonExistentFile() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TailSolution.FileLineSource(Path.of("/no/such/file.txt")));
        }

        @Test
        @DisplayName("rejects null path")
        void nullPath() {
            assertThrows(NullPointerException.class,
                    () -> new TailSolution.FileLineSource(null));
        }
    }

    // ==================== TailExtractor ====================

    @Nested
    @DisplayName("TailExtractor Tests")
    class TailExtractorTests {

        @Test
        @DisplayName("returns last 10 lines (default) from a 20 line source")
        void defaultLast10() throws IOException {
            var extractor = new TailSolution.TailExtractor(10);
            List<String> result = extractor.extract(numberedSource(20));
            assertEquals(expectedLastN(20, 10), result);
        }

        @Test
        @DisplayName("returns last 5 lines with -n 5")
        void last5() throws IOException {
            var extractor = new TailSolution.TailExtractor(5);
            List<String> result = extractor.extract(numberedSource(20));
            assertEquals(expectedLastN(20, 5), result);
        }

        @Test
        @DisplayName("returns all lines when N > total lines")
        void nLargerThanTotal() throws IOException {
            var extractor = new TailSolution.TailExtractor(50);
            List<String> result = extractor.extract(numberedSource(5));
            assertEquals(expectedLastN(5, 5), result);
            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("returns empty list when N is 0")
        void zeroLines() throws IOException {
            var extractor = new TailSolution.TailExtractor(0);
            List<String> result = extractor.extract(numberedSource(10));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("handles single-line source")
        void singleLine() throws IOException {
            var extractor = new TailSolution.TailExtractor(10);
            var source = new TailSolution.StringLineSource("only line");
            List<String> result = extractor.extract(source);
            assertEquals(List.of("only line"), result);
        }

        @Test
        @DisplayName("exact match: N equals total lines")
        void exactMatch() throws IOException {
            var extractor = new TailSolution.TailExtractor(7);
            List<String> result = extractor.extract(numberedSource(7));
            assertEquals(expectedLastN(7, 7), result);
        }

        @Test
        @DisplayName("returns last 1 line")
        void lastOneLine() throws IOException {
            var extractor = new TailSolution.TailExtractor(1);
            List<String> result = extractor.extract(numberedSource(100));
            assertEquals(List.of("Line 100"), result);
        }

        @Test
        @DisplayName("preserves line ordering in circular buffer")
        void orderingPreserved() throws IOException {
            var extractor = new TailSolution.TailExtractor(3);
            var source = new TailSolution.StringLineSource("a\nb\nc\nd\ne");
            List<String> result = extractor.extract(source);
            assertEquals(List.of("c", "d", "e"), result);
        }

        @Test
        @DisplayName("result list is unmodifiable")
        void resultIsUnmodifiable() throws IOException {
            var extractor = new TailSolution.TailExtractor(5);
            List<String> result = extractor.extract(numberedSource(10));
            assertThrows(UnsupportedOperationException.class,
                    () -> result.add("should fail"));
        }

        @Test
        @DisplayName("rejects negative lineCount")
        void negativeLineCount() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TailSolution.TailExtractor(-1));
        }

        @Test
        @DisplayName("handles large file efficiently with circular buffer")
        void largeFile() throws IOException {
            int totalLines = 100_000;
            var extractor = new TailSolution.TailExtractor(10);
            List<String> result = extractor.extract(numberedSource(totalLines));
            assertEquals(10, result.size());
            assertEquals("Line " + (totalLines - 9), result.get(0));
            assertEquals("Line " + totalLines, result.get(9));
        }
    }

    // ==================== ArgParser ====================

    @Nested
    @DisplayName("ArgParser Tests")
    class ArgParserTests {

        private final TailSolution.ArgParser parser = new TailSolution.ArgParser();

        @Test
        @DisplayName("parses file-only argument with default N=10")
        void fileOnly() {
            var args = parser.parse(new String[]{"data.txt"});
            assertEquals("data.txt", args.getFilePath());
            assertEquals(10, args.getLineCount());
        }

        @Test
        @DisplayName("parses -n flag before file")
        void nFlagBeforeFile() {
            var args = parser.parse(new String[]{"-n", "25", "server.log"});
            assertEquals("server.log", args.getFilePath());
            assertEquals(25, args.getLineCount());
        }

        @Test
        @DisplayName("parses -n flag after file")
        void nFlagAfterFile() {
            var args = parser.parse(new String[]{"server.log", "-n", "3"});
            assertEquals("server.log", args.getFilePath());
            assertEquals(3, args.getLineCount());
        }

        @Test
        @DisplayName("-n 0 is valid")
        void nZero() {
            var args = parser.parse(new String[]{"-n", "0", "file.txt"});
            assertEquals(0, args.getLineCount());
        }

        @Test
        @DisplayName("rejects empty args")
        void emptyArgs() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parse(new String[]{}));
        }

        @Test
        @DisplayName("rejects null args")
        void nullArgs() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parse(null));
        }

        @Test
        @DisplayName("rejects -n without number")
        void nWithoutNumber() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parse(new String[]{"-n"}));
        }

        @Test
        @DisplayName("rejects -n with non-numeric value")
        void nNonNumeric() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parse(new String[]{"-n", "abc", "file.txt"}));
        }

        @Test
        @DisplayName("rejects negative -n value")
        void nNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parse(new String[]{"-n", "-5", "file.txt"}));
        }

        @Test
        @DisplayName("rejects unknown flags")
        void unknownFlag() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parse(new String[]{"-x", "file.txt"}));
        }

        @Test
        @DisplayName("rejects multiple file arguments")
        void multipleFiles() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parse(new String[]{"file1.txt", "file2.txt"}));
        }

        @Test
        @DisplayName("rejects args with no file provided")
        void noFile() {
            assertThrows(IllegalArgumentException.class,
                    () -> parser.parse(new String[]{"-n", "5"}));
        }
    }

    // ==================== CliArgs ====================

    @Nested
    @DisplayName("CliArgs Tests")
    class CliArgsTests {

        @Test
        @DisplayName("stores file path and line count")
        void basicConstruction() {
            var args = new TailSolution.CliArgs("log.txt", 15);
            assertEquals("log.txt", args.getFilePath());
            assertEquals(15, args.getLineCount());
        }

        @Test
        @DisplayName("rejects null file path")
        void nullFilePath() {
            assertThrows(NullPointerException.class,
                    () -> new TailSolution.CliArgs(null, 10));
        }

        @Test
        @DisplayName("rejects negative line count")
        void negativeLineCount() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TailSolution.CliArgs("file.txt", -1));
        }

        @Test
        @DisplayName("toString includes file and count")
        void toStringFormat() {
            var args = new TailSolution.CliArgs("app.log", 20);
            String s = args.toString();
            assertTrue(s.contains("app.log"));
            assertTrue(s.contains("20"));
        }
    }

    // ==================== BufferOutput ====================

    @Nested
    @DisplayName("BufferOutput Tests")
    class BufferOutputTests {

        @Test
        @DisplayName("captures written lines in order")
        void capturesLines() {
            var buf = new TailSolution.BufferOutput();
            buf.writeLine("first");
            buf.writeLine("second");
            assertEquals(List.of("first", "second"), buf.getLines());
        }

        @Test
        @DisplayName("returned list is unmodifiable")
        void unmodifiable() {
            var buf = new TailSolution.BufferOutput();
            buf.writeLine("x");
            assertThrows(UnsupportedOperationException.class,
                    () -> buf.getLines().add("y"));
        }
    }

    // ==================== TailApp Integration ====================

    @Nested
    @DisplayName("TailApp Integration Tests")
    class TailAppIntegrationTests {

        @Test
        @DisplayName("end-to-end: extracts last 3 lines from a string source")
        void endToEndStringSource() throws IOException {
            var buf = new TailSolution.BufferOutput();
            var app = new TailSolution.TailApp(buf);
            var source = numberedSource(20);

            app.run(source, 3);

            assertEquals(List.of("Line 18", "Line 19", "Line 20"), buf.getLines());
        }

        @Test
        @DisplayName("end-to-end: default 10 lines from string source")
        void endToEndDefault10() throws IOException {
            var buf = new TailSolution.BufferOutput();
            var app = new TailSolution.TailApp(buf);
            var source = numberedSource(15);

            app.run(source, TailSolution.DEFAULT_LINE_COUNT);

            assertEquals(expectedLastN(15, 10), buf.getLines());
        }

        @Test
        @DisplayName("end-to-end: reads from a temp file via CLI args")
        void endToEndWithFile() throws IOException {
            Path tmp = Files.createTempFile("tail-integration-", ".txt");
            try {
                String content = IntStream.rangeClosed(1, 12)
                        .mapToObj(i -> "Row " + i)
                        .collect(Collectors.joining("\n"));
                Files.writeString(tmp, content);

                var buf = new TailSolution.BufferOutput();
                var app = new TailSolution.TailApp(buf);
                app.run(new String[]{"-n", "4", tmp.toString()});

                assertEquals(List.of("Row 9", "Row 10", "Row 11", "Row 12"), buf.getLines());
            } finally {
                Files.deleteIfExists(tmp);
            }
        }

        @Test
        @DisplayName("end-to-end: file with default 10 lines via CLI args")
        void endToEndFileDefault() throws IOException {
            Path tmp = Files.createTempFile("tail-default-", ".txt");
            try {
                String content = IntStream.rangeClosed(1, 25)
                        .mapToObj(i -> "Data " + i)
                        .collect(Collectors.joining("\n"));
                Files.writeString(tmp, content);

                var buf = new TailSolution.BufferOutput();
                var app = new TailSolution.TailApp(buf);
                app.run(new String[]{tmp.toString()});

                assertEquals(10, buf.getLines().size());
                assertEquals("Data 16", buf.getLines().get(0));
                assertEquals("Data 25", buf.getLines().get(9));
            } finally {
                Files.deleteIfExists(tmp);
            }
        }

        @Test
        @DisplayName("end-to-end: -n 0 produces no output")
        void endToEndZeroLines() throws IOException {
            var buf = new TailSolution.BufferOutput();
            var app = new TailSolution.TailApp(buf);
            app.run(numberedSource(10), 0);
            assertTrue(buf.getLines().isEmpty());
        }
    }
}
