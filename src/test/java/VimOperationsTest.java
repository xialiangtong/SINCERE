import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VimOperations Tests")
public class VimOperationsTest {

    // ==================== Constructor & Query Tests ====================

    @Nested
    @DisplayName("Constructor & Query Tests")
    class ConstructorTests {

        @Test
        @DisplayName("default constructor creates empty content with cursor at 0")
        void testDefaultConstructor() {
            VimOperations vim = new VimOperations();
            assertEquals("", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("constructor with initial text places cursor at tail")
        void testConstructorWithText() {
            VimOperations vim = new VimOperations("hello");
            assertEquals("hello", vim.getContent());
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("constructor with empty string")
        void testConstructorWithEmptyString() {
            VimOperations vim = new VimOperations("");
            assertEquals("", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("constructor with single character")
        void testConstructorWithSingleChar() {
            VimOperations vim = new VimOperations("x");
            assertEquals("x", vim.getContent());
            assertEquals(1, vim.getCursor());
        }
    }

    // ==================== Cursor Movement Tests ====================

    @Nested
    @DisplayName("Cursor Movement Tests")
    class CursorMovementTests {

        @Test
        @DisplayName("moveForward advances cursor by one")
        void testMoveForward() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.moveForward();
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("moveForward at tail stays at tail (boundary)")
        void testMoveForwardAtTail() {
            VimOperations vim = new VimOperations("hello");
            vim.moveForward(); // already at 5
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("moveForward on empty content stays at 0")
        void testMoveForwardEmpty() {
            VimOperations vim = new VimOperations();
            vim.moveForward();
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("moveBackward moves cursor back by one")
        void testMoveBackward() {
            VimOperations vim = new VimOperations("hello");
            vim.moveBackward();
            assertEquals(4, vim.getCursor());
        }

        @Test
        @DisplayName("moveBackward at head stays at 0 (boundary)")
        void testMoveBackwardAtHead() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.moveBackward();
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("moveBackward on empty content stays at 0")
        void testMoveBackwardEmpty() {
            VimOperations vim = new VimOperations();
            vim.moveBackward();
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("moveToHead moves cursor to 0")
        void testMoveToHead() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("moveToHead on empty content")
        void testMoveToHeadEmpty() {
            VimOperations vim = new VimOperations();
            vim.moveToHead();
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("moveToTail moves cursor to end of content")
        void testMoveToTail() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.moveToTail();
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("moveToTail on empty content")
        void testMoveToTailEmpty() {
            VimOperations vim = new VimOperations();
            vim.moveToTail();
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("multiple forward moves stop at tail")
        void testMultipleForwardStopsAtTail() {
            VimOperations vim = new VimOperations("ab");
            vim.moveToHead();
            vim.moveForward();
            vim.moveForward();
            vim.moveForward(); // should clamp
            vim.moveForward(); // should clamp
            assertEquals(2, vim.getCursor());
        }

        @Test
        @DisplayName("multiple backward moves stop at head")
        void testMultipleBackwardStopsAtHead() {
            VimOperations vim = new VimOperations("ab");
            vim.moveBackward();
            vim.moveBackward();
            vim.moveBackward(); // should clamp
            vim.moveBackward(); // should clamp
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("forward then backward returns to same position")
        void testForwardThenBackward() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.moveForward();
            vim.moveForward();
            int pos = vim.getCursor();
            vim.moveBackward();
            vim.moveForward();
            assertEquals(pos, vim.getCursor());
        }
    }

    // ==================== InsertCurrent Tests ====================

    @Nested
    @DisplayName("InsertCurrent Tests")
    class InsertCurrentTests {

        @Test
        @DisplayName("insert char at tail")
        void testInsertAtTail() {
            VimOperations vim = new VimOperations("hello");
            vim.insertCurrent('!');
            assertEquals("hello!", vim.getContent());
            assertEquals(6, vim.getCursor());
        }

        @Test
        @DisplayName("insert char at head")
        void testInsertAtHead() {
            VimOperations vim = new VimOperations("ello");
            vim.moveToHead();
            vim.insertCurrent('h');
            assertEquals("hello", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("insert char in middle")
        void testInsertInMiddle() {
            VimOperations vim = new VimOperations("hllo");
            vim.moveToHead();
            vim.moveForward();
            vim.insertCurrent('e');
            assertEquals("hello", vim.getContent());
            assertEquals(2, vim.getCursor());
        }

        @Test
        @DisplayName("insert into empty content")
        void testInsertIntoEmpty() {
            VimOperations vim = new VimOperations();
            vim.insertCurrent('a');
            assertEquals("a", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("multiple sequential inserts at tail")
        void testMultipleInserts() {
            VimOperations vim = new VimOperations();
            vim.insertCurrent('a');
            vim.insertCurrent('b');
            vim.insertCurrent('c');
            assertEquals("abc", vim.getContent());
            assertEquals(3, vim.getCursor());
        }
    }

    // ==================== InsertRange Tests ====================

    @Nested
    @DisplayName("InsertRange Tests")
    class InsertRangeTests {

        @Test
        @DisplayName("insertRange at tail")
        void testInsertRangeAtTail() {
            VimOperations vim = new VimOperations("hello");
            vim.insertRange(5, 5, " world");
            assertEquals("hello world", vim.getContent());
        }

        @Test
        @DisplayName("insertRange at head")
        void testInsertRangeAtHead() {
            VimOperations vim = new VimOperations("world");
            vim.insertRange(0, 0, "hello ");
            assertEquals("hello world", vim.getContent());
        }

        @Test
        @DisplayName("insertRange in middle")
        void testInsertRangeInMiddle() {
            VimOperations vim = new VimOperations("hd");
            vim.insertRange(1, 1, "ello worl");
            assertEquals("hello world", vim.getContent());
        }

        @Test
        @DisplayName("insertRange into empty content")
        void testInsertRangeIntoEmpty() {
            VimOperations vim = new VimOperations();
            vim.insertRange(0, 0, "hello");
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("insertRange with start beyond tail (boundary)")
        void testInsertRangeStartBeyondTail() {
            VimOperations vim = new VimOperations("abc");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.insertRange(10, 10, "x"));
        }

        @Test
        @DisplayName("insertRange with negative start (boundary)")
        void testInsertRangeNegativeStart() {
            VimOperations vim = new VimOperations("abc");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.insertRange(-1, -1, "x"));
        }

        @Test
        @DisplayName("insertRange with start > end (boundary)")
        void testInsertRangeStartGreaterThanEnd() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IllegalArgumentException.class,
                    () -> vim.insertRange(3, 1, "x"));
        }
    }

    // ==================== DeleteBackward Tests ====================

    @Nested
    @DisplayName("DeleteBackward Tests")
    class DeleteBackwardTests {

        @Test
        @DisplayName("deleteBackward removes char before cursor")
        void testDeleteBackward() {
            VimOperations vim = new VimOperations("hello");
            char deleted = vim.deleteBackward();
            assertEquals('o', deleted);
            assertEquals("hell", vim.getContent());
            assertEquals(4, vim.getCursor());
        }

        @Test
        @DisplayName("deleteBackward at head returns null char (boundary)")
        void testDeleteBackwardAtHead() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            char deleted = vim.deleteBackward();
            assertEquals('\0', deleted);
            assertEquals("hello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("deleteBackward on empty content (boundary)")
        void testDeleteBackwardEmpty() {
            VimOperations vim = new VimOperations();
            char deleted = vim.deleteBackward();
            assertEquals('\0', deleted);
            assertEquals("", vim.getContent());
        }

        @Test
        @DisplayName("deleteBackward in middle")
        void testDeleteBackwardMiddle() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.moveForward();
            vim.moveForward();
            char deleted = vim.deleteBackward();
            assertEquals('e', deleted);
            assertEquals("hllo", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("deleteBackward all chars one by one")
        void testDeleteBackwardAll() {
            VimOperations vim = new VimOperations("abc");
            vim.deleteBackward(); // c
            vim.deleteBackward(); // b
            vim.deleteBackward(); // a
            assertEquals("", vim.getContent());
            assertEquals(0, vim.getCursor());
            assertEquals('\0', vim.deleteBackward()); // nothing left
        }
    }

    // ==================== DeleteCurrent Tests ====================

    @Nested
    @DisplayName("DeleteCurrent Tests")
    class DeleteCurrentTests {

        @Test
        @DisplayName("deleteCurrent removes char at cursor")
        void testDeleteCurrent() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            char deleted = vim.deleteCurrent();
            assertEquals('h', deleted);
            assertEquals("ello", vim.getContent());
            assertEquals(0, vim.getCursor());
        }

        @Test
        @DisplayName("deleteCurrent at tail returns null char (boundary)")
        void testDeleteCurrentAtTail() {
            VimOperations vim = new VimOperations("hello");
            char deleted = vim.deleteCurrent();
            assertEquals('\0', deleted);
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("deleteCurrent on empty content (boundary)")
        void testDeleteCurrentEmpty() {
            VimOperations vim = new VimOperations();
            char deleted = vim.deleteCurrent();
            assertEquals('\0', deleted);
        }

        @Test
        @DisplayName("deleteCurrent in middle")
        void testDeleteCurrentMiddle() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.moveForward();
            vim.moveForward();
            char deleted = vim.deleteCurrent();
            assertEquals('l', deleted);
            assertEquals("helo", vim.getContent());
            assertEquals(2, vim.getCursor());
        }

        @Test
        @DisplayName("deleteCurrent on last remaining char")
        void testDeleteCurrentLastChar() {
            VimOperations vim = new VimOperations("x");
            vim.moveToHead();
            char deleted = vim.deleteCurrent();
            assertEquals('x', deleted);
            assertEquals("", vim.getContent());
            assertEquals(0, vim.getCursor());
        }
    }

    // ==================== DeleteRange Tests ====================

    @Nested
    @DisplayName("DeleteRange Tests")
    class DeleteRangeTests {

        @Test
        @DisplayName("deleteRange removes specified range")
        void testDeleteRange() {
            VimOperations vim = new VimOperations("hello world");
            String deleted = vim.deleteRange(5, 11);
            assertEquals(" world", deleted);
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("deleteRange from head")
        void testDeleteRangeFromHead() {
            VimOperations vim = new VimOperations("hello world");
            String deleted = vim.deleteRange(0, 6);
            assertEquals("hello ", deleted);
            assertEquals("world", vim.getContent());
        }

        @Test
        @DisplayName("deleteRange entire content")
        void testDeleteRangeAll() {
            VimOperations vim = new VimOperations("hello");
            String deleted = vim.deleteRange(0, 5);
            assertEquals("hello", deleted);
            assertEquals("", vim.getContent());
        }

        @Test
        @DisplayName("deleteRange with start == end returns empty")
        void testDeleteRangeSameStartEnd() {
            VimOperations vim = new VimOperations("hello");
            String deleted = vim.deleteRange(2, 2);
            assertEquals("", deleted);
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("deleteRange with start > end (boundary)")
        void testDeleteRangeStartGreaterThanEnd() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IllegalArgumentException.class,
                    () -> vim.deleteRange(4, 2));
        }

        @Test
        @DisplayName("deleteRange with negative start (boundary)")
        void testDeleteRangeNegativeStart() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.deleteRange(-1, 3));
        }

        @Test
        @DisplayName("deleteRange with end beyond tail (boundary)")
        void testDeleteRangeEndBeyondTail() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.deleteRange(2, 100));
        }

        @Test
        @DisplayName("deleteRange on empty content (boundary)")
        void testDeleteRangeEmpty() {
            VimOperations vim = new VimOperations();
            String deleted = vim.deleteRange(0, 0);
            assertEquals("", deleted);
        }
    }

    // ==================== DeletePosition Tests ====================

    @Nested
    @DisplayName("DeletePosition Tests")
    class DeletePositionTests {

        @Test
        @DisplayName("deletePosition removes char at given index")
        void testDeletePosition() {
            VimOperations vim = new VimOperations("hello");
            char deleted = vim.deletePosition(0);
            assertEquals('h', deleted);
            assertEquals("ello", vim.getContent());
        }

        @Test
        @DisplayName("deletePosition at last index")
        void testDeletePositionLast() {
            VimOperations vim = new VimOperations("hello");
            char deleted = vim.deletePosition(4);
            assertEquals('o', deleted);
            assertEquals("hell", vim.getContent());
        }

        @Test
        @DisplayName("deletePosition in middle")
        void testDeletePositionMiddle() {
            VimOperations vim = new VimOperations("hello");
            char deleted = vim.deletePosition(2);
            assertEquals('l', deleted);
            assertEquals("helo", vim.getContent());
        }

        @Test
        @DisplayName("deletePosition with negative index (boundary)")
        void testDeletePositionNegative() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.deletePosition(-1));
        }

        @Test
        @DisplayName("deletePosition beyond tail (boundary)")
        void testDeletePositionBeyondTail() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.deletePosition(5));
        }

        @Test
        @DisplayName("deletePosition on empty content (boundary)")
        void testDeletePositionEmpty() {
            VimOperations vim = new VimOperations();
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.deletePosition(0));
        }

        @Test
        @DisplayName("deletePosition on single char content")
        void testDeletePositionSingleChar() {
            VimOperations vim = new VimOperations("x");
            char deleted = vim.deletePosition(0);
            assertEquals('x', deleted);
            assertEquals("", vim.getContent());
        }
    }

    // ==================== ReplaceCurrent Tests ====================

    @Nested
    @DisplayName("ReplaceCurrent Tests")
    class ReplaceCurrentTests {

        @Test
        @DisplayName("replaceCurrent replaces char at cursor")
        void testReplaceCurrent() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            char old = vim.replaceCurrent('H');
            assertEquals('h', old);
            assertEquals("Hello", vim.getContent());
        }

        @Test
        @DisplayName("replaceCurrent at last position")
        void testReplaceCurrentLast() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.moveForward();
            vim.moveForward();
            vim.moveForward();
            vim.moveForward();
            char old = vim.replaceCurrent('O');
            assertEquals('o', old);
            assertEquals("hellO", vim.getContent());
        }

        @Test
        @DisplayName("replaceCurrent with same char")
        void testReplaceCurrentSameChar() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            char old = vim.replaceCurrent('h');
            assertEquals('h', old);
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("replaceCurrent at tail position (boundary)")
        void testReplaceCurrentAtTail() {
            VimOperations vim = new VimOperations("hello");
            // cursor is at 5, beyond last char
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.replaceCurrent('x'));
        }

