package be.quodlibet.boxable.richtext;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;

/**
 * An immutable run of text sharing a single set of style attributes.
 * <p>
 * A line of mixed-style text is modelled as an ordered list of
 * {@code RichTextSegment} instances — for example:
 * </p>
 * <pre>
 *   "Revenue grew by " (regular)  +  "25%" (bold)  +  " last quarter." (regular)
 * </pre>
 */
public final class RichTextSegment implements LineElement {

    private final String text;
    private final EnumSet<TextStyle> styles;
    private final float fontSize;
    private final Color color;

    /**
     * Creates a segment with explicit colour.
     */
    public RichTextSegment(String text, EnumSet<TextStyle> styles, float fontSize, Color color) {
        this.text = Objects.requireNonNull(text, "text");
        this.styles = styles != null ? EnumSet.copyOf(styles) : EnumSet.noneOf(TextStyle.class);
        this.fontSize = fontSize;
        this.color = color != null ? color : Color.BLACK;
    }

    /**
     * Creates a segment with default black colour.
     */
    public RichTextSegment(String text, EnumSet<TextStyle> styles, float fontSize) {
        this(text, styles, fontSize, Color.BLACK);
    }

    // == Getters ==========================================================

    public String getText()             { return text; }
    public EnumSet<TextStyle> getStyles() { return EnumSet.copyOf(styles); }
    public float getFontSize()          { return fontSize; }
    public Color getColor()             { return color; }

    public boolean isBold()      { return styles.contains(TextStyle.BOLD); }
    public boolean isItalic()    { return styles.contains(TextStyle.ITALIC); }
    public boolean isUnderline() { return styles.contains(TextStyle.UNDERLINE); }

    // ── Derived ──────────────────────────────────────────────────────────

    /**
     * Resolves the {@link PDFont} for this segment using the default body font set.
     */
    public PDFont resolveFont() {
        return FontResolver.resolve(styles);
    }

    /**
     * Computes the rendered width of this segment's text in points.
     *
     * @return width in points
     * @throws IOException if font metrics cannot be read
     */
    @Override
    public float getWidth() throws IOException {
        return WordWrapUtil.textWidth(text, resolveFont(), fontSize);
    }

    /**
     * Returns the font size (raw height of the text; line-spacing is applied
     * by the layout engine).
     */
    @Override
    public float getHeight() {
        return fontSize;
    }
}

