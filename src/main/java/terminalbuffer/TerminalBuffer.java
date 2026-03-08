package terminalbuffer;

/**
 * Core data structure for a terminal emulator text buffer.
 */
public class TerminalBuffer {
    private int maxScrollbackSize;
    private int width;
    private int height;

    /* cursor in screen space */
    private CursorPosition screenSpaceCursor;

    /* cursor in logical space */
    private CursorPosition logicalSpaceCursors;

    /* -------------------- Setup -------------------- */

    public TerminalBuffer(int width, int height, int maxScrollbackSize) {
        this.width = width;
        this.height = height;
    }

    /* -------------------- Editing and Content Access -------------------- */

    /* 
        Resize the buffer to accommodate the new screen size. The lines in the scrollback will also be adjusted.
        If, after resizing, there is not enough space to fit all the lines, some will move to the scrollback.
        If there is still not enough space to fit all the lines in the scrollback, some will be erased.
    */  
    public void resize(int width, int height) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Get the cell at row and column (screen space) that are on the Screen part*/
    public Cell getScreenCell(int screenRow, int screenColumn) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Get the cell at row and column (screen space) that are on the Scrollback part*/
    public Cell getScrollbackCell(int screenRow, int screenColumn) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* 
        Write text starting at the cursor position on the current logical line.
        The cursor will be moved. 
        Content will be overwritten.
        If the text will exceed the current logical line, it will be extended.
        The '\n' character will generate new logical lines.
    */
    public void write(String text) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /*
        Insert text starting at the cursor position on the current logical line.
        The cursor will be moved.
        Content will be moved, not overwritten.
        The '\n' character will generate new logical lines.
    */
    public void insert(String text) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Fill the current logical line with a character. */
    public void fillLogicalLine(char value) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Fill the current screen line with a character */
    public void fillScreenlLine(char value) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Clear the Screen part. */
    public void clearScreen() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Clear the Screen and Scrollback part. */
    public void clearAll() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Push a screen line at the end, in consequence the first screen line will be moved to Scrollback. */
    public void pushScreenLine() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Get the current screen line as a String. */
    public String screenLineToString() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /*
        Get the current logical line as a String. 
        If the logical line contains multiple screen lines, the contents in this string will be separated by a '\n'. 
    */
    public String logicalLineToString() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Get Screen part represented as a String. */
    public String screenToString() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Get the Screen Part and Scrollback represented as a String. */
    public StrictMath screenAndScrollbackToString() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* Equivalent with screenAndScrollbackToString. */
    public String toString() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* -------------------- Cursor -------------------- */

    public enum CursorMove {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    /* set the cursor position in screen space */
    public void setScreenCursorPos(int screenRow, int screenColumn) {
        throw new UnsupportedOperationException("Not yet implemented");
    }    

    /* set the cursor position in logical space */
    public void setLogicalCursorPos(int logicalRow, int logicalColumn) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* get the cursor position in screen space */
    public void getScreenCursorPos(int screenRow, int screenColumn) {
        throw new UnsupportedOperationException("Not yet implemented");
    }    

    /* get the cursor position in logical space */
    public void getLogicalCursorPos(int logicalRow, int logicalColumn) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* move the cursor through logical space. */
    public void moveLogicalCursor(CursorMove moveType, int logicalDelta) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* move the cursor through screen space. */
    public void moveScreenCursor(CursorMove moveType, int screenDelta) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /* -------------------- Attributes -------------------- */
    
    /* set current attributes */
    public void setAttributes(Attributes attributes) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
