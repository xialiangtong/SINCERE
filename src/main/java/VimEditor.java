import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Vim-like Text Editor
 *
 * Supports: insert, delete, replace, cursor movement, undo, redo.
 * Uses a gap buffer for efficient cursor-local edits and
 * command pattern with undo/redo stacks.
 */
public class VimEditor {

    private StringBuilder buffer;
    private int cursor; // 0-based, points to the position *before* which we insert

    private final Deque<Command> undoStack;
    private final Deque<Command> redoStack;

    public VimEditor() {
        this("");
    }

    public VimEditor(String initialText) {
        this.buffer = new StringBuilder(initialText);
        this.cursor = initialText.length(); // cursor at end
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
    }

    // ==================== Query Methods ====================

    /**
     * Returns the full text content.
     */
    public String getText() {
        return buffer.toString();
    }

    /**
     * Returns the current cursor position (0-based).
     */
    public int getCursor() {
        return cursor;
    }

    /**
     * Returns the length of the text.
     */
    public int length() {
        return buffer.length();
    }

    /**
     * Returns the character at the given position.
     */
    public char charAt(int pos) {
        if (pos < 0 || pos >= buffer.length()) {
            throw new IndexOutOfBoundsException("Position " + pos + " out of range [0, " + buffer.length() + ")");
        }
        return buffer.charAt(pos);
    }

    // ==================== Cursor Movement ====================

    /**
     * Move cursor left by one position.
     */
    public void moveLeft() {
        moveTo(cursor - 1);
    }

    /**
     * Move cursor right by one position.
     */
    public void moveRight() {
        moveTo(cursor + 1);
    }

    /**
     * Move cursor to the beginning of the text.
     */
    public void moveToStart() {
        cursor = 0;
    }

    /**
     * Move cursor to the end of the text.
     */
    public void moveToEnd() {
        cursor = buffer.length();
    }

    /**
     * Move cursor to a specific position.
     * Clamps to valid range [0, length].
     */
    public void moveTo(int pos) {
        cursor = Math.max(0, Math.min(pos, buffer.length()));
    }

    // ==================== Edit Operations ====================

    /**
     * Insert text at the current cursor position.
     * Cursor moves to the end of the inserted text.
     */
    public void insert(String text) {
        if (text == null || text.isEmpty()) return;

        Command cmd = new InsertCommand(cursor, text);
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
    }

    /**
     * Insert a single character at the current cursor position.
     */
    public void insert(char ch) {
        insert(String.valueOf(ch));
    }

    /**
     * Delete the character before the cursor (backspace).
     * Returns the deleted character, or '\0' if nothing to delete.
     */
    public char deleteBack() {
        if (cursor <= 0) return '\0';

        char deleted = buffer.charAt(cursor - 1);
        Command cmd = new DeleteCommand(cursor - 1, String.valueOf(deleted));
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        return deleted;
    }

    /**
     * Delete the character at/under the cursor (like Vim 'x').
     * Returns the deleted character, or '\0' if nothing to delete.
     */
    public char deleteAt() {
        if (cursor >= buffer.length()) return '\0';

        char deleted = buffer.charAt(cursor);
        Command cmd = new DeleteAtCommand(cursor, String.valueOf(deleted));
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        return deleted;
    }

    /**
     * Delete a range of characters [start, end).
     * Returns the deleted text.
     */
    public String deleteRange(int start, int end) {
        start = Math.max(0, start);
        end = Math.min(end, buffer.length());
        if (start >= end) return "";

        String deleted = buffer.substring(start, end);
        Command cmd = new DeleteRangeCommand(start, deleted, cursor);
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        return deleted;
    }

    /**
     * Replace the character at the given position (like Vim 'r').
     */
    public void replaceChar(int pos, char newChar) {
        if (pos < 0 || pos >= buffer.length()) {
            throw new IndexOutOfBoundsException("Position " + pos + " out of range [0, " + buffer.length() + ")");
        }

        char oldChar = buffer.charAt(pos);
        Command cmd = new ReplaceCharCommand(pos, oldChar, newChar);
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
    }

