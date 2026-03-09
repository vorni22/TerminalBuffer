package terminalbuffer;

import java.util.ArrayList;

/**
 * Core data structure for a terminal emulator text buffer.
 */
public class TerminalBuffer {
    private int maxScrollbackSize;
    private int width;
    private int height;

    /* cursor in screen space */
    private CursorPosition screenSpaceCursor;

    /* cursor in logical space — row is index into lines[], column is logical cell index */
    private CursorPosition logicalSpaceCursor;

    /* all logical lines: scrollback + screen in one list */
    private ArrayList<LogicalLine> lines;

    /* 
     * Index into lines[] where the screen region begins.
     * Lines [0, screenStartIndex) are fully in scrollback.
     * The line at screenStartIndex may straddle the boundary.
     */
    private int screenStartIndex;

    /*
     * The screen row offset within lines[screenStartIndex].
     * Screen rows of this logical line before this offset are scrollback.
     * Screen rows from this offset onward are visible screen.
     * 0 means the entire line is on screen.
     */
    private int screenStartRowOffset;

    /* -------------------- Setup -------------------- */

    public TerminalBuffer(int width, int height, int maxScrollbackSize) {
        this.width = width;
        this.height = height;
        this.maxScrollbackSize = maxScrollbackSize;

        this.screenSpaceCursor = new CursorPosition(0, 0);
        this.logicalSpaceCursor = new CursorPosition(0, 0);

        lines = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            lines.add(new LogicalLine(width));
        }
        screenStartIndex = 0;
        screenStartRowOffset = 0;
    }

    /* -------------------- Editing and Content Access -------------------- */

    /* 
        Resize the buffer to accommodate the new screen size. The lines in the scrollback will also be adjusted.
        If, after resizing, there is not enough space to fit all the lines, some will move to the scrollback.
        If there is still not enough space to fit all the lines in the scrollback, some will be erased.
    */  
    public void resize(int newWidth, int newHeight) {
        for (LogicalLine line : lines) {
            line.resize(newWidth);
        }

        this.width = newWidth;
        this.height = newHeight;

        rebalanceScreen();
        enforceScrollbackLimit();

        clampCursor();
        syncLogicalCursorFromScreen();
    }

    /* Get the cell at row and column (screen space) that are on the Screen part*/
    public Cell getScreenCell(int screenRow, int screenColumn) {
        if (screenColumn < 0 || screenColumn >= width)
            throw new IllegalArgumentException("screenColumn out of bounds");
        if (screenRow < 0 || screenRow >= height)
            throw new IllegalArgumentException("screenRow out of bounds");

        int[] resolved = resolveScreenRow(screenRow);
        int lineIdx = resolved[0];
        int rowWithinLine = resolved[1];
        LogicalLine line = lines.get(lineIdx);

        if (line.getLogicalLineLength() == 0) {
            return new Cell();
        }

        int logicalIndex = rowWithinLine * width + screenColumn;
        if (logicalIndex >= line.getLogicalLineLength()) {
            return new Cell();
        }

        return line.getCellAt(rowWithinLine, screenColumn);
    }

    /* Get the cell at row and column (screen space) that are on the Scrollback part*/
    public Cell getScrollbackCell(int screenRow, int screenColumn) {
        if (screenColumn < 0 || screenColumn >= width)
            throw new IllegalArgumentException("screenColumn out of bounds");

        int[] resolved = resolveScrollbackRow(screenRow);
        int lineIdx = resolved[0];
        int rowWithinLine = resolved[1];
        LogicalLine line = lines.get(lineIdx);

        if (line.getLogicalLineLength() == 0) {
            return new Cell();
        }

        int logicalIndex = rowWithinLine * width + screenColumn;
        if (logicalIndex >= line.getLogicalLineLength()) {
            return new Cell();
        }

        return line.getCellAt(rowWithinLine, screenColumn);
    }

    /* 
        Write text (without special characters) starting at the cursor position on the current logical line.
        The cursor will be moved. 
        Content will be overwritten.
    */
    public void write(String text) {
        if (!text.isEmpty()) {
            int lineIdx = logicalSpaceCursor.getRow();
            int logCol = logicalSpaceCursor.getColumn();
            LogicalLine line = lines.get(lineIdx);
            line.writeAt(logCol, text);

            logCol += text.length();
            logicalSpaceCursor.setColumn(logCol);
            rebalanceScreen();
            enforceScrollbackLimit();
            syncScreenCursorFromLogical();
        }
    }

    /*
        Insert text (without special characters) starting at the cursor position on the current logical line.
        The cursor will be moved.
        Content will be moved, not overwritten.
    */
    public void insert(String text) {
        if (!text.isEmpty()) {
            int lineIdx = logicalSpaceCursor.getRow();
            int logCol = logicalSpaceCursor.getColumn();
            LogicalLine line = lines.get(lineIdx);

            if (logCol >= line.getLogicalLineLength()) {
                line.writeAt(logCol, text);
            } else {
                line.insertAt(logCol, text);
            }

            logCol += text.length();
            logicalSpaceCursor.setColumn(logCol);
            rebalanceScreen();
            enforceScrollbackLimit();
            syncScreenCursorFromLogical();
        }
    }

    /* Fill the current screen line with a character */
    public void fillScreenLine(char value) {
        int lineIdx = logicalSpaceCursor.getRow();
        int logCol = logicalSpaceCursor.getColumn();
        int rowOffset = logCol / width;
        LogicalLine line = lines.get(lineIdx);
        line.fillRow(rowOffset, value);
    }

    /* Clear the Screen part. */
    public void clearScreen() {
        // Remove all lines from screenStartIndex onward
        while (lines.size() > screenStartIndex) {
            lines.remove(lines.size() - 1);
        }

        // If there was a straddling line, it's now fully scrollback — already kept
        screenStartRowOffset = 0;

        // Add fresh empty lines for the screen
        for (int i = 0; i < height; i++) {
            lines.add(new LogicalLine(width));
        }
        screenStartIndex = lines.size() - height;

        screenSpaceCursor.setRow(0);
        screenSpaceCursor.setColumn(0);
        logicalSpaceCursor.setRow(screenStartIndex);
        logicalSpaceCursor.setColumn(0);
    }

    /* Clear the Screen and Scrollback part. */
    public void clearAll() {
        lines.clear();
        screenStartIndex = 0;
        screenStartRowOffset = 0;

        for (int i = 0; i < height; i++) {
            lines.add(new LogicalLine(width));
        }

        screenSpaceCursor.setRow(0);
        screenSpaceCursor.setColumn(0);
        logicalSpaceCursor.setRow(0);
        logicalSpaceCursor.setColumn(0);
    }

    /* Push a screen line at the end, in consequence the first screen line will be moved to Scrollback. */
    public void pushScreenLine() {
        LogicalLine newLine = new LogicalLine(width);
        newLine.setUserCreated(true);
        lines.add(newLine);
        rebalanceScreen();
        enforceScrollbackLimit();
    }

    /* Get the current screen line as a String. */
    public String screenLineToString() {
        int lineIdx = logicalSpaceCursor.getRow();
        int logCol = logicalSpaceCursor.getColumn();
        int rowOffset = logCol / width;

        LogicalLine line = lines.get(lineIdx);
        if (line.getLogicalLineLength() == 0) {
            return " ".repeat(width);
        }

        int start = rowOffset * width;
        int end = Math.min(start + width, line.getLogicalLineLength());
        if (start >= line.getLogicalLineLength()) {
            return " ".repeat(width);
        }

        String content = line.getSubString(start, end);
        if (content.length() < width) {
            content = content + " ".repeat(width - content.length());
        }
        return content;
    }

    /* Get Screen part represented as a String. */
    public String screenToString() {
        return regionToString(screenStartIndex, screenStartRowOffset, lines.size(), height);
    }

    /* Get the Screen Part and Scrollback represented as a String. */
    public String screenAndScrollbackToString() {
        StringBuilder sb = new StringBuilder();

        // Scrollback region
        int scrollbackRows = totalScrollbackRows();
        if (scrollbackRows > 0) {
            String scrollbackStr = regionToString(0, 0, screenStartIndex + (screenStartRowOffset > 0 ? 1 : 0), scrollbackRows);
            sb.append(scrollbackStr);
        }

        // Screen region
        String screenStr = screenToString();
        if (sb.length() > 0 && !screenStr.isEmpty()) {
            sb.append('\n');
        }
        sb.append(screenStr);

        return sb.toString();
    }

    /* Equivalent with screenAndScrollbackToString. */
    public String toString() {
        return screenAndScrollbackToString();
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
        screenSpaceCursor.setRow(Math.max(0, Math.min(screenRow, height - 1)));
        screenSpaceCursor.setColumn(Math.max(0, Math.min(screenColumn, width - 1)));
        syncLogicalCursorFromScreen();
    }    

    /* get the cursor position in screen space */
    public CursorPosition getScreenCursorPos() {
        return new CursorPosition(screenSpaceCursor.getColumn(), screenSpaceCursor.getRow());
    }

    /* move the cursor through screen space. */
    public void moveScreenCursor(CursorMove moveType, int screenDelta) {
        int row = screenSpaceCursor.getRow();
        int col = screenSpaceCursor.getColumn();
        switch (moveType) {
            case UP -> row -= screenDelta;
            case DOWN -> row += screenDelta;
            case LEFT -> col -= screenDelta;
            case RIGHT -> col += screenDelta;
        }
        setScreenCursorPos(row, col);
    }

    /* -------------------- Attributes -------------------- */
    
    /* set current attributes */
    public void setAttributes(Attributes attributes) {
        Cell.setCurrentAttributes(attributes);
    }

    /* -------------------- Private helpers -------------------- */

    /**
     * Count screen rows in the screen region.
     */
    private int totalScreenRows() {
        int count = 0;
        for (int i = screenStartIndex; i < lines.size(); i++) {
            int lineRows = Math.max(lines.get(i).getScreenLineCount(), 1);
            if (i == screenStartIndex) {
                count += lineRows - screenStartRowOffset;
            } else {
                count += lineRows;
            }
        }
        return count;
    }

    /**
     * Count screen rows in the scrollback region.
     */
    private int totalScrollbackRows() {
        int count = 0;
        for (int i = 0; i < screenStartIndex; i++) {
            count += Math.max(lines.get(i).getScreenLineCount(), 1);
        }
        // Add the straddling portion
        if (screenStartRowOffset > 0 && screenStartIndex < lines.size()) {
            count += screenStartRowOffset;
        }
        return count;
    }

    /**
     * Resolve a screen-space row index to (lineIndex in lines[], absolute rowWithinLine).
     */
    private int[] resolveScreenRow(int screenRow) {
        int accumulated = 0;
        for (int i = screenStartIndex; i < lines.size(); i++) {
            int lineRows = Math.max(lines.get(i).getScreenLineCount(), 1);
            int visibleStart = (i == screenStartIndex) ? screenStartRowOffset : 0;
            int visibleRows = lineRows - visibleStart;

            if (screenRow < accumulated + visibleRows) {
                int rowWithinLine = visibleStart + (screenRow - accumulated);
                return new int[]{i, rowWithinLine};
            }
            accumulated += visibleRows;
        }
        throw new IllegalArgumentException("screenRow " + screenRow + " out of bounds (total: " + accumulated + ")");
    }

    /**
     * Resolve a scrollback screen-space row index to (lineIndex in lines[], absolute rowWithinLine).
     */
    private int[] resolveScrollbackRow(int scrollbackRow) {
        int accumulated = 0;
        // Lines fully in scrollback
        for (int i = 0; i < screenStartIndex; i++) {
            int lineRows = Math.max(lines.get(i).getScreenLineCount(), 1);
            if (scrollbackRow < accumulated + lineRows) {
                return new int[]{i, scrollbackRow - accumulated};
            }
            accumulated += lineRows;
        }
        // Straddling line's scrollback portion
        if (screenStartRowOffset > 0 && screenStartIndex < lines.size()) {
            if (scrollbackRow < accumulated + screenStartRowOffset) {
                return new int[]{screenStartIndex, scrollbackRow - accumulated};
            }
        }
        throw new IllegalArgumentException("scrollbackRow out of bounds");
    }

    /**
     * Move excess screen rows into scrollback by advancing screenStartIndex/screenStartRowOffset.
     */
    private void rebalanceScreen() {
        // Push rows into scrollback if screen has too many.
        // First, try to remove trailing empty lines before pushing content to scrollback.
        while (totalScreenRows() > height) {
            // Try to remove empty lines from the bottom first
            if (removeTrailingEmptyLine()) {
                continue;
            }

            int excess = totalScreenRows() - height;
            LogicalLine borderLine = lines.get(screenStartIndex);
            int lineRows = Math.max(borderLine.getScreenLineCount(), 1);
            int visibleRows = lineRows - screenStartRowOffset;

            if (visibleRows <= excess) {
                // Entire visible part of this line moves to scrollback
                screenStartRowOffset = 0;
                screenStartIndex++;

                // Adjust logical cursor
                if (logicalSpaceCursor.getRow() >= screenStartIndex) {
                    // cursor is still on screen, no change needed
                } else if (logicalSpaceCursor.getRow() == screenStartIndex - 1) {
                    // cursor was on the line that just moved fully to scrollback
                    logicalSpaceCursor.setRow(screenStartIndex);
                    logicalSpaceCursor.setColumn(0);
                }
            } else {
                // Only part of this line moves to scrollback
                screenStartRowOffset += excess;

                // Adjust logical cursor if it was in the scrolled-off portion
                if (logicalSpaceCursor.getRow() == screenStartIndex) {
                    int cursorRowInLine = logicalSpaceCursor.getColumn() / width;
                    if (cursorRowInLine < screenStartRowOffset) {
                        logicalSpaceCursor.setColumn(screenStartRowOffset * width);
                    }
                }
                break;
            }
        }

        // If screen has fewer rows than height:
        // Only reclaim from scrollback if the border logical line is divided.
        // Otherwise, add new empty lines.
        while (totalScreenRows() < height) {
            if (screenStartRowOffset > 0) {
                int deficit = height - totalScreenRows();
                int reclaimable = Math.min(deficit, screenStartRowOffset);
                screenStartRowOffset -= reclaimable;
            } else {
                lines.add(new LogicalLine(width));
            }
        }
    }

    /**
     * Enforce the maximum scrollback size by removing oldest lines.
     */
    private void enforceScrollbackLimit() {
        while (totalScrollbackRows() > maxScrollbackSize) {
            if (screenStartIndex > 0) {
                LogicalLine oldest = lines.get(0);
                int oldestRows = Math.max(oldest.getScreenLineCount(), 1);
                int excess = totalScrollbackRows() - maxScrollbackSize;

                if (oldestRows <= excess) {
                    lines.remove(0);
                    screenStartIndex--;

                    // Adjust logical cursor
                    if (logicalSpaceCursor.getRow() > 0) {
                        logicalSpaceCursor.setRow(logicalSpaceCursor.getRow() - 1);
                    }
                } else {
                    // Trim from top of oldest line
                    LogicalLine kept = oldest.trimLogicalLine(excess);
                    lines.set(0, kept);
                    break;
                }
            } else if (screenStartRowOffset > 0) {
                // The only scrollback is the straddling portion
                int excess = totalScrollbackRows() - maxScrollbackSize;
                if (excess >= screenStartRowOffset) {
                    // Discard all scrollback from the straddling line
                    LogicalLine straddling = lines.get(0);
                    LogicalLine kept = straddling.trimLogicalLine(screenStartRowOffset);
                    lines.set(0, kept);
                    screenStartRowOffset = 0;
                } else {
                    // Trim some rows from the straddling line's scrollback portion
                    LogicalLine straddling = lines.get(0);
                    LogicalLine kept = straddling.trimLogicalLine(excess);
                    lines.set(0, kept);
                    screenStartRowOffset -= excess;
                }
                break;
            } else {
                break;
            }
        }
    }

    /**
     * Sync screen cursor from logical cursor.
     */
    private void syncScreenCursorFromLogical() {
        int lineIdx = logicalSpaceCursor.getRow();
        int logCol = logicalSpaceCursor.getColumn();

        // If cursor's logical line is in scrollback, push it to the first screen position
        if (lineIdx < screenStartIndex) {
            screenSpaceCursor.setRow(0);
            screenSpaceCursor.setColumn(0);
            logicalSpaceCursor.setRow(screenStartIndex);
            logicalSpaceCursor.setColumn(screenStartRowOffset * width);
            return;
        }

        int screenRow = 0;
        for (int i = screenStartIndex; i < lineIdx && i < lines.size(); i++) {
            int lineRows = Math.max(lines.get(i).getScreenLineCount(), 1);
            int visibleStart = (i == screenStartIndex) ? screenStartRowOffset : 0;
            screenRow += lineRows - visibleStart;
        }

        // Add offset within the cursor's logical line
        int rowInLine = logCol / width;
        if (lineIdx == screenStartIndex) {
            screenRow += rowInLine - screenStartRowOffset;
        } else {
            screenRow += rowInLine;
        }
        int screenCol = logCol % width;

        screenSpaceCursor.setRow(Math.max(0, Math.min(screenRow, height - 1)));
        screenSpaceCursor.setColumn(Math.max(0, Math.min(screenCol, width - 1)));
    }

    /**
     * Sync logical cursor from screen cursor.
     */
    private void syncLogicalCursorFromScreen() {
        int screenRow = screenSpaceCursor.getRow();
        int screenCol = screenSpaceCursor.getColumn();

        try {
            int[] resolved = resolveScreenRow(screenRow);
            int lineIdx = resolved[0];
            int rowWithinLine = resolved[1];
            int logCol = rowWithinLine * width + screenCol;

            logicalSpaceCursor.setRow(lineIdx);
            logicalSpaceCursor.setColumn(logCol);
        } catch (IllegalArgumentException e) {
            // Screen row out of bounds — clamp to last valid position
            logicalSpaceCursor.setRow(lines.size() - 1);
            logicalSpaceCursor.setColumn(0);
        }
    }

    /**
     * Clamp screen cursor to valid bounds.
     */
    private void clampCursor() {
        screenSpaceCursor.setRow(Math.max(0, Math.min(screenSpaceCursor.getRow(), height - 1)));
        screenSpaceCursor.setColumn(Math.max(0, Math.min(screenSpaceCursor.getColumn(), width - 1)));
    }

    /**
     * Try to remove a trailing empty logical line from the screen region.
     * Returns true if one was removed, false if there are no removable empty lines.
     * Will not remove a line if the cursor is on it, if it's the only screen line,
     * or if it was explicitly created by the user (e.g. via pushScreenLine).
     */
    private boolean removeTrailingEmptyLine() {
        // Need at least 2 lines in the screen region to remove one
        int screenLineCount = lines.size() - screenStartIndex;
        if (screenLineCount <= 1) {
            return false;
        }

        int lastIdx = lines.size() - 1;
        LogicalLine lastLine = lines.get(lastIdx);

        // Only remove if the line is empty, not user-created, and the cursor is not on it
        if (lastLine.getLogicalLineLength() == 0
                && !lastLine.isUserCreated()
                && logicalSpaceCursor.getRow() != lastIdx) {
            lines.remove(lastIdx);
            return true;
        }

        return false;
    }

    /**
     * Convert a region of logical lines to a string.
     * Starts at lines[fromLineIdx] row fromRowOffset, up to (but not including) lines[toLineIdx].
     * Renders at most maxRows screen rows.
     */
    private String regionToString(int fromLineIdx, int fromRowOffset, int toLineIdx, int maxRows) {
        StringBuilder sb = new StringBuilder();
        int rowCount = 0;

        for (int i = fromLineIdx; i < toLineIdx && i < lines.size() && rowCount < maxRows; i++) {
            LogicalLine line = lines.get(i);
            int lineRows = Math.max(line.getScreenLineCount(), 1);
            int startRow = (i == fromLineIdx) ? fromRowOffset : 0;

            for (int r = startRow; r < lineRows && rowCount < maxRows; r++) {
                if (sb.length() > 0) sb.append('\n');

                if (line.getLogicalLineLength() == 0) {
                    sb.append(" ".repeat(width));
                } else {
                    int start = r * width;
                    int end = Math.min(start + width, line.getLogicalLineLength());
                    if (start >= line.getLogicalLineLength()) {
                        sb.append(" ".repeat(width));
                    } else {
                        String content = line.getSubString(start, end);
                        if (content.length() < width) {
                            content = content + " ".repeat(width - content.length());
                        }
                        sb.append(content);
                    }
                }
                rowCount++;
            }
        }
        return sb.toString();
    }
}
