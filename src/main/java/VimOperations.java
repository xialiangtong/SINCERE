import java.util.ArrayDeque;
import java.util.Deque;

public class VimOperations {

    private int cursor;
    private final StringBuilder content;
    private final Deque<Command> history = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    // ======================== Command Interface ========================

    interface Command {
        void execute();
        void undo();
    }

    // ======================== Command Implementations ========================

    class DeleteAtCommand implements Command {
        final int position;
        final char deleted;
        final int prevCursor;

        DeleteAtCommand(int position, char deleted, int prevCursor) {
            this.position = position;
            this.deleted = deleted;
            this.prevCursor = prevCursor;
        }

        @Override
        public void execute() {
            content.deleteCharAt(position);
            cursor = position;
        }

        @Override
        public void undo() {
            content.insert(position, deleted);
            cursor = prevCursor;
        }
    }

    class DeleteRangeCommand implements Command {
        final int start;
        final String deleted;
        final int prevCursor;

        DeleteRangeCommand(int start, String deleted, int prevCursor) {
            this.start = start;
            this.deleted = deleted;
            this.prevCursor = prevCursor;
        }

        @Override
        public void execute() {
            int end = start + deleted.length();
            content.delete(start, end);
            if (cursor < start) {
                // stays
            } else if (cursor < end) {
                cursor = start;
            } else {
                cursor -= deleted.length();
            }
        }

        @Override
        public void undo() {
            content.insert(start, deleted);
            cursor = prevCursor;
        }
    }

    class InsertAtCommand implements Command {
        final int position;
        final char inserted;
        final int prevCursor;

        InsertAtCommand(int position, char inserted, int prevCursor) {
            this.position = position;
            this.inserted = inserted;
            this.prevCursor = prevCursor;
        }

        @Override
        public void execute() {
            content.insert(position, inserted);
            cursor = position + 1;
        }

        @Override
        public void undo() {
            content.deleteCharAt(position);
            cursor = prevCursor;
        }
    }

    class InsertRangeCommand implements Command {
        final int start;
        final String inserted;
        final int prevCursor;

        InsertRangeCommand(int start, String inserted, int prevCursor) {
            this.start = start;
            this.inserted = inserted;
            this.prevCursor = prevCursor;
        }

        @Override
        public void execute() {
            content.insert(start, inserted);
            if (cursor >= start) {
                cursor += inserted.length();
            }
        }

        @Override
        public void undo() {
            content.delete(start, start + inserted.length());
            cursor = prevCursor;
        }
    }

    class ReplaceAtCommand implements Command {
        final int position;
        final char replace;
        final char replaced;

        ReplaceAtCommand(int position, char replace, char replaced) {
            this.position = position;
            this.replace = replace;
            this.replaced = replaced;
        }

        @Override
        public void execute() {
            content.setCharAt(position, replace);
        }

        @Override
        public void undo() {
            content.setCharAt(position, replaced);
        }
    }

    class ReplaceRangeCommand implements Command {
        final int start;
        final int end;
        final String replace;
        final String replaced;
        final int prevCursor;

        ReplaceRangeCommand(int start, int end, String replace, String replaced, int prevCursor) {
            this.start = start;
            this.end = end;
            this.replace = replace;
            this.replaced = replaced;
            this.prevCursor = prevCursor;
        }

        @Override
        public void execute() {
            content.replace(start, end, replace);
            int delta = replace.length() - (end - start);
            if (cursor < start) {
                // stays
            } else if (cursor < end) {
                cursor = start;
            } else {
                cursor += delta;
            }
        }

        @Override
        public void undo() {
            content.replace(start, start + replace.length(), replaced);
            cursor = prevCursor;
        }
    }

    // ======================== Constructors ========================

    public VimOperations() {
        this.content = new StringBuilder();
        this.cursor = 0;
    }

    public VimOperations(String initialContent) {
        this.content = new StringBuilder(initialContent != null ? initialContent : "");
        this.cursor = 0;
    }

    // ======================== Getters ========================

    public int getCursor() {
        return cursor;
    }

    public String getContent() {
        return content.toString();
    }

    // ======================== Cursor Movement ========================

    /**
     * Move cursor forward by 1. Clamped to content.length().
     */
    public int moveCursorForward() {
        cursor = Math.min(cursor + 1, content.length());
        return cursor;
    }

    /**
     * Move cursor backward by 1. Clamped to 0.
     */
    public int moveCursorBackward() {
        cursor = Math.max(cursor - 1, 0);
        return cursor;
    }

