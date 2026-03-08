package terminalbuffer;

public final class ScreenLine {
    private final int lineWidth;
    private Cell[] cells;

    public ScreenLine(int lineWidth) {
        this.lineWidth = lineWidth;
        cells = new Cell[this.lineWidth];
        for (int i = 0; i < lineWidth; i++) {
            cells[i] = new Cell();
        }
    }

    Cell getCell(int screenColumn) {
        return cells[screenColumn];
    }

    void setCell(int screenColumn, Cell cell) {
        cells[screenColumn] = cell;
    }
}
