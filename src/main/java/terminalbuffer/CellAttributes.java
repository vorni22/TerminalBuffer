package terminalbuffer;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents text display attributes: foreground color, background color, and style flags.
 * Instances are immutable.
 */
public final class CellAttributes {

    /**
     * Style flags that can be applied to a cell.
     */
    public enum Style {
        BOLD,
        ITALIC,
        UNDERLINE
    }

    private final Color foreground;
    private final Color background;
    private final Set<Style> styles;

    /**
     * Creates cell attributes with the given colors and styles.
     *
     * @param foreground foreground color (non-null)
     * @param background background color (non-null)
     * @param styles     set of style flags (non-null, may be empty)
     */
    public CellAttributes(Color foreground, Color background, Set<Style> styles) {
        this.foreground = Objects.requireNonNull(foreground, "foreground must not be null");
        this.background = Objects.requireNonNull(background, "background must not be null");
        this.styles = styles.isEmpty() ? EnumSet.noneOf(Style.class) : EnumSet.copyOf(styles);
    }

    /**
     * Returns default attributes: default colors, no styles.
     */
    public static CellAttributes defaultAttributes() {
        return new CellAttributes(Color.DEFAULT, Color.DEFAULT, EnumSet.noneOf(Style.class));
    }

    public Color getForeground() {
        return foreground;
    }

    public Color getBackground() {
        return background;
    }

    public Set<Style> getStyles() {
        return Set.copyOf(styles);
    }

    public boolean hasStyle(Style style) {
        return styles.contains(style);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CellAttributes that)) return false;
        return foreground == that.foreground
                && background == that.background
                && styles.equals(that.styles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreground, background, styles);
    }

    @Override
    public String toString() {
        return "CellAttributes{fg=" + foreground + ", bg=" + background + ", styles=" + styles + "}";
    }
}