    /**
     * Move cursor to position 0.
     */
    public int moveCursorToHead() {
        cursor = 0;
        return cursor;
    }

    /**
     * Move cursor to content.length().
     */
    public int moveCursorToTail() {
        cursor = content.length();
        return cursor;
    }

    // ======================== Delete ========================

    /**
     * Delete the character before the cursor (Backspace).
     * Cursor moves back by 1. No-op if cursor == 0.
     * @return the deleted char, or '\0' if nothing deleted
     */
    public char deleteBackward() {
        if (cursor == 0) return '\0';
        char deleted = content.charAt(cursor - 1);
        Command cmd = new DeleteAtCommand(cursor - 1, deleted, cursor);
        executeAndRecord(cmd);
        return deleted;
    }

    /**
     * Delete the character at the cursor (Del key).
     * Cursor stays. No-op if cursor == content.length().
     * @return the deleted char, or '\0' if nothing deleted
     */
    public char deleteCurrent() {
        if (cursor == content.length()) return '\0';
        char deleted = content.charAt(cursor);
        Command cmd = new DeleteAtCommand(cursor, deleted, cursor);
        executeAndRecord(cmd);
        return deleted;
    }

    /**
     * Delete characters in range [start, end).
     * Cursor adjusts if needed.
     * @return the deleted substring
     */
    public String deleteRange(int start, int end) {
        if (start < 0 || end > content.length() || start > end) {
            throw new IllegalArgumentException("Invalid range: [" + start + ", " + end + ")");
        }
        if (start == end) return "";
        String deleted = content.substring(start, end);
        Command cmd = new DeleteRangeCommand(start, deleted, cursor);
        executeAndRecord(cmd);
        return deleted;
    }

    // ======================== Insert ========================

    /**
     * Insert a character at the current cursor position.
     * Cursor moves forward by 1.
     * @return the inserted char
     */
    public char insertCurrent(char c) {
        Command cmd = new InsertAtCommand(cursor, c, cursor);
        executeAndRecord(cmd);
        return c;
    }

    /**
     * Insert a string at the given position.
     * Cursor adjusts if needed.
     * @return the inserted string
     */
    public String insertRange(int pos, String newContent) {
        if (newContent == null) throw new IllegalArgumentException("Content cannot be null");
        if (pos < 0 || pos > content.length()) {
            throw new IllegalArgumentException("Invalid position: " + pos);
        }
        if (newContent.isEmpty()) return "";
        Command cmd = new InsertRangeCommand(pos, newContent, cursor);
        executeAndRecord(cmd);
        return newContent;
    }

    // ======================== Replace ========================

    /**
     * Replace the character at the cursor with c.
     * Cursor stays. No-op if cursor == content.length().
     * @return the old char, or '\0' if nothing replaced
     */
    public char replaceCurrent(char c) {
        if (cursor == content.length()) return '\0';
        char old = content.charAt(cursor);
        Command cmd = new ReplaceAtCommand(cursor, c, old);
        executeAndRecord(cmd);
        return old;
    }

    /**
     * Replace characters in [start, end) with replacement string.
     * Cursor adjusts if needed.
     * @return the old substring that was replaced
     */
    public String replaceRange(int start, int end, String replacement) {
        if (replacement == null) throw new IllegalArgumentException("Replacement cannot be null");
        if (start < 0 || end > content.length() || start > end) {
            throw new IllegalArgumentException("Invalid range: [" + start + ", " + end + ")");
        }
        String old = content.substring(start, end);
        if (start == end && replacement.isEmpty()) return old;
        Command cmd = new ReplaceRangeCommand(start, end, replacement, old, cursor);
        executeAndRecord(cmd);
        return old;
    }

    // ======================== Helpers ========================

    // ======================== Undo / Redo ========================

    /**
     * Undo the last command. No-op if history is empty.
     */
    public void undo() {
        if (history.isEmpty()) return;
        Command cmd = history.pop();
        cmd.undo();
        redoStack.push(cmd);
    }

    /**
     * Redo the last undone command. No-op if redoStack is empty.
     */
    public void redo() {
        if (redoStack.isEmpty()) return;
        Command cmd = redoStack.pop();
        cmd.execute();
        history.push(cmd);
    }

    // ======================== Helpers ========================

    private void executeAndRecord(Command cmd) {
        cmd.execute();
        history.push(cmd);
        redoStack.clear();
    }
}
