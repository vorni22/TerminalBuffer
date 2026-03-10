package terminalbuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TerminalBufferTest {

    private static final int WIDTH = 10;
    private static final int HEIGHT = 4;
    private static final int MAX_SCROLLBACK = 10;

    private TerminalBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK);
    }

    @Nested
    @DisplayName("Write and cursor movement")
    class WriteTests {

        @Test
        @DisplayName("Write places text at cursor and advances cursor")
        void writeAtOrigin() {
            printHeader("writeAtOrigin — BEFORE WRITE");
            buffer.debugPrint();

            buffer.write("Hello");

            printHeader("writeAtOrigin — AFTER WRITE");
            buffer.debugPrint();

            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getColumn()).isEqualTo(5);
            assertThat(pos.getRow()).isEqualTo(0);

            // First row should contain "Hello", rest empty
            for (int col = 0; col < 5; col++) {
                assertThat(buffer.getScreenCell(0, col).getCharacter()).isEqualTo("Hello".charAt(col));
            }
            for (int col = 5; col < WIDTH; col++) {
                assertThat(buffer.getScreenCell(0, col).isEmpty()).isTrue();
            }
        }

        @Test
        @DisplayName("Write at non-zero cursor position")
        void writeAtOffset() {
            printHeader("writeAtOffset — BEFORE WRITE");
            buffer.debugPrint();

            buffer.setScreenCursorPos(1, 3);
            buffer.write("AB");

            printHeader("writeAtOffset — AFTER WRITE");
            buffer.debugPrint();

            assertThat(buffer.getScreenCell(1, 3).getCharacter()).isEqualTo('A');
            assertThat(buffer.getScreenCell(1, 4).getCharacter()).isEqualTo('B');
            // Cell before cursor should still be empty
            assertThat(buffer.getScreenCell(1, 2).isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Write overwrites existing content")
        void writeOverwrites() {
            buffer.write("ABCDEFGH");

            printHeader("writeOverwrites — BEFORE OVERWRITE");
            buffer.debugPrint();

            buffer.setScreenCursorPos(0, 2);
            buffer.write("XY");

            printHeader("writeOverwrites — AFTER OVERWRITE");
            buffer.debugPrint();

            assertThat(buffer.getScreenCell(0, 0).getCharacter()).isEqualTo('A');
            assertThat(buffer.getScreenCell(0, 1).getCharacter()).isEqualTo('B');
            assertThat(buffer.getScreenCell(0, 2).getCharacter()).isEqualTo('X');
            assertThat(buffer.getScreenCell(0, 3).getCharacter()).isEqualTo('Y');
            assertThat(buffer.getScreenCell(0, 4).getCharacter()).isEqualTo('E');
        }

        @Test
        @DisplayName("Write that exceeds width wraps into next screen row of same logical line")
        void writePastWidth() {
            TerminalBuffer wide = new TerminalBuffer(WIDTH, 6, MAX_SCROLLBACK);

            printHeader("writePastWidth — BEFORE WRITE");
            wide.debugPrint();

            wide.write("1234567890XY");

            printHeader("writePastWidth — AFTER WRITE");
            wide.debugPrint();

            // Row 0: "1234567890"
            for (int col = 0; col < WIDTH; col++) {
                assertThat(wide.getScreenCell(0, col).getCharacter()).isEqualTo("1234567890".charAt(col));
            }
            // Row 1: "XY" + padding (same logical line, wrapped)
            assertThat(wide.getScreenCell(1, 0).getCharacter()).isEqualTo('X');
            assertThat(wide.getScreenCell(1, 1).getCharacter()).isEqualTo('Y');
        }
    }

    @Nested
    @DisplayName("Insert")
    class InsertTests {

        @Test
        @DisplayName("Insert pushes existing content right")
        void insertShiftsContent() {
            printHeader("insertShiftsContent — BEFORE WRITE");
            buffer.debugPrint();

            buffer.write("ABCDE");

            printHeader("insertShiftsContent — AFTER WRITE");
            buffer.debugPrint();

            buffer.setScreenCursorPos(0, 2);

            printHeader("insertShiftsContent — AFTER SET POS");
            buffer.debugPrint();

            buffer.insert("XY");

            printHeader("insertShiftsContent — AFTER INSERT");
            buffer.debugPrint();

            assertThat(buffer.getScreenCell(0, 0).getCharacter()).isEqualTo('A');
            assertThat(buffer.getScreenCell(0, 1).getCharacter()).isEqualTo('B');
            assertThat(buffer.getScreenCell(0, 2).getCharacter()).isEqualTo('X');
            assertThat(buffer.getScreenCell(0, 3).getCharacter()).isEqualTo('Y');
            assertThat(buffer.getScreenCell(0, 4).getCharacter()).isEqualTo('C');
            assertThat(buffer.getScreenCell(0, 5).getCharacter()).isEqualTo('D');
            assertThat(buffer.getScreenCell(0, 6).getCharacter()).isEqualTo('E');
        }
    }

    @Nested
    @DisplayName("Scrollback")
    class ScrollbackTests {

        @Test
        @DisplayName("Pushing lines beyond screen height creates scrollback")
        void pushCreatesScrollback() {
            // Write content on each logical line
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("Line" + i);
            }

            printHeader("pushCreatesScrollback — BEFORE PUSH");
            buffer.debugPrint();

            // Push 2 extra lines — first 2 should go to scrollback
            buffer.pushScreenLine();
            buffer.pushScreenLine();

            printHeader("pushCreatesScrollback — AFTER PUSH x2");
            buffer.debugPrint();

            // Scrollback should have the content from the original top lines
            assertThat(buffer.getScrollbackCell(0, 0).getCharacter()).isEqualTo('L');
            assertThat(buffer.getScrollbackCell(0, 4).getCharacter()).isEqualTo('0');

            assertThat(buffer.getScrollbackCell(1, 0).getCharacter()).isEqualTo('L');
            assertThat(buffer.getScrollbackCell(1, 4).getCharacter()).isEqualTo('1');
        }

        @Test
        @DisplayName("Scrollback respects max size limit")
        void scrollbackEnforcesMax() {
            TerminalBuffer small = new TerminalBuffer(WIDTH, HEIGHT, 3);

            // Fill screen
            for (int i = 0; i < HEIGHT; i++) {
                small.setScreenCursorPos(i, 0);
                small.write("R" + i);
            }

            printHeader("scrollbackEnforcesMax — BEFORE PUSH");
            small.debugPrint();

            // Push 5 extra lines — scrollback can only hold 3
            for (int i = 0; i < 5; i++) {
                small.pushScreenLine();
            }

            printHeader("scrollbackEnforcesMax — AFTER PUSH x5");
            small.debugPrint();

            // Scrollback should have at most 3 rows
            // Oldest lines should have been evicted
            // Row 0 of scrollback should be "R2" (R0 and R1 evicted)
            assertThat(small.getScrollbackCell(0, 0).getCharacter()).isEqualTo('R');
            assertThat(small.getScrollbackCell(0, 1).getCharacter()).isEqualTo('2');

            // Accessing row 3 should throw — only 3 rows in scrollback
            assertThatThrownBy(() -> small.getScrollbackCell(3, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Long logical line straddles scrollback and screen boundary")
        void straddlingLogicalLine() {
            TerminalBuffer tall = new TerminalBuffer(WIDTH, 6, MAX_SCROLLBACK);

            printHeader("straddlingLogicalLine — BEFORE WRITE");
            tall.debugPrint();

            String longText = "ABCDEFGHIJ" + "KLMNOPQRST" + "UVWXYZ1234";
            tall.write(longText);

            printHeader("straddlingLogicalLine — AFTER WRITE");
            tall.debugPrint();

            assertThat(tall.getScreenCell(0, 0).getCharacter()).isEqualTo('A');
            assertThat(tall.getScreenCell(1, 0).getCharacter()).isEqualTo('K');
            assertThat(tall.getScreenCell(2, 0).getCharacter()).isEqualTo('U');

            printHeader("straddlingLogicalLine — BEFORE PUSH");
            tall.debugPrint();

            tall.pushScreenLine();
            tall.pushScreenLine();
            tall.pushScreenLine();

            printHeader("straddlingLogicalLine — AFTER PUSH x3");
            tall.debugPrint();

            assertThat(tall.getScrollbackCell(0, 0).getCharacter()).isEqualTo('A');
        }
    }

    @Nested
    @DisplayName("Clear")
    class ClearTests {

        @Test
        @DisplayName("clearScreen empties screen but preserves scrollback")
        void clearScreenKeepsScrollback() {
            // Push content into scrollback first
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("Line" + i);
            }
            buffer.pushScreenLine();
            buffer.pushScreenLine();

            printHeader("clearScreenKeepsScrollback — BEFORE CLEAR");
            buffer.debugPrint();

            // Now clear screen
            buffer.clearScreen();

            printHeader("clearScreenKeepsScrollback — AFTER CLEAR");
            buffer.debugPrint();

            // Screen should be empty
            for (int row = 0; row < HEIGHT; row++) {
                for (int col = 0; col < WIDTH; col++) {
                    assertThat(buffer.getScreenCell(row, col).isEmpty()).isTrue();
                }
            }

            // Scrollback should still exist
            assertThat(buffer.getScrollbackCell(0, 0).getCharacter()).isEqualTo('L');
        }

        @Test
        @DisplayName("clearAll empties both screen and scrollback")
        void clearAllRemovesEverything() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("Line" + i);
            }
            buffer.pushScreenLine();

            printHeader("clearAllRemovesEverything — BEFORE CLEAR ALL");
            buffer.debugPrint();

            buffer.clearAll();

            printHeader("clearAllRemovesEverything — AFTER CLEAR ALL");
            buffer.debugPrint();

            // Screen should be empty
            for (int row = 0; row < HEIGHT; row++) {
                for (int col = 0; col < WIDTH; col++) {
                    assertThat(buffer.getScreenCell(row, col).isEmpty()).isTrue();
                }
            }

            // Scrollback should be empty — accessing row 0 should throw
            assertThatThrownBy(() -> buffer.getScrollbackCell(0, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Screen cursor movement")
    class CursorTests {

        @Test
        @DisplayName("Cursor clamps to screen bounds")
        void cursorClamping() {
            printHeader("cursorClamping — BEFORE CLAMP");
            buffer.debugPrint();

            buffer.setScreenCursorPos(-5, -3);

            printHeader("cursorClamping — AFTER NEGATIVE SET");
            buffer.debugPrint();

            buffer.setScreenCursorPos(999, 999);
            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(HEIGHT - 1);
            assertThat(pos.getColumn()).isEqualTo(WIDTH - 1);
        }

        @Test
        @DisplayName("moveScreenCursor moves and clamps")
        void moveScreenCursorClamped() {
            buffer.setScreenCursorPos(2, 5);

            printHeader("moveScreenCursorClamped — BEFORE MOVES");
            buffer.debugPrint();

            buffer.moveScreenCursor(TerminalBuffer.CursorMove.UP, 10);

            printHeader("moveScreenCursorClamped — AFTER MOVE UP 10");
            buffer.debugPrint();
        }
    }

    @Nested
    @DisplayName("Resize")
    class ResizeTests {

        @Test
        @DisplayName("Resize wider keeps content intact")
        void resizeWider() {
            buffer.write("Hello");

            printHeader("resizeWider — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(20, HEIGHT);

            printHeader("resizeWider — AFTER RESIZE(20,4)");
            buffer.debugPrint();

            assertThat(buffer.getScreenCell(0, 0).getCharacter()).isEqualTo('H');
            assertThat(buffer.getScreenCell(0, 4).getCharacter()).isEqualTo('o');
        }

        @Test
        @DisplayName("Resize narrower reflows long lines")
        void resizeNarrower() {
            TerminalBuffer tall = new TerminalBuffer(WIDTH, 6, MAX_SCROLLBACK);
            tall.write("1234567890");

            printHeader("resizeNarrower — BEFORE RESIZE");
            tall.debugPrint();

            tall.resize(5, 6);

            printHeader("resizeNarrower — AFTER RESIZE(5,6)");
            tall.debugPrint();

            assertThat(tall.getScreenCell(0, 0).getCharacter()).isEqualTo('1');
            assertThat(tall.getScreenCell(0, 4).getCharacter()).isEqualTo('5');
            assertThat(tall.getScreenCell(1, 0).getCharacter()).isEqualTo('6');
            assertThat(tall.getScreenCell(1, 4).getCharacter()).isEqualTo('0');
        }

        @Test
        @DisplayName("Resize shorter pushes lines to scrollback")
        void resizeShorter() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("R" + i);
            }

            printHeader("resizeShorter — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(WIDTH, 2); // only 2 screen rows now

            printHeader("resizeShorter — AFTER RESIZE(10,2)");
            buffer.debugPrint();

            // Top lines should have moved to scrollback
            assertThat(buffer.getScrollbackCell(0, 0).getCharacter()).isEqualTo('R');
        }

        @Test
        @DisplayName("Resize narrower then wider preserves content")
        void resizeNarrowerThenWider() {
            TerminalBuffer tb = new TerminalBuffer(10, 6, MAX_SCROLLBACK);
            tb.write("ABCDEFGHIJ"); // 10 chars, 1 row at width=10

            printHeader("resizeNarrowerThenWider — BEFORE RESIZE");
            tb.debugPrint();

            tb.resize(5, 6);  // now 2 rows

            printHeader("resizeNarrowerThenWider — AFTER RESIZE(5,6)");
            tb.debugPrint();

            tb.resize(10, 6); // back to 1 row

            printHeader("resizeNarrowerThenWider — AFTER RESIZE(10,6)");
            tb.debugPrint();

            assertThat(tb.getScreenCell(0, 0).getCharacter()).isEqualTo('A');
            assertThat(tb.getScreenCell(0, 9).getCharacter()).isEqualTo('J');
        }
    }

    @Nested
    @DisplayName("Fill")
    class FillTests {

        @Test
        @DisplayName("fillScreenLine fills the current screen row")
        void fillScreenLineRow() {
            buffer.write("Hello");
            buffer.setScreenCursorPos(0, 0);

            printHeader("fillScreenLineRow — BEFORE FILL");
            buffer.debugPrint();

            buffer.fillScreenLine('X');

            printHeader("fillScreenLineRow — AFTER FILL");
            buffer.debugPrint();

            for (int col = 0; col < WIDTH; col++) {
                assertThat(buffer.getScreenCell(0, col).getCharacter()).isEqualTo('X');
            }
            // Other rows should be unaffected
            assertThat(buffer.getScreenCell(1, 0).isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("String representations")
    class ToStringTests {

        @Test
        @DisplayName("screenToString returns HEIGHT rows of WIDTH characters")
        void screenToStringDimensions() {
            String screen = buffer.screenToString();
            String[] rows = screen.split("\n", -1);
            assertThat(rows).hasSize(HEIGHT);
            for (String r : rows) {
                assertThat(r).hasSize(WIDTH);
            }
        }

        @Test
        @DisplayName("screenToString reflects written content")
        void screenToStringContent() {
            printHeader("screenToStringContent — BEFORE WRITE");
            buffer.debugPrint();

            buffer.write("TestLine");

            printHeader("screenToStringContent — AFTER WRITE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("screenAndScrollbackToString includes both regions")
        void fullToString() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("L" + i);
            }

            printHeader("fullToString — BEFORE PUSH");
            buffer.debugPrint();

            buffer.pushScreenLine();

            printHeader("fullToString — AFTER PUSH");
            buffer.debugPrint();
        }
    }

    @Nested
    @DisplayName("Edge cases — Write")
    class WriteEdgeCaseTests {

        @Test
        @DisplayName("Write empty string does nothing")
        void writeEmptyString() {
            printHeader("writeEmptyString — BEFORE WRITE");
            buffer.debugPrint();

            buffer.write("");
            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isEqualTo(0);
            assertThat(buffer.getScreenCell(0, 0).isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Write exactly fills one row")
        void writeExactlyOneRow() {
            printHeader("writeExactlyOneRow — BEFORE WRITE");
            buffer.debugPrint();

            buffer.write("1234567890");

            printHeader("writeExactlyOneRow — AFTER WRITE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Write exactly fills entire screen")
        void writeExactlyFillsScreen() {
            printHeader("writeExactlyFillsScreen — BEFORE WRITE");
            buffer.debugPrint();

            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                String row = String.valueOf((char) ('A' + i)).repeat(WIDTH);
                buffer.write(row);
            }

            printHeader("writeExactlyFillsScreen — AFTER WRITE");
            buffer.debugPrint();

            for (int row = 0; row < HEIGHT; row++) {
                for (int col = 0; col < WIDTH; col++) {
                    assertThat(buffer.getScreenCell(row, col).getCharacter())
                            .isEqualTo((char) ('A' + row));
                }
            }
        }

        @Test
        @DisplayName("Write single character")
        void writeSingleChar() {
            printHeader("writeSingleChar — BEFORE WRITE");
            buffer.debugPrint();

            buffer.write("Z");

            printHeader("writeSingleChar — AFTER WRITE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Write wrapping across multiple rows on small buffer")
        void writeWrapsMultipleRows() {
            TerminalBuffer small = new TerminalBuffer(3, 5, MAX_SCROLLBACK);

            printHeader("writeWrapsMultipleRows — BEFORE WRITE");
            small.debugPrint();

            small.write("ABCDEFGHI"); // 9 chars = 3 rows of width 3

            printHeader("writeWrapsMultipleRows — AFTER WRITE");
            small.debugPrint();
        }

        @Test
        @DisplayName("Write that causes scroll — text longer than entire screen")
        void writeLongerThanScreen() {
            printHeader("writeLongerThanScreen — BEFORE WRITE");
            buffer.debugPrint();

            // WIDTH=10, HEIGHT=4 → 40 cells on screen
            // Write 60 chars → should push lines into scrollback
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 60; i++) {
                sb.append((char) ('A' + (i % 26)));
            }
            buffer.write(sb.toString());

            printHeader("writeLongerThanScreen — AFTER WRITE");
            buffer.debugPrint();

            // The last 4 screen rows should have the tail of the text
            // and scrollback should contain the beginning
            assertThat(buffer.getScrollbackCell(0, 0).getCharacter()).isEqualTo('A');
        }

        @Test
        @DisplayName("Sequential writes on same line accumulate")
        void sequentialWrites() {
            printHeader("sequentialWrites — BEFORE WRITES");
            buffer.debugPrint();

            buffer.write("AB");
            buffer.write("CD");
            buffer.write("EF");

            printHeader("sequentialWrites — AFTER WRITES");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Write after moving cursor to last column")
        void writeAtLastColumn() {
            printHeader("writeAtLastColumn — BEFORE WRITE");
            buffer.debugPrint();

            buffer.setScreenCursorPos(0, WIDTH - 1);
            buffer.write("XY");

            printHeader("writeAtLastColumn — AFTER WRITE");
            buffer.debugPrint();
        }
    }

    @Nested
    @DisplayName("Edge cases — Insert")
    class InsertEdgeCaseTests {

        @Test
        @DisplayName("Insert at beginning of line")
        void insertAtBeginning() {
            buffer.write("ABCDE");

            printHeader("insertAtBeginning — BEFORE INSERT");
            buffer.debugPrint();

            buffer.setScreenCursorPos(0, 0);
            buffer.insert("XY");

            printHeader("insertAtBeginning — AFTER INSERT");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Insert that causes line to wrap")
        void insertCausesWrap() {
            buffer.write("12345678"); // 8 chars on width-10 line

            printHeader("insertCausesWrap — BEFORE INSERT");
            buffer.debugPrint();

            buffer.setScreenCursorPos(0, 4);
            buffer.insert("ABCD"); // pushes chars right, total 12 → wraps

            printHeader("insertCausesWrap — AFTER INSERT");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Insert empty string does nothing")
        void insertEmptyString() {
            buffer.write("ABC");

            printHeader("insertEmptyString — BEFORE INSERT");
            buffer.debugPrint();

            buffer.setScreenCursorPos(0, 1);
            buffer.insert("");

            printHeader("insertEmptyString — AFTER INSERT");
            buffer.debugPrint();
        }
    }

    @Nested
    @DisplayName("Edge cases — Scrollback")
    class ScrollbackEdgeCaseTests {

        @Test
        @DisplayName("Scrollback with zero max size never stores lines")
        void zeroMaxScrollback() {
            TerminalBuffer zeroSb = new TerminalBuffer(WIDTH, HEIGHT, 0);
            for (int i = 0; i < HEIGHT; i++) {
                zeroSb.setScreenCursorPos(i, 0);
                zeroSb.write("R" + i);
            }

            printHeader("zeroMaxScrollback — BEFORE PUSH");
            zeroSb.debugPrint();

            zeroSb.pushScreenLine();
            zeroSb.pushScreenLine();

            printHeader("zeroMaxScrollback — AFTER PUSH x2");
            zeroSb.debugPrint();

            // Scrollback should be empty
            assertThatThrownBy(() -> zeroSb.getScrollbackCell(0, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Push many lines — scrollback cycles through max")
        void pushManyLines() {
            TerminalBuffer tb = new TerminalBuffer(WIDTH, HEIGHT, 5);

            printHeader("pushManyLines — BEFORE PUSHES");
            tb.debugPrint();

            for (int i = 0; i < 20; i++) {
                tb.setScreenCursorPos(0, 0);
                tb.write("Iter" + String.format("%02d", i));
                tb.pushScreenLine();
            }

            printHeader("pushManyLines — AFTER 20 PUSHES");
            tb.debugPrint();

            // Scrollback should contain at most 5 rows
            // Verify we can access rows 0–4 but not 5
            for (int i = 0; i < 5; i++) {
                final int row = i;
                assertThatCode(() -> tb.getScrollbackCell(row, 0))
                        .doesNotThrowAnyException();
            }
            assertThatThrownBy(() -> tb.getScrollbackCell(5, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Scrollback after multiple wrapping writes")
        void scrollbackWithWrappedLines() {
            TerminalBuffer tb = new TerminalBuffer(5, 4, 10);
            // Line of 15 chars → 3 rows on a width-5 screen
            tb.write("ABCDEFGHIJKLMNO");

            printHeader("scrollbackWithWrappedLines — AFTER WRITE (BEFORE PUSH)");
            tb.debugPrint();

            // Now push a line — 3-row logical line should partially scroll
            tb.pushScreenLine();

            printHeader("scrollbackWithWrappedLines — AFTER PUSH");
            tb.debugPrint();

            // First scrollback row should be "ABCDE"
            assertThat(tb.getScrollbackCell(0, 0).getCharacter()).isEqualTo('A');
            assertThat(tb.getScrollbackCell(0, 4).getCharacter()).isEqualTo('E');
        }
    }

    @Nested
    @DisplayName("Edge cases — Cursor")
    class CursorEdgeCaseTests {

        @Test
        @DisplayName("Set cursor then write on different rows")
        void writeOnMultipleRows() {
            printHeader("writeOnMultipleRows — BEFORE WRITES");
            buffer.debugPrint();

            buffer.setScreenCursorPos(0, 0);
            buffer.write("Row0");
            buffer.setScreenCursorPos(2, 0);
            buffer.write("Row2");

            printHeader("writeOnMultipleRows — AFTER WRITES");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Move cursor in all four directions")
        void moveCursorAllDirections() {
            printHeader("moveCursorAllDirections — BEFORE MOVES");
            buffer.debugPrint();

            buffer.setScreenCursorPos(2, 5);

            buffer.moveScreenCursor(TerminalBuffer.CursorMove.UP, 1);
            assertThat(buffer.getScreenCursorPos().getRow()).isEqualTo(1);
            assertThat(buffer.getScreenCursorPos().getColumn()).isEqualTo(5);

            buffer.moveScreenCursor(TerminalBuffer.CursorMove.DOWN, 2);
            assertThat(buffer.getScreenCursorPos().getRow()).isEqualTo(3);

            buffer.moveScreenCursor(TerminalBuffer.CursorMove.LEFT, 3);
            assertThat(buffer.getScreenCursorPos().getColumn()).isEqualTo(2);

            buffer.moveScreenCursor(TerminalBuffer.CursorMove.RIGHT, 1);
            assertThat(buffer.getScreenCursorPos().getColumn()).isEqualTo(3);

            printHeader("moveCursorAllDirections — AFTER MOVES");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Move cursor with zero delta does nothing")
        void moveCursorZeroDelta() {
            buffer.setScreenCursorPos(1, 3);

            printHeader("moveCursorZeroDelta — BEFORE MOVE");
            buffer.debugPrint();

            buffer.moveScreenCursor(TerminalBuffer.CursorMove.UP, 0);

            printHeader("moveCursorZeroDelta — AFTER MOVE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Cursor position is valid after pushScreenLine")
        void cursorAfterPush() {
            buffer.setScreenCursorPos(HEIGHT - 1, 5);
            buffer.write("Test");

            printHeader("cursorAfterPush — BEFORE PUSH");
            buffer.debugPrint();

            buffer.pushScreenLine();

            printHeader("cursorAfterPush — AFTER PUSH");
            buffer.debugPrint();
        }
    }

    @Nested
    @DisplayName("Edge cases — Resize")
    class ResizeEdgeCaseTests {

        @Test
        @DisplayName("Resize to width 1 reflows all content into single-column lines")
        void resizeToWidth1() {
            TerminalBuffer tb = new TerminalBuffer(5, 10, MAX_SCROLLBACK);
            tb.write("ABCDE");

            printHeader("resizeToWidth1 — BEFORE RESIZE");
            tb.debugPrint();

            tb.resize(1, 10);

            printHeader("resizeToWidth1 — AFTER RESIZE(1,10)");
            tb.debugPrint();

            assertThat(tb.getScreenCell(0, 0).getCharacter()).isEqualTo('A');
            assertThat(tb.getScreenCell(1, 0).getCharacter()).isEqualTo('B');
            assertThat(tb.getScreenCell(2, 0).getCharacter()).isEqualTo('C');
            assertThat(tb.getScreenCell(3, 0).getCharacter()).isEqualTo('D');
            assertThat(tb.getScreenCell(4, 0).getCharacter()).isEqualTo('E');
        }

        @Test
        @DisplayName("Resize to same dimensions is no-op")
        void resizeSameDimensions() {
            buffer.write("Hello");

            printHeader("resizeSameDimensions — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(WIDTH, HEIGHT);

            printHeader("resizeSameDimensions — AFTER RESIZE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Resize taller adds empty rows")
        void resizeTaller() {
            buffer.write("Hello");

            printHeader("resizeTaller — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(WIDTH, HEIGHT + 4);

            printHeader("resizeTaller — AFTER RESIZE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Resize to height 1")
        void resizeToHeight1() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("R" + i);
            }

            printHeader("resizeToHeight1 — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(WIDTH, 1);

            printHeader("resizeToHeight1 — AFTER RESIZE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Multiple resizes preserve content integrity")
        void multipleResizes() {
            TerminalBuffer tb = new TerminalBuffer(10, 10, MAX_SCROLLBACK);
            tb.write("ABCDEFGHIJ");

            printHeader("multipleResizes — BEFORE RESIZE CHAIN");
            tb.debugPrint();

            tb.resize(5, 10);   // 2 rows
            tb.resize(2, 10);   // 5 rows
            tb.resize(10, 10);  // back to 1 row

            printHeader("multipleResizes — AFTER RESIZE CHAIN");
            tb.debugPrint();

            assertThat(tb.getScreenCell(0, 0).getCharacter()).isEqualTo('A');
            assertThat(tb.getScreenCell(0, 9).getCharacter()).isEqualTo('J');
        }

        @Test
        @DisplayName("Resize narrower with content on multiple logical lines")
        void resizeNarrowerMultipleLines() {
            buffer.setScreenCursorPos(0, 0);
            buffer.write("ABCDEFGH");
            buffer.setScreenCursorPos(1, 0);
            buffer.write("12345678");

            printHeader("resizeNarrowerMultipleLines — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(4, HEIGHT);

            printHeader("resizeNarrowerMultipleLines — AFTER RESIZE(4,4)");
            buffer.debugPrint();

            // Line 0: "ABCD" "EFGH"
            assertThat(buffer.getScreenCell(0, 0).getCharacter()).isEqualTo('A');
            assertThat(buffer.getScreenCell(0, 3).getCharacter()).isEqualTo('D');
            assertThat(buffer.getScreenCell(1, 0).getCharacter()).isEqualTo('E');
            assertThat(buffer.getScreenCell(1, 3).getCharacter()).isEqualTo('H');
        }

        @Test
        @DisplayName("Resize with scrollback content — scrollback lines also reflow")
        void resizeWithScrollback() {
            TerminalBuffer tb = new TerminalBuffer(10, 4, 10);
            for (int i = 0; i < 4; i++) {
                tb.setScreenCursorPos(i, 0);
                tb.write("Line" + i + "ABCD");
            }

            printHeader("resizeWithScrollback — BEFORE PUSH");
            tb.debugPrint();

            tb.pushScreenLine();
            tb.pushScreenLine();

            printHeader("resizeWithScrollback — AFTER PUSH (BEFORE RESIZE)");
            tb.debugPrint();

            tb.resize(5, 4);

            printHeader("resizeWithScrollback — AFTER RESIZE(5,4)");
            tb.debugPrint();

            // Scrollback should still have content
            assertThat(tb.getScrollbackCell(0, 0).getCharacter()).isEqualTo('L');
        }
    }

    @Nested
    @DisplayName("Edge cases — Clear")
    class ClearEdgeCaseTests {

        @Test
        @DisplayName("clearScreen on empty buffer is harmless")
        void clearScreenOnEmpty() {
            printHeader("clearScreenOnEmpty — BEFORE CLEAR");
            buffer.debugPrint();

            buffer.clearScreen();

            printHeader("clearScreenOnEmpty — AFTER CLEAR");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("clearAll on empty buffer is harmless")
        void clearAllOnEmpty() {
            printHeader("clearAllOnEmpty — BEFORE CLEAR ALL");
            buffer.debugPrint();

            buffer.clearAll();

            printHeader("clearAllOnEmpty — AFTER CLEAR ALL");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Write after clearScreen works correctly")
        void writeAfterClearScreen() {
            buffer.write("Hello");

            printHeader("writeAfterClearScreen — BEFORE CLEAR");
            buffer.debugPrint();

            buffer.clearScreen();

            printHeader("writeAfterClearScreen — AFTER CLEAR");
            buffer.debugPrint();

            buffer.write("World");

            printHeader("writeAfterClearScreen — AFTER WRITE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("Write after clearAll works correctly")
        void writeAfterClearAll() {
            buffer.write("Hello");
            buffer.pushScreenLine();

            printHeader("writeAfterClearAll — BEFORE CLEAR ALL");
            buffer.debugPrint();

            buffer.clearAll();

            printHeader("writeAfterClearAll — AFTER CLEAR ALL");
            buffer.debugPrint();

            buffer.write("Fresh");

            printHeader("writeAfterClearAll — AFTER WRITE");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("clearScreen multiple times in a row")
        void clearScreenMultipleTimes() {
            buffer.write("Data");

            printHeader("clearScreenMultipleTimes — BEFORE CLEARS");
            buffer.debugPrint();

            buffer.clearScreen();
            buffer.clearScreen();
            buffer.clearScreen();

            printHeader("clearScreenMultipleTimes — AFTER CLEARS x3");
            buffer.debugPrint();
        }
    }

    @Nested
    @DisplayName("Edge cases — Fill")
    class FillEdgeCaseTests {

        @Test
        @DisplayName("fillScreenLine on empty buffer creates content")
        void fillEmptyBuffer() {
            printHeader("fillEmptyBuffer — BEFORE FILL");
            buffer.debugPrint();

            buffer.fillScreenLine('Z');

            printHeader("fillEmptyBuffer — AFTER FILL");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("fillScreenLine on non-first row")
        void fillNonFirstRow() {
            printHeader("fillNonFirstRow — BEFORE FILL");
            buffer.debugPrint();

            buffer.setScreenCursorPos(2, 0);
            buffer.fillScreenLine('Q');

            printHeader("fillNonFirstRow — AFTER FILL");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("fillScreenLine after write on same row overwrites")
        void fillOverwritesContent() {
            buffer.write("ABCDEFGHIJ");

            printHeader("fillOverwritesContent — BEFORE FILL");
            buffer.debugPrint();

            buffer.setScreenCursorPos(0, 0);
            buffer.fillScreenLine('#');

            printHeader("fillOverwritesContent — AFTER FILL");
            buffer.debugPrint();
        }
    }

    @Nested
    @DisplayName("Edge cases — Bounds checking")
    class BoundsCheckTests {

        @Test
        @DisplayName("getScreenCell throws on negative row")
        void screenCellNegativeRow() {
            assertThatThrownBy(() -> buffer.getScreenCell(-1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getScreenCell throws on row >= height")
        void screenCellRowTooLarge() {
            assertThatThrownBy(() -> buffer.getScreenCell(HEIGHT, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getScreenCell throws on negative column")
        void screenCellNegativeCol() {
            assertThatThrownBy(() -> buffer.getScreenCell(0, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getScreenCell throws on column >= width")
        void screenCellColTooLarge() {
            assertThatThrownBy(() -> buffer.getScreenCell(0, WIDTH))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getScrollbackCell throws on negative column")
        void scrollbackCellNegativeCol() {
            assertThatThrownBy(() -> buffer.getScrollbackCell(0, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getScrollbackCell throws when no scrollback exists")
        void scrollbackCellNoScrollback() {
            assertThatThrownBy(() -> buffer.getScrollbackCell(0, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Edge cases — Mixed operations")
    class MixedOperationTests {

        @Test
        @DisplayName("Write, push, clear screen, write again — scrollback preserved")
        void writePushClearWrite() {
            buffer.write("First");

            printHeader("writePushClearWrite — BEFORE PUSH");
            buffer.debugPrint();

            buffer.pushScreenLine();
            buffer.pushScreenLine();

            printHeader("writePushClearWrite — AFTER PUSH x2");
            buffer.debugPrint();

            buffer.clearScreen();
            buffer.write("Second");

            printHeader("writePushClearWrite — AFTER CLEAR+WRITE");
            buffer.debugPrint();

            assertThat(buffer.getScreenCell(0, 0).getCharacter()).isEqualTo('S');
            // Scrollback should still have "First"
            assertThat(buffer.getScrollbackCell(0, 0).getCharacter()).isEqualTo('F');
        }

        @Test
        @DisplayName("Fill, insert, write sequence")
        void fillInsertWrite() {
            printHeader("fillInsertWrite — BEFORE OPERATIONS");
            buffer.debugPrint();

            buffer.fillScreenLine('.');
            buffer.setScreenCursorPos(0, 3);
            buffer.insert("AB");

            printHeader("fillInsertWrite — AFTER FILL+INSERT");
            buffer.debugPrint();

            assertThat(buffer.getScreenCell(0, 0).getCharacter()).isEqualTo('.');
            assertThat(buffer.getScreenCell(0, 2).getCharacter()).isEqualTo('.');
            assertThat(buffer.getScreenCell(0, 3).getCharacter()).isEqualTo('A');
            assertThat(buffer.getScreenCell(0, 4).getCharacter()).isEqualTo('B');
            assertThat(buffer.getScreenCell(0, 5).getCharacter()).isEqualTo('.');
        }

        @Test
        @DisplayName("Push until screen is all empty, then write")
        void pushAllThenWrite() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("R" + i);
            }

            printHeader("pushAllThenWrite — BEFORE PUSH ALL");
            buffer.debugPrint();

            // Push all content off screen
            for (int i = 0; i < HEIGHT; i++) {
                buffer.pushScreenLine();
            }

            printHeader("pushAllThenWrite — AFTER PUSH ALL");
            buffer.debugPrint();

            // Screen should be empty
            for (int row = 0; row < HEIGHT; row++) {
                assertThat(buffer.getScreenCell(row, 0).isEmpty()).isTrue();
            }

            buffer.setScreenCursorPos(0, 0);
            buffer.write("New");
            assertThat(buffer.getScreenCell(0, 0).getCharacter()).isEqualTo('N');
        }

        @Test
        @DisplayName("Resize after push preserves scrollback")
        void resizeAfterPush() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("Line" + i);
            }
            buffer.pushScreenLine();
            buffer.pushScreenLine();

            printHeader("resizeAfterPush — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(WIDTH, HEIGHT + 2);

            printHeader("resizeAfterPush — AFTER RESIZE");
            buffer.debugPrint();

            // Scrollback should still exist
            assertThat(buffer.getScrollbackCell(0, 0).getCharacter()).isEqualTo('L');
        }

        @Test
        @DisplayName("Write on every row then resize narrower then wider")
        void fullScreenResizeRoundTrip() {
            TerminalBuffer tb = new TerminalBuffer(10, 4, MAX_SCROLLBACK);
            for (int i = 0; i < 4; i++) {
                tb.setScreenCursorPos(i, 0);
                tb.write("Row" + i + "Data!");  // 9 chars each
            }

            printHeader("fullScreenResizeRoundTrip — BEFORE RESIZE CHAIN");
            tb.debugPrint();

            tb.resize(5, 8);

            printHeader("fullScreenResizeRoundTrip — AFTER RESIZE(5,8)");
            tb.debugPrint();

            tb.resize(10, 4);

            printHeader("fullScreenResizeRoundTrip — AFTER RESIZE(10,4)");
            tb.debugPrint();

            // Content should be preserved
            assertThat(tb.getScreenCell(0, 0).getCharacter()).isEqualTo('R');
        }
    }

    @Nested
    @DisplayName("Edge cases — screenToString")
    class ToStringEdgeCaseTests {

        @Test
        @DisplayName("screenToString on fresh buffer returns all spaces")
        void screenToStringEmpty() {
            String screen = buffer.screenToString();
            String[] rows = screen.split("\n", -1);
            assertThat(rows).hasSize(HEIGHT);
            for (String r : rows) {
                assertThat(r).isEqualTo(" ".repeat(WIDTH));
            }
        }

        @Test
        @DisplayName("screenAndScrollbackToString with no scrollback equals screenToString")
        void fullToStringNoScrollback() {
            printHeader("fullToStringNoScrollback — BEFORE WRITE");
            buffer.debugPrint();

            buffer.write("Test");

            printHeader("fullToStringNoScrollback — AFTER WRITE");
            buffer.debugPrint();
        }
    }

    @Nested
    @DisplayName("Edge cases — Cursor tracking after resize")
    class CursorTrackingAfterResizeTests {

        @Test
        @DisplayName("Resize wider — cursor stays on same character")
        void resizeWiderCursorTracksChar() {
            buffer.write("ABCDEFGH");
            buffer.setScreenCursorPos(0, 4); // pointing at 'E'

            printHeader("resizeWiderCursorTracksChar — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(20, HEIGHT);

            printHeader("resizeWiderCursorTracksChar — AFTER RESIZE(20,4)");
            buffer.debugPrint();

            // 'E' is still at column 4 on a wider row
            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isEqualTo(4);
            assertThat(buffer.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('E');
        }

        @Test
        @DisplayName("Resize narrower — cursor follows character to new row")
        void resizeNarrowerCursorFollowsChar() {
            TerminalBuffer tb = new TerminalBuffer(10, 6, MAX_SCROLLBACK);
            tb.write("ABCDEFGHIJ"); // 10 chars, 1 row at width 10
            tb.setScreenCursorPos(0, 7); // pointing at 'H'

            printHeader("resizeNarrowerCursorFollowsChar — BEFORE RESIZE");
            tb.debugPrint();

            tb.resize(5, 6); // "ABCDE" on row 0, "FGHIJ" on row 1; 'H' is at row 1, col 2

            printHeader("resizeNarrowerCursorFollowsChar — AFTER RESIZE(5,6)");
            tb.debugPrint();

            CursorPosition pos = tb.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(1);
            assertThat(pos.getColumn()).isEqualTo(2);
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('H');
        }

        @Test
        @DisplayName("Resize narrower then wider — cursor returns to original position")
        void resizeNarrowerThenWiderCursorReturns() {
            TerminalBuffer tb = new TerminalBuffer(10, 6, MAX_SCROLLBACK);
            tb.write("ABCDEFGHIJ");
            tb.setScreenCursorPos(0, 6); // pointing at 'G'

            printHeader("resizeNarrowerThenWiderCursorReturns — BEFORE RESIZE");
            tb.debugPrint();

            tb.resize(5, 6);

            printHeader("resizeNarrowerThenWiderCursorReturns — AFTER RESIZE(5,6)");
            tb.debugPrint();

            // At width 5, 'G' should be at row 1, col 1
            CursorPosition pos = tb.getScreenCursorPos();
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('G');

            tb.resize(10, 6);

            printHeader("resizeNarrowerThenWiderCursorReturns — AFTER RESIZE(10,6)");
            tb.debugPrint();

            // Back to width 10, 'G' should be at row 0, col 6 again
            pos = tb.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isEqualTo(6);
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('G');
        }

        @Test
        @DisplayName("Resize that pushes cursor's character to scrollback — cursor stays on screen")
        void resizePushesCursorToScrollback() {
            // Write on all 4 rows, cursor on row 0
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("Row" + i);
            }
            buffer.setScreenCursorPos(0, 1); // pointing at 'o' on Row0

            printHeader("resizePushesCursorToScrollback — BEFORE RESIZE");
            buffer.debugPrint();

            // Resize to height 2 — Row0 and Row1 go to scrollback
            buffer.resize(WIDTH, 2);

            printHeader("resizePushesCursorToScrollback — AFTER RESIZE(10,2)");
            buffer.debugPrint();

            CursorPosition pos = buffer.getScreenCursorPos();
            // Cursor must be on screen (row 0 or 1)
            assertThat(pos.getRow()).isBetween(0, 1);
            // Cursor should NOT be in scrollback
        }

        @Test
        @DisplayName("Resize narrower with cursor on second logical line")
        void resizeNarrowerCursorOnSecondLine() {
            buffer.setScreenCursorPos(0, 0);
            buffer.write("AAAAAAAAAA"); // 10 chars on line 0
            buffer.setScreenCursorPos(1, 0);
            buffer.write("BBBBBBBBBB"); // 10 chars on line 1
            buffer.setScreenCursorPos(1, 3); // pointing at 'B' at col 3 of line 1

            printHeader("resizeNarrowerCursorOnSecondLine — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(5, 8); // each line becomes 2 rows

            printHeader("resizeNarrowerCursorOnSecondLine — AFTER RESIZE(5,8)");
            buffer.debugPrint();

            CursorPosition pos = buffer.getScreenCursorPos();
            // Line 1's 'B' at logical col 3 → at width 5 it's screen row 2 (line0 takes rows 0-1), col 3
            assertThat(buffer.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('B');
        }

        @Test
        @DisplayName("Resize to width 1 — cursor tracks character into single-column layout")
        void resizeToWidth1CursorTracks() {
            TerminalBuffer tb = new TerminalBuffer(5, 10, MAX_SCROLLBACK);
            tb.write("ABCDE");
            tb.setScreenCursorPos(0, 3); // pointing at 'D'

            printHeader("resizeToWidth1CursorTracks — BEFORE RESIZE");
            tb.debugPrint();

            tb.resize(1, 10);

            printHeader("resizeToWidth1CursorTracks — AFTER RESIZE(1,10)");
            tb.debugPrint();

            CursorPosition pos = tb.getScreenCursorPos();
            // 'D' is at logical index 3, at width 1 it's screen row 3, col 0
            assertThat(pos.getRow()).isEqualTo(3);
            assertThat(pos.getColumn()).isEqualTo(0);
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('D');
        }

        @Test
        @DisplayName("Resize same dimensions — cursor unchanged")
        void resizeSameDimensionsCursorUnchanged() {
            buffer.write("Hello");
            buffer.setScreenCursorPos(0, 3); // pointing at 'l'

            printHeader("resizeSameDimensionsCursorUnchanged — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(WIDTH, HEIGHT);

            printHeader("resizeSameDimensionsCursorUnchanged — AFTER RESIZE");
            buffer.debugPrint();

            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isEqualTo(3);
            assertThat(buffer.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('l');
        }

        @Test
        @DisplayName("Cursor on empty cell stays valid after resize")
        void cursorOnEmptyCellAfterResize() {
            buffer.write("AB");
            buffer.setScreenCursorPos(0, 5); // pointing at empty cell

            printHeader("cursorOnEmptyCellAfterResize — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(5, HEIGHT);

            printHeader("cursorOnEmptyCellAfterResize — AFTER RESIZE(5,4)");
            buffer.debugPrint();

            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isBetween(0, HEIGHT - 1);
            assertThat(pos.getColumn()).isBetween(0, 4);
        }

        @Test
        @DisplayName("Cursor on wrapped portion of logical line — resize wider unwraps")
        void cursorOnWrappedRowResizeWider() {
            TerminalBuffer tb = new TerminalBuffer(5, 6, MAX_SCROLLBACK);
            tb.write("ABCDEFGHIJ"); // 2 rows at width 5
            tb.setScreenCursorPos(1, 2); // pointing at 'H' (logical index 7)

            printHeader("cursorOnWrappedRowResizeWider — BEFORE RESIZE");
            tb.debugPrint();

            tb.resize(10, 6); // "ABCDEFGHIJ" on 1 row; 'H' at col 7

            printHeader("cursorOnWrappedRowResizeWider — AFTER RESIZE(10,6)");
            tb.debugPrint();

            CursorPosition pos = tb.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isEqualTo(7);
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('H');
        }

        @Test
        @DisplayName("Cursor on straddling line — character in scrollback portion clamped to screen")
        void cursorOnStraddlingLineScrollbackPortion() {
            TerminalBuffer tb = new TerminalBuffer(5, 4, MAX_SCROLLBACK);
            // Write 15 chars → 3 rows at width 5
            tb.write("ABCDEFGHIJKLMNO");
            // Cursor is after last char, set it to beginning
            tb.setScreenCursorPos(0, 1); // pointing at 'B' (logical index 1)

            printHeader("cursorOnStraddlingLineScrollbackPortion — BEFORE PUSH");
            tb.debugPrint();

            // Push twice — the logical line straddles, first 2 rows in scrollback
            tb.pushScreenLine();
            tb.pushScreenLine();

            printHeader("cursorOnStraddlingLineScrollbackPortion — AFTER PUSH x2");
            tb.debugPrint();

            // Now resize — this may move cursor's char into scrollback
            tb.resize(5, 2);

            printHeader("cursorOnStraddlingLineScrollbackPortion — AFTER RESIZE(5,2)");
            tb.debugPrint();

            CursorPosition pos = tb.getScreenCursorPos();
            // Cursor must be on screen
            assertThat(pos.getRow()).isBetween(0, 1);
        }

        @Test
        @DisplayName("Multiple chained resizes — cursor always tracks the character")
        void multipleResizesCursorTracking() {
            TerminalBuffer tb = new TerminalBuffer(10, 10, MAX_SCROLLBACK);
            tb.write("ABCDEFGHIJ");
            tb.setScreenCursorPos(0, 5); // pointing at 'F'

            printHeader("multipleResizesCursorTracking — BEFORE RESIZE CHAIN");
            tb.debugPrint();

            // Resize chain: 10 → 5 → 2 → 10
            tb.resize(5, 10);

            printHeader("multipleResizesCursorTracking — AFTER RESIZE(5,10)");
            tb.debugPrint();

            CursorPosition pos = tb.getScreenCursorPos();
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('F');

            tb.resize(2, 10);

            printHeader("multipleResizesCursorTracking — AFTER RESIZE(2,10)");
            tb.debugPrint();

            pos = tb.getScreenCursorPos();
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('F');

            tb.resize(10, 10);

            printHeader("multipleResizesCursorTracking — AFTER RESIZE(10,10)");
            tb.debugPrint();

            pos = tb.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isEqualTo(5);
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('F');
        }

        @Test
        @DisplayName("Cursor at position (0,0) stays at (0,0) after any resize")
        void cursorAtOriginStaysAfterResize() {
            buffer.write("Hello");
            buffer.setScreenCursorPos(0, 0); // pointing at 'H'

            printHeader("cursorAtOriginStaysAfterResize — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(5, HEIGHT);

            printHeader("cursorAtOriginStaysAfterResize — AFTER RESIZE(5,4)");
            buffer.debugPrint();

            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isEqualTo(0);
            assertThat(buffer.getScreenCell(0, 0).getCharacter()).isEqualTo('H');
        }

        @Test
        @DisplayName("Cursor at last character — resize narrower tracks it")
        void cursorAtLastCharResizeNarrower() {
            TerminalBuffer tb = new TerminalBuffer(10, 6, MAX_SCROLLBACK);
            tb.write("ABCDEFGHIJ");
            tb.setScreenCursorPos(0, 9); // pointing at 'J'

            printHeader("cursorAtLastCharResizeNarrower — BEFORE RESIZE");
            tb.debugPrint();

            tb.resize(5, 6);

            printHeader("cursorAtLastCharResizeNarrower — AFTER RESIZE(5,6)");
            tb.debugPrint();

            CursorPosition pos = tb.getScreenCursorPos();
            // 'J' at logical index 9, width 5 → row 1, col 4
            assertThat(pos.getRow()).isEqualTo(1);
            assertThat(pos.getColumn()).isEqualTo(4);
            assertThat(tb.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('J');
        }

        @Test
        @DisplayName("getScreenCursorPos never returns out-of-bounds after any resize sequence")
        void cursorAlwaysInScreenBounds() {
            // Fill the buffer, move cursor around, resize aggressively
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("Row" + i + "ABCDE");
            }
            buffer.setScreenCursorPos(0, 3); // cursor deep in first row

            printHeader("cursorAlwaysInScreenBounds — BEFORE RESIZE CHAIN");
            buffer.debugPrint();

            // Resize to many different dimensions and verify cursor stays in bounds
            int[][] sizes = {
                {5, 2}, {3, 1}, {20, 10}, {1, 1}, {WIDTH, HEIGHT}, {7, 3}, {2, 2}
            };

            for (int[] size : sizes) {
                int w = size[0];
                int h = size[1];
                buffer.resize(w, h);

                CursorPosition pos = buffer.getScreenCursorPos();
                assertThat(pos.getRow())
                        .as("row after resize(%d,%d)", w, h)
                        .isBetween(0, h - 1);
                assertThat(pos.getColumn())
                        .as("column after resize(%d,%d)", w, h)
                        .isBetween(0, w - 1);
            }

            printHeader("cursorAlwaysInScreenBounds — AFTER RESIZE CHAIN");
            buffer.debugPrint();
        }

        @Test
        @DisplayName("setScreenCursorPos and getScreenCursorPos never expose scrollback rows")
        void cursorNeverExposesScrollback() {
            // Create scrollback
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setScreenCursorPos(i, 0);
                buffer.write("Line" + i);
            }

            printHeader("cursorNeverExposesScrollback — BEFORE PUSH");
            buffer.debugPrint();

            for (int i = 0; i < 5; i++) {
                buffer.pushScreenLine();
            }

            printHeader("cursorNeverExposesScrollback — AFTER PUSH x5");
            buffer.debugPrint();

            // Try setting cursor to every screen position — all should succeed
            for (int row = 0; row < HEIGHT; row++) {
                for (int col = 0; col < WIDTH; col++) {
                    buffer.setScreenCursorPos(row, col);
                    CursorPosition pos = buffer.getScreenCursorPos();
                    assertThat(pos.getRow()).isBetween(0, HEIGHT - 1);
                    assertThat(pos.getColumn()).isBetween(0, WIDTH - 1);
                }
            }

            // Negative and overflow values should be clamped to screen bounds
            buffer.setScreenCursorPos(-1, -1);
            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isEqualTo(0);

            buffer.setScreenCursorPos(999, 999);
            pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(HEIGHT - 1);
            assertThat(pos.getColumn()).isEqualTo(WIDTH - 1);
        }

        @Test
        @DisplayName("Write after resize uses screen-space cursor correctly")
        void writeAfterResizeUsesScreenCursor() {
            buffer.write("ABCDEFGHIJ");
            buffer.setScreenCursorPos(0, 5); // pointing at 'F'

            printHeader("writeAfterResizeUsesScreenCursor — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(5, 6);

            printHeader("writeAfterResizeUsesScreenCursor — AFTER RESIZE(5,6)");
            buffer.debugPrint();

            // Cursor should track 'F' — now at row 1, col 0
            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(buffer.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('F');

            // Write at current cursor position — should overwrite 'F' onward
            buffer.write("XY");

            printHeader("writeAfterResizeUsesScreenCursor — AFTER WRITE");
            buffer.debugPrint();

            assertThat(buffer.getScreenCell(pos.getRow(), pos.getColumn()).getCharacter()).isEqualTo('X');
            assertThat(buffer.getScreenCell(pos.getRow(), pos.getColumn() + 1).getCharacter()).isEqualTo('Y');
        }

        @Test
        @DisplayName("moveScreenCursor after resize stays in screen bounds")
        void moveAfterResize() {
            buffer.write("ABCDEFGHIJ");
            buffer.setScreenCursorPos(0, 5);

            printHeader("moveAfterResize — BEFORE RESIZE");
            buffer.debugPrint();

            buffer.resize(5, 6);

            printHeader("moveAfterResize — AFTER RESIZE(5,6)");
            buffer.debugPrint();

            // Move up — should clamp at row 0
            buffer.moveScreenCursor(TerminalBuffer.CursorMove.UP, 100);
            CursorPosition pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(0);
            assertThat(pos.getColumn()).isBetween(0, 4);

            // Move down — should clamp at height-1
            buffer.moveScreenCursor(TerminalBuffer.CursorMove.DOWN, 100);
            pos = buffer.getScreenCursorPos();
            assertThat(pos.getRow()).isEqualTo(5);
            assertThat(pos.getColumn()).isBetween(0, 4);

            printHeader("moveAfterResize — AFTER MOVES");
            buffer.debugPrint();
        }
    }

    /** Helper: print a section header with box-drawing borders. */
    private static void printHeader(String text) {
        int padding = 4;
        int innerWidth = text.length() + padding;
        System.out.println();
        System.out.println("╔" + "═".repeat(innerWidth) + "╗");
        System.out.println("║  " + text + "  ║");
        System.out.println("╚" + "═".repeat(innerWidth) + "╝");
    }
}
