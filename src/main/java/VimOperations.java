import java.util.ArrayDeque;
import java.util.Deque;

public class VimOperations {

    private int cursor;
    private StringBuilder content;
    private final Deque<Command> undos;
    private final Deque<Command> redos;

    // ======================== Constructors ========================

    public VimOperations() {
        this("");
    }

    public VimOperations(String initialContent) {
        this.content = new StringBuilder(initialContent != null ? initialContent : "");
        this.cursor = this.content.length();
        this.undos = new ArrayDeque<>();
        this.redos = new ArrayDeque<>();
    }

    // ======================== Query ========================

    public String getContent() {
        return content.toString();
    }

    public int getCursor() {
        return cursor;
    }

    // ======================== Cursor Movement ========================

    public void moveForward() {
        if (cursor < content.length()) cursor++;
    }

    public void moveBackward() {
        if (cursor > 0) cursor--;
    }

    public void moveToHead() {
        cursor = 0;
    }

    public void moveToTail() {
        cursor = content.length();
    }

    // ======================== Delete ========================

    public char deleteBackward() {
        if (cursor <= 0) return '\0';
        char deleted = content.charAt(cursor - 1);
        Command cmd = new DeleteAtCommand(cursor - 1, deleted);
        executeCommand(cmd);
        return deleted;
    }

    public char deleteCurrent() {
        if (cursor >= content.length()) return '\0';
        char deleted = content.charAt(cursor);
        Command cmd = new DeleteAtCommand(cursor, deleted);
        executeCommand(cmd);
        return deleted;
    }

    public String deleteRange(int start, int end) {
        if (start > end)
            throw new IllegalArgumentException("start > end: " + start + " > " + end);
        if (start < 0 || end > content.length())
            throw new IndexOutOfBoundsException(
                    "Range [" + start + ", " + end + ") out of bounds for length " + content.length());
        if (start == end) return "";
        String deleted = content.substring(start, end);
        Command cmd = new DeleteRangeCommand(start, deleted, cursor);
        executeCommand(cmd);
        return deleted;
    }

    public char deletePosition(int pos) {
        if (pos < 0 || pos >= content.length())
            throw new IndexOutOfBoundsException(
                    "Position: " + pos + ", Length: " + content.length());
        char deleted = content.charAt(pos);
        Command cmd = new DeleteAtCommand(pos, deleted);
        executeCommand(cmd);
        return deleted;
    }

    // ======================== Insert ========================

    public char insertCurrent(char c) {
        insertRange(cursor, cursor, String.valueOf(c));
        return c;
    }

    public String insertRange(int start, int end, String newContent) {
        if (start > end)
            throw new IllegalArgumentException("start > end: " + start + " > " + end);
        if (start < 0 || end > content.length())
            throw new IndexOutOfBoundsException(
                    "Range [" + start + ", " + end + ") out of bounds for length " + content.length());
        String oldText = content.substring(start, end);
        Command cmd = new InsertRangeCommand(start, oldText, newContent, cursor);
        executeCommand(cmd);
        return oldText;
    }

    // ======================== Replace ========================

    public char replaceCurrent(char c) {
        if (cursor < 0 || cursor >= content.length())
            throw new IndexOutOfBoundsException(
                    "Cursor: " + cursor + ", Length: " + content.length());
        char oldChar = content.charAt(cursor);
        Command cmd = new ReplaceCurrentCommand(cursor, oldChar, c, cursor);
        executeCommand(cmd);
        return oldChar;
    }

    public String replaceRange(int start, int end, String replacement) {
        if (start > end)
            throw new IllegalArgumentException("start > end: " + start + " > " + end);
        if (start < 0 || end > content.length())
            throw new IndexOutOfBoundsException(
                    "Range [" + start + ", " + end + ") out of bounds for length " + content.length());
        String oldText = content.substring(start, end);
        Command cmd = new ReplaceRangeCommand(start, oldText, replacement, cursor);
        executeCommand(cmd);
        return oldText;
    }

    // ======================== Undo / Redo ========================

    private void executeCommand(Command cmd) {
        cmd.execute();
        undos.push(cmd);
        redos.clear();
    }

    public boolean undo() {
        if (undos.isEmpty()) return false;
        Command cmd = undos.pop();
        cmd.undo();
        redos.push(cmd);
        return true;
    }

    public boolean redo() {
        if (redos.isEmpty()) return false;
        Command cmd = redos.pop();
        cmd.execute();
        undos.push(cmd);
        return true;
    }

    // ======================== Command Interface ========================

    interface Command {
        void execute();
        void undo();
    }

    // ======================== Concrete Commands ========================

    class InsertRangeCommand implements Command {
        private final int start;
        private final String oldText;
        private final String newContent;
        private final int preCursor;

        InsertRangeCommand(int start, String oldText, String newContent, int preCursor) {
            this.start = start;
            this.oldText = oldText;
            this.newContent = newContent;
            this.preCursor = preCursor;
        }

        @Override
        public void execute() {
            content.replace(start, start + oldText.length(), newContent);
            cursor = start + newContent.length();
        }

        @Override
        public void undo() {
            content.replace(start, start + newContent.length(), oldText);
            cursor = preCursor;
        }
    }

    class DeleteAtCommand implements Command {
        private final int pos;
        private final char deleted;

        DeleteAtCommand(int pos, char deleted) {
            this.pos = pos;
            this.deleted = deleted;
        }

        @Override
        public void execute() {
            content.deleteCharAt(pos);
            cursor = Math.min(pos, content.length());
        }

        @Override
        public void undo() {
            content.insert(pos, deleted);
            cursor = pos + 1;
        }
    }

    class DeleteRangeCommand implements Command {
        private final int start;
        private final String deleted;
        private final int preCursor;

        DeleteRangeCommand(int start, String deleted, int preCursor) {
            this.start = start;
            this.deleted = deleted;
            this.preCursor = preCursor;
        }

        @Override
        public void execute() {
            content.delete(start, start + deleted.length());
            cursor = Math.min(start, content.length());
        }

        @Override
        public void undo() {
            content.insert(start, deleted);
            cursor = preCursor;
        }
    }

    class ReplaceCurrentCommand implements Command {
        private final int pos;
        private final char oldChar;
        private final char newChar;
        private final int preCursor;

        ReplaceCurrentCommand(int pos, char oldChar, char newChar, int preCursor) {
            this.pos = pos;
            this.oldChar = oldChar;
            this.newChar = newChar;
            this.preCursor = preCursor;
        }

        @Override
        public void execute() {
            content.setCharAt(pos, newChar);
        }

        @Override
        public void undo() {
            content.setCharAt(pos, oldChar);
            cursor = preCursor;
        }
    }

    class ReplaceRangeCommand implements Command {
        private final int start;
        private final String oldText;
        private final String newText;
        private final int preCursor;

        ReplaceRangeCommand(int start, String oldText, String newText, int preCursor) {
            this.start = start;
            this.oldText = oldText;
            this.newText = newText;
            this.preCursor = preCursor;
        }

        @Override
        public void execute() {
            content.replace(start, start + oldText.length(), newText);
            cursor = start + newText.length();
        }

        @Override
        public void undo() {
            content.replace(start, start + newText.length(), oldText);
            cursor = preCursor;
        }
    }
}
