import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VimEditor Tests")
public class VimEditorTest {

    // ==================== Constructor & Query Tests ====================

    @Nested
    @DisplayName("Constructor & Query Tests")
    class ConstructorTests {

        @Test
        @DisplayName("default constructor creates empty editor")
        void testDefaultConstructor() {
            VimEditor editor = new VimEditor();
            assertEquals("", editor.getText());
            assertEquals(0, editor.getCursor());
            assertEquals(0, editor.length());
        }

        @Test
        @DisplayName("constructor with initial text")
        void testInitialText() {
            VimEditor editor = new VimEditor("hello");
            assertEquals("hello", editor.getText());
            assertEquals(5, editor.getCursor()); // cursor at end
            assertEquals(5, editor.length());
        }

        @Test
        @DisplayName("charAt returns correct character")
        void testCharAt() {
            VimEditor editor = new VimEditor("abc");
            assertEquals('a', editor.charAt(0));
            assertEquals('b', editor.charAt(1));
            assertEquals('c', editor.charAt(2));
        }

        @Test
        @DisplayName("charAt throws on out of bounds")
        void testCharAtOutOfBounds() {
            VimEditor editor = new VimEditor("abc");
            assertThrows(IndexOutOfBoundsException.class, () -> editor.charAt(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> editor.charAt(3));
        }

        @Test
        @DisplayName("charAt throws on empty editor")
        void testCharAtEmpty() {
            VimEditor editor = new VimEditor();
            assertThrows(IndexOutOfBoundsException.class, () -> editor.charAt(0));
        }
    }

    // ==================== Cursor Movement Tests ====================

    @Nested
    @DisplayName("Cursor Movement Tests")
    class CursorMovementTests {

        @Test
        @DisplayName("moveLeft moves cursor left by one")
        void testMoveLeft() {
            VimEditor editor = new VimEditor("hello");
            editor.moveLeft();
            assertEquals(4, editor.getCursor());
        }

        @Test
        @DisplayName("moveLeft at position 0 stays at 0")
        void testMoveLeftAtStart() {
            VimEditor editor = new VimEditor("hello");
            editor.moveToStart();
            editor.moveLeft();
            assertEquals(0, editor.getCursor());
        }

        @Test
        @DisplayName("moveRight moves cursor right by one")
        void testMoveRight() {
            VimEditor editor = new VimEditor("hello");
            editor.moveToStart();
            editor.moveRight();
            assertEquals(1, editor.getCursor());
        }

        @Test
        @DisplayName("moveRight at end stays at end")
        void testMoveRightAtEnd() {
            VimEditor editor = new VimEditor("hello");
            editor.moveRight();
            assertEquals(5, editor.getCursor());
        }

        @Test
        @DisplayName("moveToStart moves cursor to 0")
        void testMoveToStart() {
            VimEditor editor = new VimEditor("hello");
            editor.moveToStart();
            assertEquals(0, editor.getCursor());
        }

        @Test
        @DisplayName("moveToEnd moves cursor to length")
        void testMoveToEnd() {
            VimEditor editor = new VimEditor("hello");
            editor.moveToStart();
            editor.moveToEnd();
            assertEquals(5, editor.getCursor());
        }

        @Test
        @DisplayName("moveTo clamps to valid range")
        void testMoveToClamps() {
            VimEditor editor = new VimEditor("hello");
            editor.moveTo(-5);
            assertEquals(0, editor.getCursor());
            editor.moveTo(100);
            assertEquals(5, editor.getCursor());
        }

        @Test
        @DisplayName("moveTo specific position")
        void testMoveToMiddle() {
            VimEditor editor = new VimEditor("hello");
            editor.moveTo(3);
            assertEquals(3, editor.getCursor());
        }

        @Test
        @DisplayName("cursor movement on empty editor")
        void testCursorOnEmpty() {
            VimEditor editor = new VimEditor();
            editor.moveLeft();
            assertEquals(0, editor.getCursor());
            editor.moveRight();
            assertEquals(0, editor.getCursor());
            editor.moveToStart();
            assertEquals(0, editor.getCursor());
            editor.moveToEnd();
            assertEquals(0, editor.getCursor());
        }
    }

