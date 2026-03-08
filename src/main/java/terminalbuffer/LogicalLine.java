package terminalbuffer;

import java.util.ArrayList;

public final class LogicalLine {
    private int screenLineWidth;
    ArrayList<ScreenLine> screenLines;

    public LogicalLine(int screenLineWidth) {
        this.screenLineWidth = screenLineWidth;
        screenLines = new ArrayList<>();
    }

    /* Get the cell at the logicalLineIndex */
    public Cell getCell(int logicalLineIndex) throws IllegalArgumentException {
        if (logicalLineIndex < 0 || logicalLineIndex >= getLogicalLineLength())
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        int row = getScreenRow(logicalLineIndex);
        int column = getScreenColumn(logicalLineIndex);

        return screenLines.get(row).getCell(column);
    }

    /* Write text beginning at logicalLineIndex, overwriting content. Extends the line if needed. */
    public void writeAt(int logicalLineIndex, String text) throws IllegalArgumentException {
        if (logicalLineIndex < 0)
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        for (int i = 0; i < text.length(); i++) {
            setCellChar(logicalLineIndex, text.charAt(i));
            logicalLineIndex++;
        }
    }

    /* Write text beginning at logicalLineIndex, pushing old content further right. */
    public void insertAt(int logicalLineIndex, String text) throws IllegalArgumentException {
        if (logicalLineIndex < 0 || logicalLineIndex >= getLogicalLineLength())
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        // Shift existing content right — iterate backwards to avoid overwriting
        int oldLength = getLogicalLineLength();
        for (int index = oldLength - 1; index >= logicalLineIndex; index--) {
            setCell(index + text.length(), new Cell(getCell(index)));
        }

        // Write the new text
        for (int i = 0; i < text.length(); i++) {
            setCellChar(logicalLineIndex + i, text.charAt(i));
        }
    }

    /* Cut the current line to [0, logicalLineIndex], inclusive */
    public void cutAt(int logicalLineIndex) throws IllegalArgumentException {
        if (logicalLineIndex < 0 || logicalLineIndex >= getLogicalLineLength())
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        // Clear cells after the cut point on the same screen line
        for (int i = logicalLineIndex + 1; i < getLogicalLineLength(); i++) {
            int row = getScreenRow(i);
            int col = getScreenColumn(i);
            screenLines.get(row).getCell(col).clear();
        }

        // Remove any screen lines fully beyond the cut point
        int keepRows = getScreenRow(logicalLineIndex) + 1;
        if (keepRows < screenLines.size()) {
            screenLines.subList(keepRows, screenLines.size()).clear();
        }
    }

    /* Split the current LogicalLine into 2 LogicalLines:
        1. This LogicalLine keeps [0, logicalLineIndex), exclusive.
        2. Returns a new LogicalLine with [logicalLineIndex, end).
    */
    public LogicalLine splitAt(int logicalLineIndex) throws IllegalArgumentException {
        if (logicalLineIndex < 0 || logicalLineIndex >= getLogicalLineLength())
            throw new IllegalArgumentException("logicalLineIndex out of bounds.");

        LogicalLine other = new LogicalLine(screenLineWidth);
        for (int i = logicalLineIndex; i < getLogicalLineLength(); i++) {
            other.setCell(i - logicalLineIndex, new Cell(getCell(i)));
        }

        if (logicalLineIndex > 0) {
            cutAt(logicalLineIndex - 1);
        } else {
            screenLines.clear();
        }

        return other;
    }

    /* Keep the first n screen rows and return the rest as a new LogicalLine */
    public LogicalLine trimLogicalLine(int n) {
        if (n < 0)
            throw new IllegalArgumentException("n must be non-negative.");

        if (n >= screenLines.size()) {
            return new LogicalLine(screenLineWidth);
        }

        LogicalLine other = new LogicalLine(screenLineWidth);

        if (n == 0) {
            other.screenLines.addAll(this.screenLines);
            this.screenLines.clear();
            return other;
        }

        other.screenLines.addAll(this.screenLines.subList(n, screenLines.size()));
        this.screenLines.subList(n, screenLines.size()).clear();

        return other;
    }