        @Test
        @DisplayName("replaceCurrent on empty content (boundary)")
        void testReplaceCurrentEmpty() {
            VimOperations vim = new VimOperations();
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.replaceCurrent('x'));
        }
    }

    // ==================== ReplaceRange Tests ====================

    @Nested
    @DisplayName("ReplaceRange Tests")
    class ReplaceRangeTests {

        @Test
        @DisplayName("replaceRange replaces text in range")
        void testReplaceRange() {
            VimOperations vim = new VimOperations("hello world");
            String old = vim.replaceRange(0, 5, "HELLO");
            assertEquals("hello", old);
            assertEquals("HELLO world", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange with shorter replacement")
        void testReplaceRangeShorter() {
            VimOperations vim = new VimOperations("hello world");
            String old = vim.replaceRange(0, 5, "hi");
            assertEquals("hello", old);
            assertEquals("hi world", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange with longer replacement")
        void testReplaceRangeLonger() {
            VimOperations vim = new VimOperations("hi world");
            String old = vim.replaceRange(0, 2, "hello");
            assertEquals("hi", old);
            assertEquals("hello world", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange with empty replacement (acts as delete)")
        void testReplaceRangeWithEmpty() {
            VimOperations vim = new VimOperations("hello world");
            String old = vim.replaceRange(5, 11, "");
            assertEquals(" world", old);
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange with empty range (acts as insert)")
        void testReplaceRangeEmptyRange() {
            VimOperations vim = new VimOperations("helloworld");
            String old = vim.replaceRange(5, 5, " ");
            assertEquals("", old);
            assertEquals("hello world", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange entire content")
        void testReplaceRangeAll() {
            VimOperations vim = new VimOperations("old");
            String old = vim.replaceRange(0, 3, "new content");
            assertEquals("old", old);
            assertEquals("new content", vim.getContent());
        }

        @Test
        @DisplayName("replaceRange with start > end (boundary)")
        void testReplaceRangeStartGreaterThanEnd() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IllegalArgumentException.class,
                    () -> vim.replaceRange(5, 2, "x"));
        }

        @Test
        @DisplayName("replaceRange with negative start (boundary)")
        void testReplaceRangeNegativeStart() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.replaceRange(-1, 3, "x"));
        }

        @Test
        @DisplayName("replaceRange with end beyond tail (boundary)")
        void testReplaceRangeEndBeyondTail() {
            VimOperations vim = new VimOperations("hello");
            assertThrows(IndexOutOfBoundsException.class,
                    () -> vim.replaceRange(2, 100, "x"));
        }

        @Test
        @DisplayName("replaceRange on empty content with 0,0 range")
        void testReplaceRangeEmptyContent() {
            VimOperations vim = new VimOperations();
            String old = vim.replaceRange(0, 0, "hello");
            assertEquals("", old);
            assertEquals("hello", vim.getContent());
        }
    }

    // ==================== Undo Tests ====================

    @Nested
    @DisplayName("Undo Tests")
    class UndoTests {

        @Test
        @DisplayName("undo on fresh editor returns false")
        void testUndoEmpty() {
            VimOperations vim = new VimOperations("hello");
            assertFalse(vim.undo());
        }

        @Test
        @DisplayName("undo insertCurrent restores content and cursor")
        void testUndoInsertCurrent() {
            VimOperations vim = new VimOperations("hello");
            vim.insertCurrent('!');
            assertEquals("hello!", vim.getContent());
            vim.undo();
            assertEquals("hello", vim.getContent());
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("undo insertRange restores content")
        void testUndoInsertRange() {
            VimOperations vim = new VimOperations("hello");
            vim.insertRange(5, 5, " world");
            assertEquals("hello world", vim.getContent());
            vim.undo();
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("undo deleteBackward restores deleted char")
        void testUndoDeleteBackward() {
            VimOperations vim = new VimOperations("hello");
            vim.deleteBackward();
            assertEquals("hell", vim.getContent());
            vim.undo();
            assertEquals("hello", vim.getContent());
            assertEquals(5, vim.getCursor());
        }

        @Test
        @DisplayName("undo deleteCurrent restores deleted char")
        void testUndoDeleteCurrent() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.deleteCurrent();
            assertEquals("ello", vim.getContent());
            vim.undo();
            assertEquals("hello", vim.getContent());
            assertEquals(1, vim.getCursor());
        }

        @Test
        @DisplayName("undo deleteRange restores deleted range")
        void testUndoDeleteRange() {
            VimOperations vim = new VimOperations("hello world");
            vim.deleteRange(5, 11);
            assertEquals("hello", vim.getContent());
            vim.undo();
            assertEquals("hello world", vim.getContent());
        }

        @Test
        @DisplayName("undo deletePosition restores deleted char")
        void testUndoDeletePosition() {
            VimOperations vim = new VimOperations("hello");
            vim.deletePosition(2);
            assertEquals("helo", vim.getContent());
            vim.undo();
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("undo replaceCurrent restores original char")
        void testUndoReplaceCurrent() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.replaceCurrent('H');
            assertEquals("Hello", vim.getContent());
            vim.undo();
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("undo replaceRange restores original text")
        void testUndoReplaceRange() {
            VimOperations vim = new VimOperations("hello world");
            vim.replaceRange(0, 5, "HELLO");
            assertEquals("HELLO world", vim.getContent());
            vim.undo();
            assertEquals("hello world", vim.getContent());
        }

        @Test
        @DisplayName("multiple undos in sequence")
        void testMultipleUndos() {
            VimOperations vim = new VimOperations();
            vim.insertCurrent('a');
            vim.insertCurrent('b');
            vim.insertCurrent('c');
            assertEquals("abc", vim.getContent());

            vim.undo();
            assertEquals("ab", vim.getContent());
            vim.undo();
            assertEquals("a", vim.getContent());
            vim.undo();
            assertEquals("", vim.getContent());
            assertFalse(vim.undo()); // nothing left
        }
    }

    // ==================== Redo Tests ====================

    @Nested
    @DisplayName("Redo Tests")
    class RedoTests {

        @Test
        @DisplayName("redo on fresh editor returns false")
        void testRedoEmpty() {
            VimOperations vim = new VimOperations("hello");
            assertFalse(vim.redo());
        }

        @Test
        @DisplayName("redo after undo restores the operation")
        void testRedoAfterUndo() {
            VimOperations vim = new VimOperations("hello");
            vim.insertCurrent('!');
            vim.undo();
            assertEquals("hello", vim.getContent());
            vim.redo();
            assertEquals("hello!", vim.getContent());
            assertEquals(6, vim.getCursor());
        }

        @Test
        @DisplayName("redo deleteBackward")
        void testRedoDeleteBackward() {
            VimOperations vim = new VimOperations("hello");
            vim.deleteBackward();
            vim.undo();
            assertEquals("hello", vim.getContent());
            vim.redo();
            assertEquals("hell", vim.getContent());
        }

        @Test
        @DisplayName("redo replaceCurrent")
        void testRedoReplaceCurrent() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.replaceCurrent('H');
            vim.undo();
            vim.redo();
            assertEquals("Hello", vim.getContent());
        }

        @Test
        @DisplayName("new edit clears redo stack")
        void testNewEditClearsRedo() {
            VimOperations vim = new VimOperations("hello");
            vim.insertCurrent('!');
            vim.undo();
            vim.insertCurrent('?'); // new edit clears redo
            assertFalse(vim.redo());
            assertEquals("hello?", vim.getContent());
        }

        @Test
        @DisplayName("multiple undo then redo")
        void testMultipleUndoRedo() {
            VimOperations vim = new VimOperations();
            vim.insertCurrent('a');
            vim.insertCurrent('b');
            vim.insertCurrent('c');

            vim.undo(); // remove c
            vim.undo(); // remove b
            assertEquals("a", vim.getContent());

            vim.redo(); // re-add b
            assertEquals("ab", vim.getContent());
            vim.redo(); // re-add c
            assertEquals("abc", vim.getContent());
            assertFalse(vim.redo()); // nothing left
        }
    }

    // ==================== Undo/Redo Interaction Tests ====================

    @Nested
    @DisplayName("Undo/Redo Interaction Tests")
    class UndoRedoInteractionTests {

        @Test
        @DisplayName("undo-redo-undo cycle")
        void testUndoRedoUndoCycle() {
            VimOperations vim = new VimOperations("hello");
            vim.insertCurrent('!');
            assertEquals("hello!", vim.getContent());

            vim.undo();
            assertEquals("hello", vim.getContent());

            vim.redo();
            assertEquals("hello!", vim.getContent());

            vim.undo();
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("interleaved operations with undo")
        void testInterleavedOps() {
            VimOperations vim = new VimOperations("hello");
            vim.moveToHead();
            vim.replaceCurrent('H');    // Hello, cursor=0
            vim.moveToTail();           // cursor=5
            vim.insertCurrent('!');     // Hello!, cursor=6
            vim.moveBackward();         // cursor=5
            vim.deleteBackward();       // delete 'o' at index 4 → Hell!, cursor=4

            assertEquals("Hell!", vim.getContent());

            vim.undo(); // restore 'o' → Hello!
            assertEquals("Hello!", vim.getContent());

            vim.undo(); // remove '!' → Hello
            assertEquals("Hello", vim.getContent());

            vim.undo(); // restore 'h' → hello
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("redo is cleared after any new edit")
        void testRedoClearedAfterEdit() {
            VimOperations vim = new VimOperations("abc");
            vim.insertCurrent('d');      // abcd
            vim.undo();                  // abc, redo has 'd'

            vim.deleteBackward();        // ab, redo cleared
            assertFalse(vim.redo());
            assertEquals("ab", vim.getContent());
        }

        @Test
        @DisplayName("undo all then redo all")
        void testUndoAllRedoAll() {
            VimOperations vim = new VimOperations();
            vim.insertCurrent('a');
            vim.insertCurrent('b');
            vim.insertCurrent('c');
            vim.moveToHead();
            vim.replaceCurrent('A');
            vim.moveToTail();
            vim.deleteBackward();

            // Undo everything
            while (vim.undo()) {}
            assertEquals("", vim.getContent());

            // Redo everything
            while (vim.redo()) {}
            assertEquals("Ab", vim.getContent());
        }

        @Test
        @DisplayName("undo deleteRange then redo")
        void testUndoRedoDeleteRange() {
            VimOperations vim = new VimOperations("hello world");
            vim.deleteRange(5, 11);
            assertEquals("hello", vim.getContent());

            vim.undo();
            assertEquals("hello world", vim.getContent());

            vim.redo();
            assertEquals("hello", vim.getContent());
        }

        @Test
        @DisplayName("undo replaceRange then redo")
        void testUndoRedoReplaceRange() {
            VimOperations vim = new VimOperations("hello world");
            vim.replaceRange(0, 5, "HI");
            assertEquals("HI world", vim.getContent());

            vim.undo();
            assertEquals("hello world", vim.getContent());

            vim.redo();
            assertEquals("HI world", vim.getContent());
        }
    }
}
