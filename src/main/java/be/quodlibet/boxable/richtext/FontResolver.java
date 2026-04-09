package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.FontSet;
import be.quodlibet.boxable.FontStyle;
import be.quodlibet.boxable.utils.FontUtils;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that resolves a {@link PDFont} from a {@link FontSet} (or the default
 * font set provided by {@link FontUtils#getDefaultFontSet()}) based on a
 * combination of {@link TextStyle} flags.
 * <p>
 * Results are cached so the same style combination always returns the same instance.
 * </p>
 * <p>
 * <strong>Single Responsibility:</strong> Only resolves fonts — no rendering logic.
 * </p>
 */
public final class FontResolver {

    /** Cache keyed by "familyName|FONT_STYLE". */
    private static final Map<String, PDFont> CACHE = new ConcurrentHashMap<>();

    private FontResolver() {
        // utility class
    }

    /**
     * Returns the default body {@link FontSet} by delegating to
     * {@link FontUtils#getDefaultFontSet()}, so that any fonts registered
     * via {@link FontUtils#addDefaultFonts} are respected.
     *
     * @return the current default font set
     */
    private static FontSet getDefaultBodyFontSet() {
        return FontUtils.getDefaultFontSet();
    }

    /**
     * Resolves the correct {@link PDFont} variant from the given {@link FontSet}
     * based on bold/italic flags derived from the style set.
     *
     * @param fontSet the font family (nullable — defaults to the default font set)
     * @param styles  the active text styles
     * @return the resolved PDFont
     */
    public static PDFont resolve(FontSet fontSet, EnumSet<TextStyle> styles) {
        final FontSet effectiveFontSet = (fontSet != null) ? fontSet : getDefaultBodyFontSet();
        FontStyle fs = toFontStyle(styles);
        String key = effectiveFontSet.getFamilyName() + "|" + fs.name();
        return CACHE.computeIfAbsent(key, k -> effectiveFontSet.getFont(fs));
    }

    /**
     * Convenience overload using the default body font family.
     *
     * @param styles the active text styles
     * @return the resolved PDFont
     */
    public static PDFont resolve(EnumSet<TextStyle> styles) {
        return resolve(getDefaultBodyFontSet(), styles);
    }

    /**
     * Resolves a header font from a {@link FontSet} with explicit bold/italic flags.
     *
     * @param fontSet the font family (nullable — defaults to the default font set)
     * @param bold    whether bold
     * @param italic  whether italic
     * @return the resolved PDFont
     */
    public static PDFont resolveHeader(FontSet fontSet, boolean bold, boolean italic) {
        FontSet effective = fontSet != null ? fontSet : getDefaultBodyFontSet();
        EnumSet<TextStyle> styles = EnumSet.noneOf(TextStyle.class);
        if (bold) styles.add(TextStyle.BOLD);
        if (italic) styles.add(TextStyle.ITALIC);
        return resolve(effective, styles);
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

