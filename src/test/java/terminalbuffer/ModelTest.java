package terminalbuffer;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the supporting model classes: {@link Cell}, {@link CellAttributes}, {@link CursorPosition}, {@link Color}.
 */
class ModelTest {

    // ─── Color ──────────────────────────────────────────────────────────

    @Test
    void colorEnumHas17Values() {
        // DEFAULT + 16 colors
        assertThat(Color.values()).hasSize(17);
    }

    // ─── CellAttributes ────────────────────────────────────────────────

    @Test
    void defaultAttributesHaveDefaultColorsAndNoStyles() {
        CellAttributes attrs = CellAttributes.defaultAttributes();
        assertThat(attrs.getForeground()).isEqualTo(Color.DEFAULT);
        assertThat(attrs.getBackground()).isEqualTo(Color.DEFAULT);
        assertThat(attrs.getStyles()).isEmpty();
    }

    @Test
    void attributesEqualityByValue() {
        var a = new CellAttributes(Color.RED, Color.BLUE, EnumSet.of(CellAttributes.Style.BOLD));
        var b = new CellAttributes(Color.RED, Color.BLUE, EnumSet.of(CellAttributes.Style.BOLD));
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void attributesInequalityOnDifferentValues() {
        var a = new CellAttributes(Color.RED, Color.DEFAULT, EnumSet.noneOf(CellAttributes.Style.class));
        var b = new CellAttributes(Color.GREEN, Color.DEFAULT, EnumSet.noneOf(CellAttributes.Style.class));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void attributesHasStyle() {
        var attrs = new CellAttributes(Color.DEFAULT, Color.DEFAULT,
                EnumSet.of(CellAttributes.Style.ITALIC, CellAttributes.Style.UNDERLINE));
        assertThat(attrs.hasStyle(CellAttributes.Style.ITALIC)).isTrue();
        assertThat(attrs.hasStyle(CellAttributes.Style.UNDERLINE)).isTrue();
        assertThat(attrs.hasStyle(CellAttributes.Style.BOLD)).isFalse();
    }

    @Test
    void attributesStylesAreImmutableCopy() {
        var attrs = new CellAttributes(Color.DEFAULT, Color.DEFAULT,
                EnumSet.of(CellAttributes.Style.BOLD));
        Set<CellAttributes.Style> styles = attrs.getStyles();
        assertThatThrownBy(() -> styles.add(CellAttributes.Style.ITALIC))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void attributesRejectsNullArguments() {
        assertThatThrownBy(() -> new CellAttributes(null, Color.DEFAULT, EnumSet.noneOf(CellAttributes.Style.class)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CellAttributes(Color.DEFAULT, null, EnumSet.noneOf(CellAttributes.Style.class)))
                .isInstanceOf(NullPointerException.class);
    }

    // ─── Cell ───────────────────────────────────────────────────────────

    @Test
    void newCellIsEmpty() {
        Cell cell = new Cell();
        assertThat(cell.isEmpty()).isTrue();
        assertThat(cell.getCharacter()).isEqualTo('\0');
        assertThat(cell.getAttributes()).isEqualTo(CellAttributes.defaultAttributes());
    }

    @Test
    void cellWithCharacterIsNotEmpty() {
        Cell cell = new Cell('A', CellAttributes.defaultAttributes());
        assertThat(cell.isEmpty()).isFalse();
        assertThat(cell.getCharacter()).isEqualTo('A');
    }

    @Test
    void cellClearResetsToEmpty() {
        Cell cell = new Cell('X', new CellAttributes(Color.RED, Color.BLUE,
                EnumSet.of(CellAttributes.Style.BOLD)));
        cell.clear();
        assertThat(cell.isEmpty()).isTrue();
        assertThat(cell.getAttributes()).isEqualTo(CellAttributes.defaultAttributes());
    }

    @Test
    void cellToStringForEmptyIsSpace() {
        assertThat(new Cell().toString()).isEqualTo(" ");
    }

    @Test
    void cellToStringForCharIsChar() {
        assertThat(new Cell('Z', CellAttributes.defaultAttributes()).toString()).isEqualTo("Z");
    }

    // ─── CursorPosition ────────────────────────────────────────────────

    @Test
    void cursorPositionGetSet() {
        var pos = new CursorPosition(5, 10);
        assertThat(pos.getColumn()).isEqualTo(5);
        assertThat(pos.getRow()).isEqualTo(10);

        pos.setColumn(20);
        pos.setRow(15);
        assertThat(pos.getColumn()).isEqualTo(20);
        assertThat(pos.getRow()).isEqualTo(15);
    }
}