    /* Obtain a string representation of the line between [start, end) */
    public String getSubString(int logicalLineIndexStart, int logicalLineIndexEnd) {
        if (logicalLineIndexStart >= logicalLineIndexEnd)
            throw new IllegalArgumentException("logicalLineIndexStart must be smaller than logicalLineIndexEnd.");

        if (logicalLineIndexStart < 0 || logicalLineIndexEnd > getLogicalLineLength())
            throw new IllegalArgumentException("Arguments out of bounds.");

        StringBuilder sb = new StringBuilder();
        for (int i = logicalLineIndexStart; i < logicalLineIndexEnd; i++) {
            char value = getCell(i).getCharacter();
            sb.append(value == '\0' ? ' ' : value);
        }

        return sb.toString();
    }

    /* Resize this line to the new screen width. */
    public void resize(int newScreenLineWidth) {
        if (newScreenLineWidth <= 0)
            throw new IllegalArgumentException("newScreenLineWidth must be positive.");

        if (screenLines.isEmpty() || newScreenLineWidth == this.screenLineWidth) {
            this.screenLineWidth = newScreenLineWidth;
            return;
        }

        // Collect all cells in logical order, stripping trailing empty rows
        int totalCells = getLogicalLineLength();
        Cell[] allCells = new Cell[totalCells];
        for (int i = 0; i < totalCells; i++) {
            allCells[i] = getCell(i);
        }

        // Find the last non-empty cell to determine how many cells we actually need
        int lastNonEmpty = -1;
        for (int i = totalCells - 1; i >= 0; i--) {
            if (!allCells[i].isEmpty()) {
                lastNonEmpty = i;
                break;
            }
        }

        // Update width
        this.screenLineWidth = newScreenLineWidth;
        screenLines.clear();

        if (lastNonEmpty < 0) {
            // All cells were empty — nothing to rebuild
            return;
        }

        // Rebuild: we need enough rows to hold cells [0, lastNonEmpty]
        int cellsToKeep = lastNonEmpty + 1;
        int newRowCount = (cellsToKeep + newScreenLineWidth - 1) / newScreenLineWidth; // ceil division

        for (int row = 0; row < newRowCount; row++) {
            ScreenLine line = new ScreenLine(newScreenLineWidth);
            for (int col = 0; col < newScreenLineWidth; col++) {
                int idx = row * newScreenLineWidth + col;
                if (idx < cellsToKeep) {
                    line.setCell(col, new Cell(allCells[idx]));
                }
                // else: already initialized to empty Cell by ScreenLine constructor
            }
            screenLines.add(line);
        }
    }

    @Override
    public String toString() {
        if (screenLines.isEmpty()) return "";
        return getSubString(0, getLogicalLineLength());
    }

    public int getLogicalLineLength() {
        return screenLines.size() * this.screenLineWidth;
    }

    public int getScreenLineCount() {
        return screenLines.size();
    }

    private int getScreenRow(int logicalLineIndex) {
        return logicalLineIndex / screenLineWidth;
    }

    private int getScreenColumn(int logicalLineIndex) {
        return logicalLineIndex % screenLineWidth;
    }

    private void setCell(int logicalLineIndex, Cell cell) {
        int row = getScreenRow(logicalLineIndex);
        int column = getScreenColumn(logicalLineIndex);

        while (row >= screenLines.size()) {
            screenLines.add(new ScreenLine(screenLineWidth));
        }

        screenLines.get(row).setCell(column, cell);
    }

    private void setCellChar(int logicalLineIndex, char value) {
        int row = getScreenRow(logicalLineIndex);
        int column = getScreenColumn(logicalLineIndex);

        while (row >= screenLines.size()) {
            screenLines.add(new ScreenLine(screenLineWidth));
        }

        screenLines.get(row).getCell(column).setCharacter(value);
    }
}
