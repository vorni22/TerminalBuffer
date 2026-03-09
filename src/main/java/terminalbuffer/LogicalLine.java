package terminalbuffer;

import java.util.ArrayList;

public final class LogicalLine {
    private int screenLineWidth;
    private ArrayList<Cell> cells;
    private boolean userCreated;

    public LogicalLine(int screenLineWidth) {
        this.screenLineWidth = screenLineWidth;
        cells = new ArrayList<>();
        this.userCreated = false;
    }

    /* Get the cell at the logicalLineIndex */
    public Cell getCell(int logicalLineIndex) throws IllegalArgumentException {
        if (logicalLineIndex < 0 || logicalLineIndex >= getLogicalLineLength())
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        return cells.get(logicalLineIndex);
    }

    /* Write text beginning at logicalLineIndex, overwriting content. Extends the line if needed. */
    public void writeAt(int logicalLineIndex, String text) throws IllegalArgumentException {
        if (logicalLineIndex < 0)
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        for (int i = 0; i < text.length(); i++) {
            ensureCapacity(logicalLineIndex);
            cells.get(logicalLineIndex).setCharacter(text.charAt(i));
            logicalLineIndex++;
        }
    }

    /* Write text beginning at logicalLineIndex, pushing old content further right. */
    public void insertAt(int logicalLineIndex, String text) throws IllegalArgumentException {
        if (logicalLineIndex < 0 || logicalLineIndex >= getLogicalLineLength())
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        // Trim trailing empty cells before inserting to avoid inflating the line
        trimTrailingEmptyCells();

        // If logicalLineIndex is now beyond the trimmed length, just write instead
        if (logicalLineIndex >= cells.size()) {
            writeAt(logicalLineIndex, text);
            return;
        }

        ArrayList<Cell> newCells = new ArrayList<>(text.length());
        for (int i = 0; i < text.length(); i++) {
            Cell cell = new Cell();
            cell.setCharacter(text.charAt(i));
            newCells.add(cell);
        }

        cells.addAll(logicalLineIndex, newCells);
        padToRowBoundary();
    }

    /* Cut the current line to [0, logicalLineIndex], inclusive */
    public void cutAt(int logicalLineIndex) throws IllegalArgumentException {
        if (logicalLineIndex < 0 || logicalLineIndex >= getLogicalLineLength())
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        if (logicalLineIndex + 1 < cells.size()) {
            cells.subList(logicalLineIndex + 1, cells.size()).clear();
        }

        padToRowBoundary();
    }

    /* Split the current LogicalLine into 2 LogicalLines:
        1. This LogicalLine keeps [0, logicalLineIndex), exclusive.
        2. Returns a new LogicalLine with [logicalLineIndex, end).
    */
    public LogicalLine splitAt(int logicalLineIndex) throws IllegalArgumentException {
        if (logicalLineIndex < 0 || logicalLineIndex >= getLogicalLineLength())
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        LogicalLine other = new LogicalLine(screenLineWidth);
        other.cells = new ArrayList<>(cells.subList(logicalLineIndex, cells.size()));

        if (logicalLineIndex > 0) {
            cells.subList(logicalLineIndex, cells.size()).clear();
            padToRowBoundary();
        } else {
            cells.clear();
        }

        other.padToRowBoundary();
        return other;
    }

    /* Keep the first n screen rows and return the rest as a new LogicalLine */
    public LogicalLine trimLogicalLine(int n) {
        if (n < 0)
            throw new IllegalArgumentException("n must be non-negative.");

        if (n >= getScreenLineCount()) {
            return new LogicalLine(screenLineWidth);
        }

        LogicalLine other = new LogicalLine(screenLineWidth);

        int splitIndex = n * screenLineWidth;

        if (n == 0) {
            other.cells = this.cells;
            this.cells = new ArrayList<>();
            return other;
        }

        other.cells = new ArrayList<>(cells.subList(splitIndex, cells.size()));
        cells.subList(splitIndex, cells.size()).clear();

        return other;
    }

