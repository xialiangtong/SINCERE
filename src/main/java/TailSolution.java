import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Production-level implementation of the Unix {@code tail} command.
 *
 * <p>MVP: given a file path, prints the last 10 lines to stdout.
 * <p>Flag {@code -n <N>}: prints the last N lines instead.
 *
 * <h3>OOP Design</h3>
 * <ul>
 *   <li>{@link LineSource} – abstraction over where lines come from (file, string, etc.)</li>
 *   <li>{@link OutputTarget} – abstraction over where output goes (console, buffer, etc.)</li>
 *   <li>{@link TailExtractor} – core algorithm: extracts the last N lines using a circular buffer</li>
 *   <li>{@link CliArgs} – immutable value-object for parsed CLI arguments</li>
 *   <li>{@link ArgParser} – stateless parser that converts raw args into {@link CliArgs}</li>
 *   <li>{@link TailApp} – orchestrator that wires everything together</li>
 * </ul>
 */
public class TailSolution {

    // ======================== Default Constants ========================

    public static final int DEFAULT_LINE_COUNT = 10;

    // ======================== Line Source ========================

    /**
     * Abstraction for a source of text lines.
     * Allows the core algorithm to be tested without touching the file system.
     */
    public interface LineSource {
        /**
         * Opens a {@link BufferedReader} over the underlying content.
         * Callers are responsible for closing the reader.
         */
        BufferedReader open() throws IOException;
    }

    /**
     * Reads lines from a file on disk.
     */
    public static class FileLineSource implements LineSource {
        private final Path path;

