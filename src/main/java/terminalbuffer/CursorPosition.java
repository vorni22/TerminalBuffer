package terminalbuffer;

/**
 * Represents the cursor position within the terminal screen.
 * Column and row are 0-based.
 */
public class CursorPosition {

    private int column;
    private int row;

    public CursorPosition(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public String toString() {
        return "CursorPosition{col=" + column + ", row=" + row + "}";
    }
}