    /**
     * Replace a range [start, end) with new text.
     */
    public void replaceRange(int start, int end, String newText) {
        start = Math.max(0, start);
        end = Math.min(end, buffer.length());
        if (start > end) {
            throw new IllegalArgumentException("start (" + start + ") > end (" + end + ")");
        }

        String oldText = buffer.substring(start, end);
        Command cmd = new ReplaceRangeCommand(start, oldText, newText, cursor);
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
    }

    // ==================== Undo / Redo ====================

    /**
     * Undo the last operation.
     * @return true if an operation was undone, false if nothing to undo.
     */
    public boolean undo() {
        if (undoStack.isEmpty()) return false;

        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        return true;
    }

    /**
     * Redo the last undone operation.
     * @return true if an operation was redone, false if nothing to redo.
     */
    public boolean redo() {
        if (redoStack.isEmpty()) return false;

        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
        return true;
    }

    /**
     * Returns true if undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Returns true if redo is available.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    // ==================== Command Pattern ====================

    /**
     * Command interface for undoable operations.
     */
    private interface Command {
        void execute();
        void undo();
    }

    /**
     * Insert text at a position.
     */
    private class InsertCommand implements Command {
        private final int pos;
        private final String text;

        InsertCommand(int pos, String text) {
            this.pos = pos;
            this.text = text;
        }

        @Override
        public void execute() {
            buffer.insert(pos, text);
            cursor = pos + text.length();
        }

        @Override
        public void undo() {
            buffer.delete(pos, pos + text.length());
            cursor = pos;
        }
    }

    /**
     * Delete character before cursor (backspace).
     */
    private class DeleteCommand implements Command {
        private final int pos;
        private final String deleted;

        DeleteCommand(int pos, String deleted) {
            this.pos = pos;
            this.deleted = deleted;
        }

        @Override
        public void execute() {
            buffer.delete(pos, pos + deleted.length());
            cursor = pos;
        }

        @Override
        public void undo() {
            buffer.insert(pos, deleted);
            cursor = pos + deleted.length();
        }
    }

    /**
     * Delete character at/under cursor (Vim 'x').
     */
    private class DeleteAtCommand implements Command {
        private final int pos;
        private final String deleted;

        DeleteAtCommand(int pos, String deleted) {
            this.pos = pos;
            this.deleted = deleted;
        }

        @Override
        public void execute() {
            buffer.delete(pos, pos + deleted.length());
            // Cursor stays, but clamp if at end
            cursor = Math.min(pos, buffer.length());
        }

        @Override
        public void undo() {
            buffer.insert(pos, deleted);
            cursor = pos;
        }
    }

    /**
     * Delete a range of characters.
     */
    private class DeleteRangeCommand implements Command {
        private final int start;
        private final String deleted;
        private final int prevCursor;

        DeleteRangeCommand(int start, String deleted, int prevCursor) {
            this.start = start;
            this.deleted = deleted;
            this.prevCursor = prevCursor;
        }

        @Override
        public void execute() {
            buffer.delete(start, start + deleted.length());
            cursor = Math.min(start, buffer.length());
        }

        @Override
        public void undo() {
            buffer.insert(start, deleted);
            cursor = prevCursor;
        }
    }

    /**
     * Replace a single character.
     */
    private class ReplaceCharCommand implements Command {
        private final int pos;
        private final char oldChar;
        private final char newChar;

        ReplaceCharCommand(int pos, char oldChar, char newChar) {
            this.pos = pos;
            this.oldChar = oldChar;
            this.newChar = newChar;
        }

        @Override
        public void execute() {
            buffer.setCharAt(pos, newChar);
        }

        @Override
        public void undo() {
            buffer.setCharAt(pos, oldChar);
        }
    }

    /**
     * Replace a range of text.
     */
    private class ReplaceRangeCommand implements Command {
        private final int start;
        private final String oldText;
        private final String newText;
        private final int prevCursor;

        ReplaceRangeCommand(int start, String oldText, String newText, int prevCursor) {
            this.start = start;
            this.oldText = oldText;
            this.newText = newText;
            this.prevCursor = prevCursor;
        }

        @Override
        public void execute() {
            buffer.delete(start, start + oldText.length());
            buffer.insert(start, newText);
            cursor = start + newText.length();
        }

        @Override
        public void undo() {
            buffer.delete(start, start + newText.length());
            buffer.insert(start, oldText);
            cursor = prevCursor;
        }
    }
}
