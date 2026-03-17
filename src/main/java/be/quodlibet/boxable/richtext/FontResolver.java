package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.FontSet;
import be.quodlibet.boxable.FontStyle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that resolves a {@link PDFont} from a {@link FontSet} (or default Helvetica)
 * based on a combination of {@link TextStyle} flags.
 * <p>
 * Results are cached so the same style combination always returns the same instance.
 * </p>
 * <p>
 * <strong>Single Responsibility:</strong> Only resolves fonts — no rendering logic.
 * </p>
 */
public final class FontResolver {

    /** Default body font family (Helvetica). */
    private static final FontSet DEFAULT_BODY_FONT_SET = HeaderFont.HELVETICA.getFontSet();

    /** Cache keyed by "fontSetIdentity|bold|italic". */
    private static final Map<String, PDFont> CACHE = new ConcurrentHashMap<>();

    private FontResolver() {
        // utility class
    }

    /**
     * Resolves the correct {@link PDFont} variant from the given {@link FontSet}
     * based on bold/italic flags derived from the style set.
     *
     * @param fontSet the font family (nullable — defaults to Helvetica)
     * @param styles  the active text styles
     * @return the resolved PDFont
     */
    public static PDFont resolve(FontSet fontSet, EnumSet<TextStyle> styles) {
        final FontSet effectiveFontSet = (fontSet != null) ? fontSet : DEFAULT_BODY_FONT_SET;
        FontStyle fs = toFontStyle(styles);
        String key = System.identityHashCode(effectiveFontSet) + "|" + fs.name();
        return CACHE.computeIfAbsent(key, k -> effectiveFontSet.getFont(fs));
    }

    /**
     * Convenience overload using the default body font family.
     *
     * @param styles the active text styles
     * @return the resolved PDFont
     */
    public static PDFont resolve(EnumSet<TextStyle> styles) {
        return resolve(DEFAULT_BODY_FONT_SET, styles);
    }

    /**
     * Resolves a header font from a {@link HeaderFont} enum with explicit bold/italic.
     *
     * @param headerFont the header font family
     * @param bold       whether bold
     * @param italic     whether italic
     * @return the resolved PDFont
     */
    public static PDFont resolveHeader(HeaderFont headerFont, boolean bold, boolean italic) {
        FontSet fontSet = (headerFont != null ? headerFont : HeaderFont.HELVETICA).getFontSet();
        EnumSet<TextStyle> styles = EnumSet.noneOf(TextStyle.class);
        if (bold) styles.add(TextStyle.BOLD);
        if (italic) styles.add(TextStyle.ITALIC);
        return resolve(fontSet, styles);
    }

    /**
     * Returns the default body {@link FontSet}.
     *
     * @return Helvetica font set
     */
    public static FontSet getDefaultBodyFontSet() {
        return DEFAULT_BODY_FONT_SET;
    }

    private static FontStyle toFontStyle(EnumSet<TextStyle> styles) {
        boolean bold = styles != null && styles.contains(TextStyle.BOLD);
        boolean italic = styles != null && styles.contains(TextStyle.ITALIC);
        if (bold && italic) return FontStyle.BOLD_ITALIC;
        if (bold) return FontStyle.BOLD;
        if (italic) return FontStyle.ITALIC;
        return FontStyle.REGULAR;
    }
}

