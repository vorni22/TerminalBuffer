package terminalbuffer;

/**
 * Represents a single character cell in the terminal grid.
 * Each cell holds a character (or '\0' for empty) and its display attributes.
 */
public class Cell {

    private static Attributes currentAttributes = Attributes.defaultAttributes();

    private char character;
    private Attributes attributes;

    /**
     * Creates an empty cell with the current static attributes.
     */
    public Cell() {
        this.character = '\0';
        this.attributes = currentAttributes;
    }

    /**
     * Creates a cell with the given character and the current static attributes.
     *
     * @param character the character to display ('\0' for empty)
     */
    public Cell(char character) {
        this.character = character;
        this.attributes = currentAttributes;
    }

    public Cell(Cell other) {
        this.character = other.character;
        this.attributes = other.attributes;  // safe — Attributes is immutable
    }

    /**
     * Set the static attributes used for all new Cell creations.
     */
    public static void setCurrentAttributes(Attributes attributes) {
        currentAttributes = attributes;
    }

    /**
     * Get the static attributes currently used for new Cell creations.
     */
    public static Attributes getCurrentAttributes() {
        return currentAttributes;
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