    // ==================== Insert Tests ====================

    @Nested
    @DisplayName("Insert Tests")
    class InsertTests {

        @Test
        @DisplayName("insert text at end")
        void testInsertAtEnd() {
            VimEditor editor = new VimEditor("hello");
            editor.insert(" world");
            assertEquals("hello world", editor.getText());
            assertEquals(11, editor.getCursor());
        }

        @Test
        @DisplayName("insert text at beginning")
        void testInsertAtBeginning() {
            VimEditor editor = new VimEditor("world");
            editor.moveToStart();
            editor.insert("hello ");
            assertEquals("hello world", editor.getText());
            assertEquals(6, editor.getCursor());
        }

        @Test
        @DisplayName("insert text in middle")
        void testInsertInMiddle() {
            VimEditor editor = new VimEditor("helo");
            editor.moveTo(3);
            editor.insert("l");
            assertEquals("hello", editor.getText());
            assertEquals(4, editor.getCursor());
        }

        @Test
        @DisplayName("insert single character")
        void testInsertChar() {
            VimEditor editor = new VimEditor("ab");
            editor.moveTo(1);
            editor.insert('X');
            assertEquals("aXb", editor.getText());
            assertEquals(2, editor.getCursor());
        }

        @Test
        @DisplayName("insert into empty editor")
        void testInsertIntoEmpty() {
            VimEditor editor = new VimEditor();
            editor.insert("hello");
            assertEquals("hello", editor.getText());
            assertEquals(5, editor.getCursor());
        }

