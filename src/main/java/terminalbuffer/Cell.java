package terminalbuffer;

/**
 * Represents a single character cell in the terminal grid.
 * Each cell holds a character (or '\0' for empty) and its display attributes.
 */
public class Cell {

    private char character;
    private Attributes attributes;

    /**
     * Creates an empty cell with default attributes.
     */
    public Cell() {
        this.character = '\0';
        this.attributes = Attributes.defaultAttributes();
    }

    /**
     * Creates a cell with the given character and attributes.
     *
     * @param character  the character to display ('\0' for empty)
     * @param attributes display attributes (non-null)
     */
    public Cell(char character, Attributes attributes) {
        this.character = character;
        this.attributes = attributes;
    }

    public Cell(Cell other) {
        this.character = other.character;
        this.attributes = other.attributes;  // safe — Attributes is immutable
    }

    public char getCharacter() {
        return character;
    }

    public void setCharacter(char character) {
        this.character = character;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    /**
     * Returns true if this cell has no visible character.
     */
    public boolean isEmpty() {
        return character == '\0';
    }

    /**
     * Resets the cell to empty with default attributes.
     */
    public void clear() {
        this.character = '\0';
        this.attributes = Attributes.defaultAttributes();
    }

    @Override
    public String toString() {
        return isEmpty() ? " " : String.valueOf(character);
    }
}
