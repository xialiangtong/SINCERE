import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VimOperations Tests")
public class VimOperationsTest {

    private VimOperations vim;

    @BeforeEach
    void setUp() {
        vim = new VimOperations("Hello");
    }

    // ──────────────────────────────────────────────
    // 1. Constructor & Getters
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Constructor & Getter Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor — empty content, cursor at 0")
        void defaultConstructor() {
            VimOperations v = new VimOperations();
            assertEquals("", v.getContent());
            assertEquals(0, v.getCursor());
        }

        @Test
        @DisplayName("String constructor — content set, cursor at 0")
        void stringConstructor() {
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("Null string constructor — treated as empty")
        void nullConstructor() {
            VimOperations v = new VimOperations(null);
            assertEquals("", v.getContent());
            assertEquals(0, v.getCursor());
        }
    }

    // ──────────────────────────────────────────────
    // 2. Cursor Movement
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Cursor Movement Tests")
    class CursorMovementTests {

        @Test
        @DisplayName("moveCursorForward — moves from 0 to 1")
        void moveForward() {
            assertEquals(1, vim.moveCursorForward());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("moveCursorForward multiple times")
        void moveForwardMultiple() {
            vim.moveCursorForward();
            vim.moveCursorForward();
            vim.moveCursorForward();
            assertEquals(3, vim.getCursor());
        }

        @Test
        @DisplayName("moveCursorForward clamped at content.length()")
        void moveForwardClampedAtTail() {
            // "Hello" length=5, move 6 times
            for (int i = 0; i < 6; i++) {
                vim.moveCursorForward();
            }
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("moveCursorForward on empty content — stays at 0")
        void moveForwardOnEmpty() {
            VimOperations v = new VimOperations();
            assertEquals(0, v.moveCursorForward());
            assertEquals(0, v.getCursor());
        }

        @Test
        @DisplayName("moveCursorBackward — moves from 1 to 0")
        void moveBackward() {
            vim.moveCursorForward(); // cursor=1
            assertEquals(0, vim.moveCursorBackward());
        }

        @Test
        @DisplayName("moveCursorBackward clamped at 0")
        void moveBackwardClampedAtHead() {
            assertEquals(0, vim.moveCursorBackward());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("moveCursorBackward on empty content — stays at 0")
        void moveBackwardOnEmpty() {
            VimOperations v = new VimOperations();
            assertEquals(0, v.moveCursorBackward());
        }

        @Test
        @DisplayName("moveCursorToHead — cursor goes to 0")
        void moveToHead() {
            vim.moveCursorForward();
            vim.moveCursorForward();
            assertEquals(0, vim.moveCursorToHead());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("moveCursorToHead when already at head — stays 0")
        void moveToHeadAlreadyAtHead() {
            assertEquals(0, vim.moveCursorToHead());
        }

        @Test
        @DisplayName("moveCursorToTail — cursor goes to content.length()")
        void moveToTail() {
            assertEquals(5, vim.moveCursorToTail());
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("moveCursorToTail when already at tail — stays")
        void moveToTailAlreadyAtTail() {
            vim.moveCursorToTail();
            assertEquals(5, vim.moveCursorToTail());
        }

        @Test
        @DisplayName("moveCursorToTail on empty content — stays 0")
        void moveToTailOnEmpty() {
            VimOperations v = new VimOperations();
            assertEquals(0, v.moveCursorToTail());
        }

        @Test
        @DisplayName("Forward then backward — returns to original position")
        void forwardThenBackwardRoundTrip() {
            vim.moveCursorForward();
            vim.moveCursorForward();
            vim.moveCursorBackward();
            vim.moveCursorBackward();
            assertEquals(0, vim.getCursor());
        }
    }

    // ──────────────────────────────────────────────
    // 3. Delete Operations
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("deleteBackward — deletes char before cursor")
        void deleteBackward() {
            vim.moveCursorForward(); // cursor=1, before 'e'
            char deleted = vim.deleteBackward();
            assertEquals('H', deleted);
            assertEquals("ello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("deleteBackward at head — no-op, returns '\\0'")
        void deleteBackwardAtHead() {
            char deleted = vim.deleteBackward();
            assertEquals('\0', deleted);
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("deleteBackward on empty content — no-op")
        void deleteBackwardOnEmpty() {
            VimOperations v = new VimOperations();
            assertEquals('\0', v.deleteBackward());
            assertEquals("", v.getContent());
        }

        @Test
        @DisplayName("deleteBackward at tail — deletes last char")
        void deleteBackwardAtTail() {
            vim.moveCursorToTail(); // cursor=5
            char deleted = vim.deleteBackward();
            assertEquals('o', deleted);
            assertEquals("Hell", vim.getContent());
            assertEquals(4, vim.getCursor());
        }

        @Test
        @DisplayName("deleteCurrent — deletes char at cursor")
        void deleteCurrent() {
            // cursor=0, char at 0 is 'H'
            char deleted = vim.deleteCurrent();
            assertEquals('H', deleted);
            assertEquals("ello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("deleteCurrent at tail — no-op, returns '\\0'")
        void deleteCurrentAtTail() {
            vim.moveCursorToTail(); // cursor=5, nothing at index 5
            char deleted = vim.deleteCurrent();
            assertEquals('\0', deleted);
            assertEquals("Hello", vim.getContent());
        }

        @Test
        @DisplayName("deleteCurrent on empty content — no-op")
        void deleteCurrentOnEmpty() {
            VimOperations v = new VimOperations();
            assertEquals('\0', v.deleteCurrent());
        }

        @Test
        @DisplayName("deleteRange — deletes [start, end)")
        void deleteRange() {
            // "Hello" → delete [1,3) = "el" → "Hlo"
            String deleted = vim.deleteRange(1, 3);
            assertEquals("el", deleted);
            assertEquals("Hlo", vim.getContent());
        }

        @Test
        @DisplayName("deleteRange — cursor before range stays unchanged")
        void deleteRangeCursorBeforeRange() {
            // cursor=0, delete [2,4)
            String deleted = vim.deleteRange(2, 4);
            assertEquals("ll", deleted);
            assertEquals("Heo", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("deleteRange — cursor inside range moves to start")
        void deleteRangeCursorInsideRange() {
            vim.moveCursorForward(); // cursor=1
            vim.moveCursorForward(); // cursor=2
            // cursor=2, delete [1,4) = "ell"
            vim.deleteRange(1, 4);
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("deleteRange — cursor after range shifts left")
        void deleteRangeCursorAfterRange() {
            vim.moveCursorToTail(); // cursor=5
            // delete [0,2) = "He" → "llo", cursor was 5 → now 3
            vim.deleteRange(0, 2);
            assertEquals("llo", vim.getContent());
            assertEquals(3, vim.getCursor());
        }

        @Test
        @DisplayName("deleteRange — start == end, no-op")
        void deleteRangeEmpty() {
            String deleted = vim.deleteRange(2, 2);
            assertEquals("", deleted);
            assertEquals("Hello", vim.getContent());
        }

        @Test
        @DisplayName("deleteRange — start < 0, throws exception")
        void deleteRangeStartNegative() {
            assertThrows(IllegalArgumentException.class, () -> vim.deleteRange(-1, 2));
        }

        @Test
        @DisplayName("deleteRange — end > content.length(), throws exception")
        void deleteRangeEndBeyondLength() {
            assertThrows(IllegalArgumentException.class, () -> vim.deleteRange(0, 10));
        }

        @Test
        @DisplayName("deleteRange — start > end, throws exception")
        void deleteRangeStartGreaterThanEnd() {
            assertThrows(IllegalArgumentException.class, () -> vim.deleteRange(3, 1));
        }

        @Test
        @DisplayName("deleteBackward multiple times consecutively")
        void deleteBackwardConsecutive() {
            vim.moveCursorForward();
            vim.moveCursorForward();
            vim.moveCursorForward(); // cursor=3
            assertEquals('l', vim.deleteBackward()); // "Helo", cursor=2
            assertEquals('e', vim.deleteBackward()); // "Hlo", cursor=1
            assertEquals('H', vim.deleteBackward()); // "lo", cursor=0
            assertEquals('\0', vim.deleteBackward()); // no-op at head
            assertEquals("lo", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("deleteCurrent on single-char content — leaves empty")
        void deleteCurrentSingleChar() {
            VimOperations v = new VimOperations("X");
            char deleted = v.deleteCurrent();
            assertEquals('X', deleted);
            assertEquals("", v.getContent());
            assertEquals(0, v.getCursor());
        }

        @Test
        @DisplayName("deleteRange entire content — empties, cursor to 0")
        void deleteRangeEntireContent() {
            vim.moveCursorForward();
            vim.moveCursorForward(); // cursor=2
            vim.deleteRange(0, 5);
            assertEquals("", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("deleteRange (0,0) at head — no-op")
        void deleteRangeZeroZero() {
            String deleted = vim.deleteRange(0, 0);
            assertEquals("", deleted);
            assertEquals("Hello", vim.getContent());
        }
    }

    // ──────────────────────────────────────────────
    // 4. Insert Operations
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Insert Tests")
    class InsertTests {

        @Test
        @DisplayName("insertCurrent — inserts at cursor, cursor moves forward")
        void insertCurrent() {
            char inserted = vim.insertCurrent('X');
            assertEquals('X', inserted);
            assertEquals("XHello", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("insertCurrent at tail — appends")
        void insertCurrentAtTail() {
            vim.moveCursorToTail();
            vim.insertCurrent('!');
            assertEquals("Hello!", vim.getContent());
            assertEquals(6, vim.getCursor());
        }

        @Test
        @DisplayName("insertCurrent in middle")
        void insertCurrentInMiddle() {
            vim.moveCursorForward();
            vim.moveCursorForward(); // cursor=2
            vim.insertCurrent('X');
            assertEquals("HeXllo", vim.getContent());
            assertEquals(3, vim.getCursor());
        }

        @Test
        @DisplayName("insertCurrent on empty content")
        void insertCurrentOnEmpty() {
            VimOperations v = new VimOperations();
            v.insertCurrent('A');
            assertEquals("A", v.getContent());
            assertEquals(1, v.getCursor());
        }

        @Test
        @DisplayName("insertRange — inserts string at position")
        void insertRange() {
            String inserted = vim.insertRange(2, "XY");
            assertEquals("XY", inserted);
            assertEquals("HeXYllo", vim.getContent());
        }

        @Test
        @DisplayName("insertRange — cursor before pos stays")
        void insertRangeCursorBeforePos() {
            // cursor=0, insert at pos=3
            vim.insertRange(3, "XX");
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("insertRange — cursor at/after pos shifts right")
        void insertRangeCursorAfterPos() {
            vim.moveCursorToTail(); // cursor=5
            vim.insertRange(2, "XX"); // "HeXXllo", length 7
            assertEquals(7, vim.getCursor());
        }

        @Test
        @DisplayName("insertRange at 0 — prepends")
        void insertRangeAtHead() {
            vim.insertRange(0, "Hi ");
            assertEquals("Hi Hello", vim.getContent());
        }

        @Test
        @DisplayName("insertRange at length — appends")
        void insertRangeAtTail() {
            vim.insertRange(5, " World");
            assertEquals("Hello World", vim.getContent());
        }

        @Test
        @DisplayName("insertRange — pos < 0, throws exception")
        void insertRangeNegativePos() {
            assertThrows(IllegalArgumentException.class, () -> vim.insertRange(-1, "X"));
        }

        @Test
        @DisplayName("insertRange — pos > content.length(), throws exception")
        void insertRangePosBeyondLength() {
            assertThrows(IllegalArgumentException.class, () -> vim.insertRange(10, "X"));
        }

        @Test
        @DisplayName("insertCurrent multiple chars consecutively")
        void insertCurrentConsecutive() {
            vim.insertCurrent('A'); // "AHello", cursor=1
            vim.insertCurrent('B'); // "ABHello", cursor=2
            vim.insertCurrent('C'); // "ABCHello", cursor=3
            assertEquals("ABCHello", vim.getContent());
            assertEquals(3, vim.getCursor());
        }

        @Test
        @DisplayName("insertRange with empty string — no-op")
        void insertRangeEmptyString() {
            vim.insertRange(2, "");
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("insertRange with null — throws exception")
        void insertRangeNull() {
            assertThrows(IllegalArgumentException.class, () -> vim.insertRange(2, null));
        }
    }

    // ──────────────────────────────────────────────
    // 5. Replace Operations
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Replace Tests")
    class ReplaceTests {

        @Test
        @DisplayName("replaceCurrent — replaces char at cursor")
        void replaceCurrent() {
            char old = vim.replaceCurrent('X');
            assertEquals('H', old);
            assertEquals("Xello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("replaceCurrent at tail — no-op, returns '\\0'")
        void replaceCurrentAtTail() {
            vim.moveCursorToTail();
            char old = vim.replaceCurrent('X');
            assertEquals('\0', old);
            assertEquals("Hello", vim.getContent());
        }

        @Test
        @DisplayName("replaceCurrent on empty — no-op")
        void replaceCurrentOnEmpty() {
            VimOperations v = new VimOperations();
            assertEquals('\0', v.replaceCurrent('X'));
        }

        @Test
        @DisplayName("replaceCurrent in middle")
        void replaceCurrentInMiddle() {
            vim.moveCursorForward();
            vim.moveCursorForward(); // cursor=2
            char old = vim.replaceCurrent('X');
            assertEquals('l', old);
            assertEquals("HeXlo", vim.getContent());
            assertEquals(2, vim.getCursor());
        }

        @Test
        @DisplayName("replaceRange — replaces [start, end) with new string")
        void replaceRange() {
            // "Hello" → replace [1,4) "ell" with "XY" → "HXYo"
            String old = vim.replaceRange(1, 4, "XY");
            assertEquals("ell", old);
            assertEquals("HXYo", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange — replacement longer than original")
        void replaceRangeLonger() {
            // "Hello" → replace [0,1) "H" with "XYZ" → "XYZello"
            String old = vim.replaceRange(0, 1, "XYZ");
            assertEquals("H", old);
            assertEquals("XYZello", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange — replacement shorter than original")
        void replaceRangeShorter() {
            // "Hello" → replace [0,3) "Hel" with "X" → "Xlo"
            String old = vim.replaceRange(0, 3, "X");
            assertEquals("Hel", old);
            assertEquals("Xlo", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange — cursor before range stays")
        void replaceRangeCursorBefore() {
            // cursor=0, replace [2,4)
            vim.replaceRange(2, 4, "XX");
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("replaceRange — cursor inside range moves to start")
        void replaceRangeCursorInside() {
            vim.moveCursorForward();
            vim.moveCursorForward(); // cursor=2
            vim.replaceRange(1, 4, "X");
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("replaceRange — cursor after range adjusts")
        void replaceRangeCursorAfter() {
            vim.moveCursorToTail(); // cursor=5
            // replace [0,2) "He" with "X" → "Xllo", cursor was 5 → 5 - 2 + 1 = 4
            vim.replaceRange(0, 2, "X");
            assertEquals("Xllo", vim.getContent());
            assertEquals(4, vim.getCursor());
        }

        @Test
        @DisplayName("replaceRange — start < 0, throws exception")
        void replaceRangeStartNegative() {
            assertThrows(IllegalArgumentException.class, () -> vim.replaceRange(-1, 2, "X"));
        }

        @Test
        @DisplayName("replaceRange — end > content.length(), throws exception")
        void replaceRangeEndBeyondLength() {
            assertThrows(IllegalArgumentException.class, () -> vim.replaceRange(0, 10, "X"));
        }

        @Test
        @DisplayName("replaceRange — start > end, throws exception")
        void replaceRangeStartGreaterThanEnd() {
            assertThrows(IllegalArgumentException.class, () -> vim.replaceRange(3, 1, "X"));
        }

        @Test
        @DisplayName("replaceCurrent with same char — content unchanged")
        void replaceCurrentSameChar() {
            char old = vim.replaceCurrent('H');
            assertEquals('H', old);
            assertEquals("Hello", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange with empty replacement — acts as delete")
        void replaceRangeEmptyReplacement() {
            String old = vim.replaceRange(1, 4, "");
            assertEquals("ell", old);
            assertEquals("Ho", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange entire content")
        void replaceRangeEntireContent() {
            String old = vim.replaceRange(0, 5, "World");
            assertEquals("Hello", old);
            assertEquals("World", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange with null replacement — throws exception")
        void replaceRangeNullReplacement() {
            assertThrows(IllegalArgumentException.class, () -> vim.replaceRange(0, 2, null));
        }

        @Test
        @DisplayName("replaceRange start == end — acts as insert")
        void replaceRangeStartEqualsEnd() {
            vim.replaceRange(2, 2, "XY");
            assertEquals("HeXYllo", vim.getContent());
        }
    }

    // ──────────────────────────────────────────────
    // 6. Combined Operations
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Combined Operation Tests")
    class CombinedTests {

        @Test
        @DisplayName("Insert then delete — content restored")
        void insertThenDelete() {
            vim.insertCurrent('X'); // "XHello", cursor=1
            vim.moveCursorBackward(); // cursor=0
            vim.deleteCurrent(); // delete 'X' → "Hello", cursor=0
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("Move to tail, insert, move to head")
        void moveInsertMove() {
            vim.moveCursorToTail();
            vim.insertCurrent('!'); // "Hello!", cursor=6
            vim.moveCursorToHead(); // cursor=0
            assertEquals("Hello!", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("Multiple deleteBackward from tail — empties content")
        void deleteAllFromTail() {
            vim.moveCursorToTail();
            for (int i = 0; i < 5; i++) {
                vim.deleteBackward();
            }
            assertEquals("", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("Replace then move and insert")
        void replaceThenInsert() {
            vim.replaceCurrent('J'); // "Jello", cursor=0
            vim.moveCursorToTail();
            vim.insertCurrent('!'); // "Jello!", cursor=6
            assertEquals("Jello!", vim.getContent());
        }

        @Test
        @DisplayName("deleteBackward after insertCurrent — returns inserted char")
        void deleteBackwardAfterInsert() {
            vim.insertCurrent('Z'); // "ZHello", cursor=1
            char deleted = vim.deleteBackward(); // back to "Hello", cursor=0
            assertEquals('Z', deleted);
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("All operations on single-character content")
        void singleCharContent() {
            VimOperations v = new VimOperations("A");
            assertEquals(0, v.getCursor());

            // replaceCurrent
            assertEquals('A', v.replaceCurrent('B'));
            assertEquals("B", v.getContent());

            // moveCursorForward
            v.moveCursorForward(); // cursor=1
            assertEquals(1, v.getCursor());

            // deleteBackward at tail
            assertEquals('B', v.deleteBackward());
            assertEquals("", v.getContent());
            assertEquals(0, v.getCursor());

            // insertCurrent on empty
            v.insertCurrent('C');
            assertEquals("C", v.getContent());
            assertEquals(1, v.getCursor());

            // moveCursorBackward
            v.moveCursorBackward(); // cursor=0
            assertEquals(0, v.getCursor());

            // deleteCurrent
            assertEquals('C', v.deleteCurrent());
            assertEquals("", v.getContent());
        }
    }

    // ──────────────────────────────────────────────
    // 7. Undo / Redo Operations
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Undo/Redo Tests")
    class UndoRedoTests {

        // --- Undo basics ---

        @Test
        @DisplayName("undo on empty history — no-op")
        void undoEmptyHistory() {
            vim.undo();
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("undo insertCurrent — removes inserted char, restores cursor")
        void undoInsertCurrent() {
            vim.insertCurrent('X'); // "XHello", cursor=1
            vim.undo();
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("undo deleteBackward — restores char and cursor")
        void undoDeleteBackward() {
            vim.moveCursorForward(); // cursor=1
            vim.deleteBackward(); // "ello", cursor=0
            vim.undo();
            assertEquals("Hello", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("undo deleteCurrent — restores char and cursor")
        void undoDeleteCurrent() {
            vim.deleteCurrent(); // "ello", cursor=0
            vim.undo();
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("undo deleteRange — restores substring and cursor")
        void undoDeleteRange() {
            vim.moveCursorForward();
            vim.moveCursorForward(); // cursor=2
            vim.deleteRange(1, 4); // "Ho", cursor=1
            vim.undo();
            assertEquals("Hello", vim.getContent());
            assertEquals(2, vim.getCursor());
        }

        @Test
        @DisplayName("undo insertRange — removes inserted string and restores cursor")
        void undoInsertRange() {
            vim.moveCursorToTail(); // cursor=5
            vim.insertRange(2, "XY"); // "HeXYllo", cursor=7
            vim.undo();
            assertEquals("Hello", vim.getContent());
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("undo replaceCurrent — restores original char")
        void undoReplaceCurrent() {
            vim.replaceCurrent('Z'); // "Zello", cursor=0
            vim.undo();
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("undo replaceRange — restores original substring and cursor")
        void undoReplaceRange() {
            vim.moveCursorToTail(); // cursor=5
            vim.replaceRange(0, 2, "X"); // "Xllo", cursor=4
            vim.undo();
            assertEquals("Hello", vim.getContent());
            assertEquals(5, vim.getCursor());
        }

        // --- Redo basics ---

        @Test
        @DisplayName("redo on empty redoStack — no-op")
        void redoEmptyStack() {
            vim.redo();
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("redo after undo insertCurrent — re-inserts char")
        void redoInsertCurrent() {
            vim.insertCurrent('X'); // "XHello", cursor=1
            vim.undo(); // "Hello", cursor=0
            vim.redo(); // "XHello", cursor=1
            assertEquals("XHello", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("redo after undo deleteBackward — re-deletes char")
        void redoDeleteBackward() {
            vim.moveCursorForward(); // cursor=1
            vim.deleteBackward(); // "ello", cursor=0
            vim.undo(); // "Hello", cursor=1
            vim.redo(); // "ello", cursor=0
            assertEquals("ello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("redo after undo replaceRange — re-applies replacement")
        void redoReplaceRange() {
            vim.replaceRange(0, 3, "XY"); // "XYlo", cursor=0
            vim.undo(); // "Hello", cursor=0
            vim.redo(); // "XYlo", cursor=0
            assertEquals("XYlo", vim.getContent());
        }

        // --- Multiple undo/redo ---

        @Test
        @DisplayName("multiple undos — unwinds all operations")
        void multipleUndos() {
            vim.insertCurrent('A'); // "AHello", cursor=1
            vim.insertCurrent('B'); // "ABHello", cursor=2
            vim.insertCurrent('C'); // "ABCHello", cursor=3
            vim.undo(); // "ABHello", cursor=2
            assertEquals("ABHello", vim.getContent());
            assertEquals(2, vim.getCursor());
            vim.undo(); // "AHello", cursor=1
            assertEquals("AHello", vim.getContent());
            assertEquals(1, vim.getCursor());
            vim.undo(); // "Hello", cursor=0
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("multiple redos — re-applies all undone operations")
        void multipleRedos() {
            vim.insertCurrent('A');
            vim.insertCurrent('B');
            vim.insertCurrent('C');
            vim.undo();
            vim.undo();
            vim.undo(); // back to "Hello"
            vim.redo(); // "AHello"
            assertEquals("AHello", vim.getContent());
            assertEquals(1, vim.getCursor());
            vim.redo(); // "ABHello"
            assertEquals("ABHello", vim.getContent());
            assertEquals(2, vim.getCursor());
            vim.redo(); // "ABCHello"
            assertEquals("ABCHello", vim.getContent());
            assertEquals(3, vim.getCursor());
        }

        @Test
        @DisplayName("undo past beginning — extra undos are no-ops")
        void undoPastBeginning() {
            vim.insertCurrent('X');
            vim.undo();
            vim.undo(); // no-op
            vim.undo(); // no-op
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("redo past end — extra redos are no-ops")
        void redoPastEnd() {
            vim.insertCurrent('X');
            vim.undo();
            vim.redo();
            vim.redo(); // no-op
            assertEquals("XHello", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        // --- Redo invalidation ---

        @Test
        @DisplayName("new operation after undo — clears redo stack")
        void newOpClearsRedo() {
            vim.insertCurrent('A'); // "AHello"
            vim.insertCurrent('B'); // "ABHello"
            vim.undo(); // "AHello"
            vim.insertCurrent('Z'); // "AZHello" — diverged
            vim.redo(); // no-op, redo stack was cleared
            assertEquals("AZHello", vim.getContent());
            assertEquals(2, vim.getCursor());
        }

        // --- Cursor movement does not affect undo/redo ---

        @Test
        @DisplayName("cursor moves are not in undo history")
        void cursorMovesNotUndone() {
            vim.insertCurrent('X'); // "XHello", cursor=1
            vim.moveCursorForward(); // cursor=2
            vim.moveCursorForward(); // cursor=3
            vim.undo(); // undoes insertCurrent, not cursor moves
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        // --- Mixed operation undo/redo ---

        @Test
        @DisplayName("undo mixed ops — delete then insert then undo both")
        void undoMixedOps() {
            vim.deleteCurrent(); // "ello", cursor=0
            vim.insertCurrent('Z'); // "Zello", cursor=1
            vim.undo(); // "ello", cursor=0
            assertEquals("ello", vim.getContent());
            assertEquals(0, vim.getCursor());
            vim.undo(); // "Hello", cursor=0
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("undo/redo round-trip — state fully restored")
        void undoRedoRoundTrip() {
            vim.replaceCurrent('J'); // "Jello"
            vim.moveCursorToTail();
            vim.insertCurrent('!'); // "Jello!"
            vim.undo(); // "Jello", cursor=5
            vim.undo(); // "Hello", cursor=0
            vim.redo(); // "Jello", cursor=0
            vim.redo(); // "Jello!", cursor=6
            assertEquals("Jello!", vim.getContent());
            assertEquals(6, vim.getCursor());
        }

        @Test
        @DisplayName("undo deleteRange then redo — range deleted again")
        void undoRedoDeleteRange() {
            vim.deleteRange(1, 4); // "Ho", cursor=0
            vim.undo(); // "Hello", cursor=0
            assertEquals("Hello", vim.getContent());
            vim.redo(); // "Ho", cursor=0
            assertEquals("Ho", vim.getContent());
        }

        @Test
        @DisplayName("undo insertRange then redo — range inserted again")
        void undoRedoInsertRange() {
            vim.insertRange(2, "XYZ"); // "HeXYZllo"
            vim.undo();
            assertEquals("Hello", vim.getContent());
            vim.redo();
            assertEquals("HeXYZllo", vim.getContent());
        }

        // --- Redo for remaining command types ---

        @Test
        @DisplayName("redo after undo deleteCurrent — re-deletes char")
        void redoDeleteCurrent() {
            vim.deleteCurrent(); // "ello", cursor=0
            vim.undo(); // "Hello", cursor=0
            vim.redo(); // "ello", cursor=0
            assertEquals("ello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("redo after undo replaceCurrent — re-replaces char")
        void redoReplaceCurrent() {
            vim.replaceCurrent('Z'); // "Zello", cursor=0
            vim.undo(); // "Hello", cursor=0
            vim.redo(); // "Zello", cursor=0
            assertEquals("Zello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("redo insertRange — verifies cursor restored")
        void redoInsertRangeWithCursor() {
            vim.moveCursorToTail(); // cursor=5
            vim.insertRange(2, "XY"); // "HeXYllo", cursor=7
            vim.undo(); // "Hello", cursor=5
            assertEquals(5, vim.getCursor());
            vim.redo(); // "HeXYllo", cursor=7
            assertEquals("HeXYllo", vim.getContent());
            assertEquals(7, vim.getCursor());
        }

        @Test
        @DisplayName("redo deleteRange — verifies cursor restored")
        void redoDeleteRangeWithCursor() {
            vim.moveCursorToTail(); // cursor=5
            vim.deleteRange(0, 2); // "llo", cursor=3
            vim.undo(); // "Hello", cursor=5
            assertEquals(5, vim.getCursor());
            vim.redo(); // "llo", cursor=3
            assertEquals("llo", vim.getContent());
            assertEquals(3, vim.getCursor());
        }

        // --- Undo on empty content ---

        @Test
        @DisplayName("undo restores content from empty — delete all then undo")
        void undoRestoresFromEmpty() {
            vim.deleteRange(0, 5); // "", cursor=0
            assertEquals("", vim.getContent());
            vim.undo(); // "Hello", cursor=0
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("undo insertCurrent on empty content — back to empty")
        void undoInsertCurrentOnEmpty() {
            VimOperations v = new VimOperations();
            v.insertCurrent('A'); // "A", cursor=1
            v.undo(); // "", cursor=0
            assertEquals("", v.getContent());
            assertEquals(0, v.getCursor());
        }

        // --- Interleaved undo/redo ---

        @Test
        @DisplayName("interleaved undo/redo — undo 2, redo 1, undo 1")
        void interleavedUndoRedo() {
            vim.insertCurrent('A'); // "AHello", cursor=1
            vim.insertCurrent('B'); // "ABHello", cursor=2
            vim.insertCurrent('C'); // "ABCHello", cursor=3
            vim.undo(); // "ABHello", cursor=2
            vim.undo(); // "AHello", cursor=1
            vim.redo(); // "ABHello", cursor=2
            assertEquals("ABHello", vim.getContent());
            assertEquals(2, vim.getCursor());
            vim.undo(); // "AHello", cursor=1
            assertEquals("AHello", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("undo after undo+redo cycle — can undo again")
        void undoAfterUndoRedoCycle() {
            vim.replaceCurrent('X'); // "Xello"
            vim.undo(); // "Hello"
            vim.redo(); // "Xello"
            vim.undo(); // "Hello"
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        // --- Partial redo then new op ---

        @Test
        @DisplayName("partial redo then new op — clears remaining redo")
        void partialRedoThenNewOp() {
            vim.insertCurrent('A'); // "AHello"
            vim.insertCurrent('B'); // "ABHello"
            vim.insertCurrent('C'); // "ABCHello"
            vim.undo(); // "ABHello"
            vim.undo(); // "AHello"
            vim.undo(); // "Hello"
            vim.redo(); // "AHello" — redo stack still has B, C
            assertEquals("AHello", vim.getContent());
            vim.insertCurrent('Z'); // "AZHello" — new op clears redo
            vim.redo(); // no-op
            assertEquals("AZHello", vim.getContent());
            assertEquals(2, vim.getCursor());
        }

        // --- Undo replaceRange with length-changing replacement ---

        @Test
        @DisplayName("undo replaceRange shorter — cursor restored after shrink")
        void undoReplaceRangeShorter() {
            vim.moveCursorToTail(); // cursor=5
            vim.replaceRange(0, 3, "X"); // "Xlo", cursor=3
            assertEquals(3, vim.getCursor());
            vim.undo(); // "Hello", cursor=5
            assertEquals("Hello", vim.getContent());
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("undo replaceRange longer — cursor restored after grow")
        void undoReplaceRangeLonger() {
            vim.moveCursorToTail(); // cursor=5
            vim.replaceRange(0, 1, "XYZ"); // "XYZello", cursor=7
            assertEquals(7, vim.getCursor());
            vim.undo(); // "Hello", cursor=5
            assertEquals("Hello", vim.getContent());
            assertEquals(5, vim.getCursor());
        }

        // --- Cursor movement between op and undo ---

        @Test
        @DisplayName("cursor move between op and undo — undo still restores correctly")
        void cursorMoveBetweenOpAndUndo() {
            vim.insertCurrent('X'); // "XHello", cursor=1
            vim.moveCursorToTail(); // cursor=6
            vim.moveCursorBackward(); // cursor=5
            vim.undo(); // undoes insert, restores cursor to 0 (prevCursor)
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        // --- Stress: all 6 command types in sequence ---

        @Test
        @DisplayName("undo/redo all 6 command types in sequence")
        void undoRedoAllCommandTypes() {
            // 1. insertCurrent
            vim.insertCurrent('A'); // "AHello", cursor=1
            // 2. deleteCurrent
            vim.deleteCurrent(); // "Aello", cursor=1 (deleted 'H')
            // 3. deleteBackward
            vim.moveCursorForward(); // cursor=2
            vim.deleteBackward(); // "Allo", cursor=1 (deleted 'e' at idx 1)
            // 4. insertRange
            vim.insertRange(1, "XY"); // "AXYllo", cursor=3
            // 5. replaceCurrent
            vim.replaceCurrent('Z'); // "AXYZlo", cursor=3 (replaced 'l' with 'Z')
            // 6. replaceRange
            vim.replaceRange(0, 2, "QW"); // "QWYZlo", cursor=3

            assertEquals("QWYZlo", vim.getContent());
            assertEquals(3, vim.getCursor());

            // Undo all 6 in reverse
            vim.undo(); // undo replaceRange → "AXYZlo", cursor=3
            assertEquals("AXYZlo", vim.getContent());
            vim.undo(); // undo replaceCurrent → "AXYllo", cursor=3
            assertEquals("AXYllo", vim.getContent());
            vim.undo(); // undo insertRange → "Allo", cursor=1
            assertEquals("Allo", vim.getContent());
            assertEquals(1, vim.getCursor());
            vim.undo(); // undo deleteBackward → "Aello", cursor=2
            assertEquals("Aello", vim.getContent());
            assertEquals(2, vim.getCursor());
            vim.undo(); // undo deleteCurrent → "AHello", cursor=1
            assertEquals("AHello", vim.getContent());
            assertEquals(1, vim.getCursor());
            vim.undo(); // undo insertCurrent → "Hello", cursor=0
            assertEquals("Hello", vim.getContent());
            assertEquals(0, vim.getCursor());

            // Redo all 6 forward
            vim.redo(); // "AHello", cursor=1
            assertEquals("AHello", vim.getContent());
            vim.redo(); // "Aello", cursor=1
            assertEquals("Aello", vim.getContent());
            vim.redo(); // "Allo", cursor=1
            assertEquals("Allo", vim.getContent());
            vim.redo(); // "AXYllo", cursor=3
            assertEquals("AXYllo", vim.getContent());
            vim.redo(); // "AXYZlo", cursor=3
            assertEquals("AXYZlo", vim.getContent());
            vim.redo(); // "QWYZlo", cursor=3
            assertEquals("QWYZlo", vim.getContent());
            assertEquals(3, vim.getCursor());
        }
    }
}