        @Test
        @DisplayName("insert null does nothing")
        void testInsertNull() {
            VimEditor editor = new VimEditor("hello");
            editor.insert((String) null);
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("insert empty string does nothing")
        void testInsertEmpty() {
            VimEditor editor = new VimEditor("hello");
            editor.insert("");
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("multiple sequential inserts")
        void testMultipleInserts() {
            VimEditor editor = new VimEditor();
            editor.insert("a");
            editor.insert("b");
            editor.insert("c");
            assertEquals("abc", editor.getText());
            assertEquals(3, editor.getCursor());
        }
    }

    // ==================== Delete Back (Backspace) Tests ====================

    @Nested
    @DisplayName("DeleteBack Tests")
    class DeleteBackTests {

        @Test
        @DisplayName("deleteBack removes character before cursor")
        void testDeleteBack() {
            VimEditor editor = new VimEditor("hello");
            char deleted = editor.deleteBack();
            assertEquals('o', deleted);
            assertEquals("hell", editor.getText());
            assertEquals(4, editor.getCursor());
        }

        @Test
        @DisplayName("deleteBack at position 0 returns null char")
        void testDeleteBackAtStart() {
            VimEditor editor = new VimEditor("hello");
            editor.moveToStart();
            char deleted = editor.deleteBack();
            assertEquals('\0', deleted);
            assertEquals("hello", editor.getText());
            assertEquals(0, editor.getCursor());
        }

        @Test
        @DisplayName("deleteBack in middle")
        void testDeleteBackInMiddle() {
            VimEditor editor = new VimEditor("hello");
            editor.moveTo(3);
            char deleted = editor.deleteBack();
            assertEquals('l', deleted);
            assertEquals("helo", editor.getText());
            assertEquals(2, editor.getCursor());
        }

        @Test
        @DisplayName("deleteBack on empty editor")
        void testDeleteBackEmpty() {
            VimEditor editor = new VimEditor();
            char deleted = editor.deleteBack();
            assertEquals('\0', deleted);
            assertEquals("", editor.getText());
        }

        @Test
        @DisplayName("deleteBack all characters one by one")
        void testDeleteBackAll() {
            VimEditor editor = new VimEditor("abc");
            editor.deleteBack(); // c
            editor.deleteBack(); // b
            editor.deleteBack(); // a
            assertEquals("", editor.getText());
            assertEquals(0, editor.getCursor());
            assertEquals('\0', editor.deleteBack()); // nothing left
        }
    }

    // ==================== Delete At (Vim 'x') Tests ====================

    @Nested
    @DisplayName("DeleteAt Tests")
    class DeleteAtTests {

        @Test
        @DisplayName("deleteAt removes character at cursor")
        void testDeleteAt() {
            VimEditor editor = new VimEditor("hello");
            editor.moveToStart();
            char deleted = editor.deleteAt();
            assertEquals('h', deleted);
            assertEquals("ello", editor.getText());
            assertEquals(0, editor.getCursor());
        }

        @Test
        @DisplayName("deleteAt at end returns null char")
        void testDeleteAtEnd() {
            VimEditor editor = new VimEditor("hello");
            char deleted = editor.deleteAt();
            assertEquals('\0', deleted);
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("deleteAt in middle")
        void testDeleteAtMiddle() {
            VimEditor editor = new VimEditor("hello");
            editor.moveTo(2);
            char deleted = editor.deleteAt();
            assertEquals('l', deleted);
            assertEquals("helo", editor.getText());
            assertEquals(2, editor.getCursor());
        }

        @Test
        @DisplayName("deleteAt last character clamps cursor")
        void testDeleteAtLastChar() {
            VimEditor editor = new VimEditor("a");
            editor.moveToStart();
            char deleted = editor.deleteAt();
            assertEquals('a', deleted);
            assertEquals("", editor.getText());
            assertEquals(0, editor.getCursor());
        }

        @Test
        @DisplayName("deleteAt on empty editor")
        void testDeleteAtEmpty() {
            VimEditor editor = new VimEditor();
            char deleted = editor.deleteAt();
            assertEquals('\0', deleted);
        }
    }

    // ==================== Delete Range Tests ====================

    @Nested
    @DisplayName("DeleteRange Tests")
    class DeleteRangeTests {

        @Test
        @DisplayName("deleteRange removes specified range")
        void testDeleteRange() {
            VimEditor editor = new VimEditor("hello world");
            String deleted = editor.deleteRange(5, 11);
            assertEquals(" world", deleted);
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("deleteRange from beginning")
        void testDeleteRangeFromStart() {
            VimEditor editor = new VimEditor("hello world");
            String deleted = editor.deleteRange(0, 6);
            assertEquals("hello ", deleted);
            assertEquals("world", editor.getText());
            assertEquals(0, editor.getCursor());
        }

        @Test
        @DisplayName("deleteRange clamps to valid bounds")
        void testDeleteRangeClamps() {
            VimEditor editor = new VimEditor("hello");
            String deleted = editor.deleteRange(-5, 100);
            assertEquals("hello", deleted);
            assertEquals("", editor.getText());
        }

        @Test
        @DisplayName("deleteRange with start >= end returns empty")
        void testDeleteRangeInvalid() {
            VimEditor editor = new VimEditor("hello");
            String deleted = editor.deleteRange(3, 2);
            assertEquals("", deleted);
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("deleteRange on empty editor")
        void testDeleteRangeEmpty() {
            VimEditor editor = new VimEditor();
            String deleted = editor.deleteRange(0, 5);
            assertEquals("", deleted);
            assertEquals("", editor.getText());
        }
    }

    // ==================== Replace Char Tests ====================

    @Nested
    @DisplayName("ReplaceChar Tests")
    class ReplaceCharTests {

        @Test
        @DisplayName("replaceChar replaces single character")
        void testReplaceChar() {
            VimEditor editor = new VimEditor("hello");
            editor.replaceChar(0, 'H');
            assertEquals("Hello", editor.getText());
        }

        @Test
        @DisplayName("replaceChar at last position")
        void testReplaceCharLast() {
            VimEditor editor = new VimEditor("hello");
            editor.replaceChar(4, 'O');
            assertEquals("hellO", editor.getText());
        }

        @Test
        @DisplayName("replaceChar with same character")
        void testReplaceCharSame() {
            VimEditor editor = new VimEditor("hello");
            editor.replaceChar(0, 'h');
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("replaceChar throws on out of bounds")
        void testReplaceCharOutOfBounds() {
            VimEditor editor = new VimEditor("hello");
            assertThrows(IndexOutOfBoundsException.class, () -> editor.replaceChar(-1, 'x'));
            assertThrows(IndexOutOfBoundsException.class, () -> editor.replaceChar(5, 'x'));
        }

        @Test
        @DisplayName("replaceChar on empty editor throws")
        void testReplaceCharEmpty() {
            VimEditor editor = new VimEditor();
            assertThrows(IndexOutOfBoundsException.class, () -> editor.replaceChar(0, 'x'));
        }
    }

    // ==================== Replace Range Tests ====================

    @Nested
    @DisplayName("ReplaceRange Tests")
    class ReplaceRangeTests {

        @Test
        @DisplayName("replaceRange replaces text in range")
        void testReplaceRange() {
            VimEditor editor = new VimEditor("hello world");
            editor.replaceRange(0, 5, "HELLO");
            assertEquals("HELLO world", editor.getText());
            assertEquals(5, editor.getCursor());
        }

        @Test
        @DisplayName("replaceRange with shorter replacement")
        void testReplaceRangeShorter() {
            VimEditor editor = new VimEditor("hello world");
            editor.replaceRange(0, 5, "hi");
            assertEquals("hi world", editor.getText());
        }

        @Test
        @DisplayName("replaceRange with longer replacement")
        void testReplaceRangeLonger() {
            VimEditor editor = new VimEditor("hi world");
            editor.replaceRange(0, 2, "hello");
            assertEquals("hello world", editor.getText());
        }

        @Test
        @DisplayName("replaceRange with empty replacement (acts as delete)")
        void testReplaceRangeWithEmpty() {
            VimEditor editor = new VimEditor("hello world");
            editor.replaceRange(5, 11, "");
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("replaceRange with empty range (acts as insert)")
        void testReplaceRangeEmptyRange() {
            VimEditor editor = new VimEditor("helloworld");
            editor.replaceRange(5, 5, " ");
            assertEquals("hello world", editor.getText());
        }

        @Test
        @DisplayName("replaceRange throws when start > end")
        void testReplaceRangeInvalidRange() {
            VimEditor editor = new VimEditor("hello");
            assertThrows(IllegalArgumentException.class, () -> editor.replaceRange(5, 2, "x"));
        }
    }

    // ==================== Undo Tests ====================

    @Nested
    @DisplayName("Undo Tests")
    class UndoTests {

        @Test
        @DisplayName("undo insert restores previous text")
        void testUndoInsert() {
            VimEditor editor = new VimEditor("hello");
            editor.insert(" world");
            editor.undo();
            assertEquals("hello", editor.getText());
            assertEquals(5, editor.getCursor());
        }

        @Test
        @DisplayName("undo deleteBack restores deleted character")
        void testUndoDeleteBack() {
            VimEditor editor = new VimEditor("hello");
            editor.deleteBack();
            editor.undo();
            assertEquals("hello", editor.getText());
            assertEquals(5, editor.getCursor());
        }

        @Test
        @DisplayName("undo deleteAt restores deleted character")
        void testUndoDeleteAt() {
            VimEditor editor = new VimEditor("hello");
            editor.moveToStart();
            editor.deleteAt();
            editor.undo();
            assertEquals("hello", editor.getText());
            assertEquals(0, editor.getCursor());
        }

        @Test
        @DisplayName("undo deleteRange restores deleted range")
        void testUndoDeleteRange() {
            VimEditor editor = new VimEditor("hello world");
            editor.deleteRange(5, 11);
            editor.undo();
            assertEquals("hello world", editor.getText());
        }

        @Test
        @DisplayName("undo replaceChar restores original character")
        void testUndoReplaceChar() {
            VimEditor editor = new VimEditor("hello");
            editor.replaceChar(0, 'H');
            assertEquals("Hello", editor.getText());
            editor.undo();
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("undo replaceRange restores original text")
        void testUndoReplaceRange() {
            VimEditor editor = new VimEditor("hello world");
            editor.replaceRange(0, 5, "HELLO");
            editor.undo();
            assertEquals("hello world", editor.getText());
        }

        @Test
        @DisplayName("undo on fresh editor returns false")
        void testUndoEmpty() {
            VimEditor editor = new VimEditor("hello");
            assertFalse(editor.undo());
            assertFalse(editor.canUndo());
        }

        @Test
        @DisplayName("multiple undos in sequence")
        void testMultipleUndos() {
            VimEditor editor = new VimEditor();
            editor.insert("a");
            editor.insert("b");
            editor.insert("c");
            assertEquals("abc", editor.getText());

            editor.undo();
            assertEquals("ab", editor.getText());
            editor.undo();
            assertEquals("a", editor.getText());
            editor.undo();
            assertEquals("", editor.getText());
            assertFalse(editor.undo()); // nothing left
        }
    }

    // ==================== Redo Tests ====================

    @Nested
    @DisplayName("Redo Tests")
    class RedoTests {

        @Test
        @DisplayName("redo after undo restores the operation")
        void testRedo() {
            VimEditor editor = new VimEditor("hello");
            editor.insert(" world");
            editor.undo();
            assertEquals("hello", editor.getText());
            editor.redo();
            assertEquals("hello world", editor.getText());
            assertEquals(11, editor.getCursor());
        }

        @Test
        @DisplayName("redo deleteBack")
        void testRedoDeleteBack() {
            VimEditor editor = new VimEditor("hello");
            editor.deleteBack();
            editor.undo();
            assertEquals("hello", editor.getText());
            editor.redo();
            assertEquals("hell", editor.getText());
        }

        @Test
        @DisplayName("redo replaceChar")
        void testRedoReplaceChar() {
            VimEditor editor = new VimEditor("hello");
            editor.replaceChar(0, 'H');
            editor.undo();
            editor.redo();
            assertEquals("Hello", editor.getText());
        }

        @Test
        @DisplayName("redo on fresh editor returns false")
        void testRedoEmpty() {
            VimEditor editor = new VimEditor("hello");
            assertFalse(editor.redo());
            assertFalse(editor.canRedo());
        }

        @Test
        @DisplayName("new edit clears redo stack")
        void testNewEditClearsRedo() {
            VimEditor editor = new VimEditor("hello");
            editor.insert(" world");
            editor.undo();
            assertTrue(editor.canRedo());

            editor.insert(" earth"); // new edit clears redo
            assertFalse(editor.canRedo());
            assertEquals("hello earth", editor.getText());
        }

        @Test
        @DisplayName("multiple undo then redo")
        void testMultipleUndoRedo() {
            VimEditor editor = new VimEditor();
            editor.insert("a");
            editor.insert("b");
            editor.insert("c");

            editor.undo(); // remove c
            editor.undo(); // remove b
            assertEquals("a", editor.getText());

            editor.redo(); // re-add b
            assertEquals("ab", editor.getText());
            editor.redo(); // re-add c
            assertEquals("abc", editor.getText());
            assertFalse(editor.redo()); // nothing left
        }
    }

    // ==================== Undo/Redo Interaction Tests ====================

    @Nested
    @DisplayName("Undo/Redo Interaction Tests")
    class UndoRedoInteractionTests {

        @Test
        @DisplayName("undo-redo-undo cycle")
        void testUndoRedoUndoCycle() {
            VimEditor editor = new VimEditor("hello");
            editor.insert("!");
            assertEquals("hello!", editor.getText());

            editor.undo();
            assertEquals("hello", editor.getText());

            editor.redo();
            assertEquals("hello!", editor.getText());

            editor.undo();
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("interleaved operations with undo")
        void testInterleavedOps() {
            VimEditor editor = new VimEditor("hello");
            editor.replaceChar(0, 'H');   // Hello
            editor.insert("!");           // Hello!
            editor.moveTo(5);
            editor.deleteBack();          // Hell!

            assertEquals("Hell!", editor.getText());

            editor.undo(); // restore 'o' → Hello!
            assertEquals("Hello!", editor.getText());

            editor.undo(); // remove '!' → Hello
            assertEquals("Hello", editor.getText());

            editor.undo(); // restore 'h' → hello
            assertEquals("hello", editor.getText());
        }

        @Test
        @DisplayName("redo is cleared after any new edit")
        void testRedoClearedAfterEdit() {
            VimEditor editor = new VimEditor("abc");
            editor.insert("d");       // abcd
            editor.undo();            // abc, redo has 'd'

            editor.deleteBack();      // ab, redo cleared
            assertFalse(editor.canRedo());
            assertEquals("ab", editor.getText());
        }

        @Test
        @DisplayName("undo all then redo all")
        void testUndoAllRedoAll() {
            VimEditor editor = new VimEditor();
            editor.insert("a");
            editor.insert("b");
            editor.insert("c");
            editor.replaceChar(0, 'A');
            editor.deleteBack();

            // Undo everything
            while (editor.canUndo()) editor.undo();
            assertEquals("", editor.getText());

            // Redo everything
            while (editor.canRedo()) editor.redo();
            assertEquals("Ab", editor.getText());
        }
    }

    // ==================== Complex / Edge Case Tests ====================

    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {

        @Test
        @DisplayName("build text character by character, then undo all")
        void testBuildAndUndoAll() {
            VimEditor editor = new VimEditor();
            String word = "hello";
            for (char c : word.toCharArray()) {
                editor.insert(c);
            }
            assertEquals("hello", editor.getText());

            for (int i = 0; i < word.length(); i++) {
                editor.undo();
            }
            assertEquals("", editor.getText());
        }

        @Test
        @DisplayName("insert, move, insert produces correct text")
        void testInsertMoveInsert() {
            VimEditor editor = new VimEditor();
            editor.insert("world");
            editor.moveToStart();
            editor.insert("hello ");
            assertEquals("hello world", editor.getText());
            assertEquals(6, editor.getCursor());
        }

        @Test
        @DisplayName("replace entire content")
        void testReplaceEntireContent() {
            VimEditor editor = new VimEditor("old content");
            editor.replaceRange(0, editor.length(), "new content");
            assertEquals("new content", editor.getText());
        }

        @Test
        @DisplayName("cursor position preserved through undo of deleteRange")
        void testCursorPreservedOnUndoDeleteRange() {
            VimEditor editor = new VimEditor("hello world");
            editor.moveTo(7); // cursor at position 7
            editor.deleteRange(0, 5); // delete "hello"
            assertEquals(" world", editor.getText());

            editor.undo();
            assertEquals("hello world", editor.getText());
            assertEquals(7, editor.getCursor()); // cursor restored
        }

        @Test
        @DisplayName("single character editor operations")
        void testSingleCharEditor() {
            VimEditor editor = new VimEditor("x");
            editor.moveToStart();
            editor.replaceChar(0, 'y');
            assertEquals("y", editor.getText());

            editor.deleteAt();
            assertEquals("", editor.getText());

            editor.undo(); // restore 'y'
            assertEquals("y", editor.getText());

            editor.undo(); // restore 'x'
            assertEquals("x", editor.getText());
        }

        @Test
        @DisplayName("deleteBack and deleteAt interleaved")
        void testDeleteBackAndDeleteAt() {
            VimEditor editor = new VimEditor("abcde");
            editor.moveTo(2); // cursor between 'b' and 'c'
            editor.deleteBack();  // delete 'b' → acde, cursor=1
            editor.deleteAt();    // delete 'c' → ade, cursor=1
            assertEquals("ade", editor.getText());
            assertEquals(1, editor.getCursor());

            editor.undo(); // restore 'c' → acde
            assertEquals("acde", editor.getText());
            editor.undo(); // restore 'b' → abcde
            assertEquals("abcde", editor.getText());
        }

        @Test
        @DisplayName("replaceRange undo restores cursor position")
        void testReplaceRangeUndoCursor() {
            VimEditor editor = new VimEditor("hello world");
            editor.moveTo(3);
            editor.replaceRange(0, 5, "HI");
            assertEquals("HI world", editor.getText());
            assertEquals(2, editor.getCursor());

            editor.undo();
            assertEquals("hello world", editor.getText());
            assertEquals(3, editor.getCursor()); // original cursor restored
        }
    }
}
