package be.quodlibet.boxable.richtext;

/**
 * Text alignment options for rich text content within a block.
 */
public enum TextAlignment {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY;

    /**
     * Parses a string into a {@link TextAlignment}, defaulting to {@link #LEFT}.
     *
     * @param value the alignment name (case-insensitive)
     * @return the corresponding alignment
     */
    public static TextAlignment parse(final String value) {
        if (value == null) {
            return LEFT;
        }
        switch (value.trim().toUpperCase()) {
            case "CENTER":
                return CENTER;
            case "RIGHT":
                return RIGHT;
            case "JUSTIFY":
                return JUSTIFY;
            default:
                return LEFT;
        }
    }
}

