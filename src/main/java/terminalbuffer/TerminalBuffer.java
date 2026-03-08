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
        throw new UnsupportedOperationException("Not yet implemented");
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
