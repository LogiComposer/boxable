package be.quodlibet.boxable.richtext;

/**
 * Predefined text formatting levels used to apply consistent sizing and
 * styling to content within a {@link RichTextBlock}.
 *
 * <ul>
 *   <li>{@link #BODY}    — Normal body text (default 10 pt).</li>
 *   <li>{@link #HEADER1} — Primary heading (default 18 pt, bold).</li>
 *   <li>{@link #HEADER2} — Secondary heading (default 14 pt, bold).</li>
 * </ul>
 */
public enum TextType {

    /** Normal body text. */
    BODY(10f, false),

    /** Primary heading — larger, bold. */
    HEADER1(18f, true),

    /** Secondary heading — medium, bold. */
    HEADER2(14f, true);

    private final float defaultFontSize;
    private final boolean bold;

    TextType(float defaultFontSize, boolean bold) {
        this.defaultFontSize = defaultFontSize;
        this.bold = bold;
    }

    /** Returns the default font size for this text type. */
    public float getDefaultFontSize() {
        return defaultFontSize;
    }

    /** Returns {@code true} if this type is rendered in bold by default. */
    public boolean isBold() {
        return bold;
    }
}