    /* Resize this line to the new screen width — O(1). */
    public void resize(int newScreenLineWidth) {
        if (newScreenLineWidth <= 0)
            throw new IllegalArgumentException("newScreenLineWidth must be positive.");

        this.screenLineWidth = newScreenLineWidth;
        padToRowBoundary();
    }

    /* Obtain a string representation of the line between [start, end) */
    public String getSubString(int logicalLineIndexStart, int logicalLineIndexEnd) {
        if (logicalLineIndexStart >= logicalLineIndexEnd)
            throw new IllegalArgumentException("logicalLineIndexStart must be smaller than logicalLineIndexEnd.");

        if (logicalLineIndexStart < 0 || logicalLineIndexEnd > getLogicalLineLength())
            throw new IllegalArgumentException("Arguments out of bounds.");

        StringBuilder sb = new StringBuilder();
        for (int i = logicalLineIndexStart; i < logicalLineIndexEnd; i++) {
            char value = cells.get(i).getCharacter();
            sb.append(value == '\0' ? ' ' : value);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        if (cells.isEmpty()) return "";
        return getSubString(0, getLogicalLineLength());
    }

    public int getLogicalLineLength() {
        return cells.size();
    }

    public int getScreenLineCount() {
        if (cells.isEmpty()) return 0;
        return (cells.size() + screenLineWidth - 1) / screenLineWidth;
    }

    public int getScreenLineWidth() {
        return screenLineWidth;
    }

    /* Get a cell by screen coordinates (row, column) */
    public Cell getCellAt(int screenRow, int screenColumn) {
        int index = screenRow * screenLineWidth + screenColumn;
        return getCell(index);
    }

    /* Fill the entire screen row with a character. Current attributes will apply. */
    public void fillRow(int screenRow, char value) {
        if (screenRow < 0)
            throw new IllegalArgumentException("screenRow must be non-negative.");

        int startIndex = screenRow * screenLineWidth;
        ensureCapacity(startIndex + screenLineWidth - 1);

        for (int col = 0; col < screenLineWidth; col++) {
            Cell cell = new Cell();
            cell.setCharacter(value);
            cells.set(startIndex + col, cell);
        }
    }

    /* Fill the entire logical line with a value. Current attributes will apply. */
    public void fillAll(char value) {
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = new Cell();
            cell.setCharacter(value);
            cells.set(i, cell);
        }
    }

    /**
     * Ensure the cells list has at least (index + 1) elements,
     * padding with empty cells to the next full row boundary.
     */
    private void ensureCapacity(int index) {
        if (index < cells.size()) return;

        // Calculate which row this index falls on, then pad to end of that row
        int neededRows = (index / screenLineWidth) + 1;
        int neededSize = neededRows * screenLineWidth;

        while (cells.size() < neededSize) {
            cells.add(new Cell());
        }
    }

    /**
     * Pad or trim the cells list so its size is a multiple of screenLineWidth.
     * Trailing completely empty rows beyond the last non-empty cell are trimmed.
     */
    private void padToRowBoundary() {
        if (cells.isEmpty()) return;

        int remainder = cells.size() % screenLineWidth;
        if (remainder != 0) {
            int padding = screenLineWidth - remainder;
            for (int i = 0; i < padding; i++) {
                cells.add(new Cell());
            }
        }
    }

    /**
     * Remove trailing cells that are empty ('\0') from the cells list.
     */
    private void trimTrailingEmptyCells() {
        int last = cells.size() - 1;
        while (last >= 0 && cells.get(last).getCharacter() == '\0') {
            last--;
        }
        if (last + 1 < cells.size()) {
            cells.subList(last + 1, cells.size()).clear();
        }
    }

    public boolean isUserCreated() {
        return userCreated;
    }

    public void setUserCreated(boolean userCreated) {
        this.userCreated = userCreated;
    }
}
