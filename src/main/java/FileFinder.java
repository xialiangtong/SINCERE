import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileFinder {

    // ======================== File System Nodes ========================

    /**
     * Base class for file system nodes.
     */
    public static abstract class FsNode {
        private final String name;

        protected FsNode(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name must not be null or empty");
            }
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract boolean isDirectory();
    }

    /**
     * A file node with metadata.
     */
    public static class File extends FsNode {
        private final long sizeBytes;
        private final long lastModifiedEpochMs;
        private final String extension;

        public File(String name, long sizeBytes, long lastModifiedEpochMs) {
            super(name);
            this.sizeBytes = sizeBytes;
            this.lastModifiedEpochMs = lastModifiedEpochMs;
            int dot = name.lastIndexOf('.');
            this.extension = (dot >= 0) ? name.substring(dot) : "";
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public long getLastModifiedEpochMs() {
            return lastModifiedEpochMs;
        }

        public String getExtension() {
            return extension;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public String toString() {
            return "File{" + getName() + ", " + sizeBytes + "B}";
        }
    }

    /**
     * A directory node containing children.
     */
    public static class Directory extends FsNode {
        private final List<FsNode> children;

        public Directory(String name) {
            super(name);
            this.children = new ArrayList<>();
        }

        public Directory addChild(FsNode child) {
            if (child == null) {
                throw new IllegalArgumentException("Child must not be null");
            }
            children.add(child);
            return this;
        }

        public List<FsNode> getChildren() {
            return Collections.unmodifiableList(children);
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public String toString() {
            return "Directory{" + getName() + ", " + children.size() + " children}";
        }
    }

    // ======================== Filter Interface ========================

    /**
     * Strategy interface for filtering files.
     */
    public interface FileFilter {
        boolean matches(File file);
    }

    // ======================== Leaf Filters ========================

    /**
     * Matches files larger than minBytes.
     */
    public static class SizeFilter implements FileFilter {
        private final long minBytes;

        public SizeFilter(long minBytes) {
            this.minBytes = minBytes;
        }

        @Override
        public boolean matches(File file) {
            return file.getSizeBytes() > minBytes;
        }
    }

    /**
     * Matches files with the given extension (case-insensitive).
     */
    public static class ExtFilter implements FileFilter {
        private final String extension;

        public ExtFilter(String extension) {
            if (extension == null) throw new IllegalArgumentException("Extension must not be null");
            this.extension = extension.toLowerCase();
        }

        @Override
        public boolean matches(File file) {
            return file.getExtension().toLowerCase().equals(extension);
        }
    }

    /**
     * Matches files whose name contains the given substring (case-insensitive).
     */
    public static class NameContainsFilter implements FileFilter {
        private final String substring;

        public NameContainsFilter(String substring) {
            if (substring == null) throw new IllegalArgumentException("Substring must not be null");
            this.substring = substring.toLowerCase();
        }

        @Override
        public boolean matches(File file) {
            return file.getName().toLowerCase().contains(substring);
        }
    }

    /**
     * Matches files modified after the given epoch millis.
     */
    public static class ModifiedAfterFilter implements FileFilter {
        private final long afterEpochMs;

        public ModifiedAfterFilter(long afterEpochMs) {
            this.afterEpochMs = afterEpochMs;
        }

        @Override
        public boolean matches(File file) {
            return file.getLastModifiedEpochMs() > afterEpochMs;
        }
    }

    // ======================== Composite Filters ========================

    /**
     * Matches if ALL inner filters match (logical AND).
     */
    public static class AndFilter implements FileFilter {
        private final List<FileFilter> filters;

        public AndFilter(FileFilter... filters) {
            this.filters = List.of(filters);
        }

        @Override
        public boolean matches(File file) {
            for (FileFilter f : filters) {
                if (!f.matches(file)) return false;
            }
            return true;
        }
    }

    /**
     * Matches if ANY inner filter matches (logical OR).
     */
    public static class OrFilter implements FileFilter {
        private final List<FileFilter> filters;

        public OrFilter(FileFilter... filters) {
            this.filters = List.of(filters);
        }

        @Override
        public boolean matches(File file) {
            for (FileFilter f : filters) {
                if (f.matches(file)) return true;
            }
            return false;
        }
    }

    /**
     * Matches if the inner filter does NOT match (logical NOT).
     */
    public static class NotFilter implements FileFilter {
        private final FileFilter inner;

        public NotFilter(FileFilter inner) {
            if (inner == null) throw new IllegalArgumentException("Inner filter must not be null");
            this.inner = inner;
        }

        @Override
        public boolean matches(File file) {
            return !inner.matches(file);
        }
    }

    // ======================== Search Options ========================

    /**
     * Optional parameters to control search behavior.
     */
    public static class SearchOptions {
        private final int maxResults;  // 0 = unlimited
        private final int maxDepth;    // 0 = unlimited

        public SearchOptions(int maxResults, int maxDepth) {
            this.maxResults = maxResults;
            this.maxDepth = maxDepth;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public static SearchOptions unlimited() {
            return new SearchOptions(0, 0);
        }
    }

    // ======================== Find (core traversal) ========================

    /**
     * Find all files under root matching the filter.
     */
    public static List<File> find(Directory root, FileFilter filter) {
        return find(root, filter, SearchOptions.unlimited());
    }

    /**
     * Find files under root matching the filter, respecting search options.
     */
    public static List<File> find(Directory root, FileFilter filter, SearchOptions options) {
        if (root == null) throw new IllegalArgumentException("Root must not be null");
        if (filter == null) throw new IllegalArgumentException("Filter must not be null");
        if (options == null) options = SearchOptions.unlimited();

        List<File> results = new ArrayList<>();
        dfs(root, filter, options, results, 1);
        return results;
    }

    private static void dfs(Directory dir, FileFilter filter, SearchOptions options,
                            List<File> results, int currentDepth) {
        // Check maxDepth: stop recursion if exceeded
        if (options.getMaxDepth() > 0 && currentDepth > options.getMaxDepth()) {
            return;
        }

        for (FsNode child : dir.getChildren()) {
            // Early exit if maxResults reached
            if (options.getMaxResults() > 0 && results.size() >= options.getMaxResults()) {
                return;
            }

            if (child.isDirectory()) {
                dfs((Directory) child, filter, options, results, currentDepth + 1);
            } else {
                File file = (File) child;
                if (filter.matches(file)) {
                    results.add(file);
                }
            }
        }
    }
}