        public FileLineSource(Path path) {
            this.path = Objects.requireNonNull(path, "path must not be null");
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("File does not exist: " + path);
            }
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Not a regular file: " + path);
            }
        }

        public Path getPath() {
            return path;
        }

        @Override
        public BufferedReader open() throws IOException {
            return Files.newBufferedReader(path);
        }
    }

    /**
     * Reads lines from an in-memory string. Primarily intended for testing.
     */
    public static class StringLineSource implements LineSource {
        private final String content;

        public StringLineSource(String content) {
            this.content = Objects.requireNonNull(content, "content must not be null");
        }

        @Override
        public BufferedReader open() {
            return new BufferedReader(new StringReader(content));
        }
    }

    // ======================== Output Target ========================

    /**
     * Abstraction for an output destination.
     */
    public interface OutputTarget {
        void writeLine(String line);
    }

    /**
     * Writes to {@code System.out}.
     */
    public static class ConsoleOutput implements OutputTarget {
        @Override
        public void writeLine(String line) {
            System.out.println(line);
        }
    }

    /**
     * Captures output in-memory. Useful for testing.
     */
    public static class BufferOutput implements OutputTarget {
        private final List<String> lines = new ArrayList<>();

        @Override
        public void writeLine(String line) {
            lines.add(line);
        }

        public List<String> getLines() {
            return Collections.unmodifiableList(lines);
        }
    }

    // ======================== Tail Extractor ========================

    /**
     * Core algorithm: reads from a {@link LineSource} and returns the last N lines
     * using a circular-buffer approach so memory usage is O(N), not O(total-lines).
     */
    public static class TailExtractor {

        private final int lineCount;

        public TailExtractor(int lineCount) {
            if (lineCount < 0) {
                throw new IllegalArgumentException("lineCount must be >= 0, got: " + lineCount);
            }
            this.lineCount = lineCount;
        }

        public int getLineCount() {
            return lineCount;
        }

        /**
         * Extracts the last {@code lineCount} lines from the given source.
         *
         * @param source the line source to read from
         * @return an unmodifiable list of up to {@code lineCount} lines (in order)
         * @throws IOException if reading fails
         */
        public List<String> extract(LineSource source) throws IOException {
            if (lineCount == 0) {
                // Consume nothing – just return empty.
                return Collections.emptyList();
            }

            String[] ring = new String[lineCount];
            int writeIndex = 0;
            int totalLines = 0;

            try (BufferedReader reader = source.open()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ring[writeIndex] = line;
                    writeIndex = (writeIndex + 1) % lineCount;
                    totalLines++;
                }
            }

            // Determine how many lines we actually collected.
            int count = Math.min(totalLines, lineCount);
            List<String> result = new ArrayList<>(count);

            // The oldest line in the ring sits at writeIndex if the ring is full,
            // otherwise at index 0.
            int startIndex = (totalLines >= lineCount) ? writeIndex : 0;
            for (int i = 0; i < count; i++) {
                result.add(ring[(startIndex + i) % lineCount]);
            }

            return Collections.unmodifiableList(result);
        }
    }

    // ======================== CLI Argument Parsing ========================

    /**
     * Immutable value-object holding parsed CLI arguments.
     */
    public static class CliArgs {
        private final String filePath;
        private final int lineCount;

        public CliArgs(String filePath, int lineCount) {
            this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
            if (lineCount < 0) {
                throw new IllegalArgumentException("lineCount must be >= 0");
            }
            this.lineCount = lineCount;
        }

        public String getFilePath() {
            return filePath;
        }

        public int getLineCount() {
            return lineCount;
        }

        @Override
        public String toString() {
            return "CliArgs{file='" + filePath + "', lines=" + lineCount + "}";
        }
    }

    /**
     * Stateless parser that converts raw CLI args into a {@link CliArgs} instance.
     *
     * <p>Usage: {@code tail <file>} or {@code tail -n <N> <file>}
     */
    public static class ArgParser {

        /**
         * Parses the supplied arguments.
         *
         * @param args raw command-line arguments
         * @return a {@link CliArgs} instance
         * @throws IllegalArgumentException if arguments are invalid
         */
        public CliArgs parse(String[] args) {
            if (args == null || args.length == 0) {
                throw new IllegalArgumentException("Usage: tail [-n <N>] <file>");
            }

            int lineCount = DEFAULT_LINE_COUNT;
            String filePath = null;

            int i = 0;
            while (i < args.length) {
                if ("-n".equals(args[i])) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("-n requires a numeric argument");
                    }
                    try {
                        lineCount = Integer.parseInt(args[i + 1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid number for -n: '" + args[i + 1] + "'");
                    }
                    if (lineCount < 0) {
                        throw new IllegalArgumentException(
                                "-n value must be >= 0, got: " + lineCount);
                    }
                    i += 2;
                } else if (args[i].startsWith("-")) {
                    throw new IllegalArgumentException("Unknown flag: " + args[i]);
                } else {
                    if (filePath != null) {
                        throw new IllegalArgumentException(
                                "Multiple file arguments are not supported");
                    }
                    filePath = args[i];
                    i++;
                }
            }

            if (filePath == null) {
                throw new IllegalArgumentException("No file argument provided");
            }

            return new CliArgs(filePath, lineCount);
        }
    }

    // ======================== TailApp Orchestrator ========================

    /**
     * High-level orchestrator that wires argument parsing, extraction, and output.
     */
    public static class TailApp {
        private final ArgParser argParser;
        private final OutputTarget output;

        public TailApp(OutputTarget output) {
            this(new ArgParser(), output);
        }

        public TailApp(ArgParser argParser, OutputTarget output) {
            this.argParser = Objects.requireNonNull(argParser);
            this.output = Objects.requireNonNull(output);
        }

        /**
         * Runs the tail command with the given raw arguments.
         */
        public void run(String[] args) throws IOException {
            CliArgs cliArgs = argParser.parse(args);
            LineSource source = new FileLineSource(Path.of(cliArgs.getFilePath()));
            TailExtractor extractor = new TailExtractor(cliArgs.getLineCount());
            List<String> lines = extractor.extract(source);
            lines.forEach(output::writeLine);
        }

        /**
         * Runs the tail extraction against an arbitrary {@link LineSource}
         * (useful for testing without real files).
         */
        public void run(LineSource source, int lineCount) throws IOException {
            TailExtractor extractor = new TailExtractor(lineCount);
            List<String> lines = extractor.extract(source);
            lines.forEach(output::writeLine);
        }
    }

    // ======================== Main Entry Point ========================

    /**
     * CLI entry point.
     *
     * <pre>
     *   java TailSolution myfile.txt          # last 10 lines
     *   java TailSolution -n 5 myfile.txt     # last 5 lines
     * </pre>
     */
    public static void main(String[] args) {
        TailApp app = new TailApp(new ConsoleOutput());
        try {
            app.run(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            System.exit(2);
        }
    }
}
