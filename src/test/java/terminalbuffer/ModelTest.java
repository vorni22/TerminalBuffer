package terminalbuffer;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the supporting model classes: {@link Cell}, {@link Attributes}, {@link CursorPosition}, {@link Color}.
 */
class ModelTest {

    // ─── Color ──────────────────────────────────────────────────────────

    @Test
    void colorEnumHas17Values() {
        // DEFAULT + 16 colors
        assertThat(Color.values()).hasSize(17);
    }

    // ─── Attributes ────────────────────────────────────────────────

    @Test
    void defaultAttributesHaveDefaultColorsAndNoStyles() {
        Attributes attrs = Attributes.defaultAttributes();
        assertThat(attrs.getForeground()).isEqualTo(Color.DEFAULT);
        assertThat(attrs.getBackground()).isEqualTo(Color.DEFAULT);
        assertThat(attrs.getStyles()).isEmpty();
    }

    @Test
    void attributesEqualityByValue() {
        var a = new Attributes(Color.RED, Color.BLUE, EnumSet.of(Attributes.Style.BOLD));
        var b = new Attributes(Color.RED, Color.BLUE, EnumSet.of(Attributes.Style.BOLD));
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void attributesInequalityOnDifferentValues() {
        var a = new Attributes(Color.RED, Color.DEFAULT, EnumSet.noneOf(Attributes.Style.class));
        var b = new Attributes(Color.GREEN, Color.DEFAULT, EnumSet.noneOf(Attributes.Style.class));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void attributesHasStyle() {
        var attrs = new Attributes(Color.DEFAULT, Color.DEFAULT,
                EnumSet.of(Attributes.Style.ITALIC, Attributes.Style.UNDERLINE));
        assertThat(attrs.hasStyle(Attributes.Style.ITALIC)).isTrue();
        assertThat(attrs.hasStyle(Attributes.Style.UNDERLINE)).isTrue();
        assertThat(attrs.hasStyle(Attributes.Style.BOLD)).isFalse();
    }

    @Test
    void attributesStylesAreImmutableCopy() {
        var attrs = new Attributes(Color.DEFAULT, Color.DEFAULT,
                EnumSet.of(Attributes.Style.BOLD));
        Set<Attributes.Style> styles = attrs.getStyles();
        assertThatThrownBy(() -> styles.add(Attributes.Style.ITALIC))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void attributesRejectsNullArguments() {
        assertThatThrownBy(() -> new Attributes(null, Color.DEFAULT, EnumSet.noneOf(Attributes.Style.class)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Attributes(Color.DEFAULT, null, EnumSet.noneOf(Attributes.Style.class)))
                .isInstanceOf(NullPointerException.class);
    }

    // ─── Cell ───────────────────────────────────────────────────────────

    @Test
    void newCellIsEmpty() {
        Cell cell = new Cell();
        assertThat(cell.isEmpty()).isTrue();
        assertThat(cell.getCharacter()).isEqualTo('\0');
        assertThat(cell.getAttributes()).isEqualTo(Attributes.defaultAttributes());
    }

    @Test
    void cellWithCharacterIsNotEmpty() {
        Cell cell = new Cell('A', Attributes.defaultAttributes());
        assertThat(cell.isEmpty()).isFalse();
        assertThat(cell.getCharacter()).isEqualTo('A');
    }

    @Test
    void cellClearResetsToEmpty() {
        Cell cell = new Cell('X', new Attributes(Color.RED, Color.BLUE,
                EnumSet.of(Attributes.Style.BOLD)));
        cell.clear();
        assertThat(cell.isEmpty()).isTrue();
        assertThat(cell.getAttributes()).isEqualTo(Attributes.defaultAttributes());
    }

    @Test
    void cellToStringForEmptyIsSpace() {
        assertThat(new Cell().toString()).isEqualTo(" ");
    }

    @Test
    void cellToStringForCharIsChar() {
        assertThat(new Cell('Z', Attributes.defaultAttributes()).toString()).isEqualTo("Z");
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
