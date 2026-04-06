package be.quodlibet.boxable;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Enumeration of the Standard 14 font families supported for header and body
 * rendering.  Each constant maps to the four style variants (regular, bold,
 * italic, bold-italic) of a PDF Standard 14 font family.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * FontSet fonts = FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN);
 * }</pre>
 */
public enum Standard14FontFamily {

    /** Helvetica (sans-serif) — the PDF default. */
    HELVETICA("Helvetica",
            Standard14Fonts.FontName.HELVETICA,
            Standard14Fonts.FontName.HELVETICA_BOLD,
            Standard14Fonts.FontName.HELVETICA_OBLIQUE,
            Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE),

    /** Times-Roman (serif). */
    TIMES_ROMAN("Times-Roman",
            Standard14Fonts.FontName.TIMES_ROMAN,
            Standard14Fonts.FontName.TIMES_BOLD,
            Standard14Fonts.FontName.TIMES_ITALIC,
            Standard14Fonts.FontName.TIMES_BOLD_ITALIC),

    /** Courier (monospaced). */
    COURIER("Courier",
            Standard14Fonts.FontName.COURIER,
            Standard14Fonts.FontName.COURIER_BOLD,
            Standard14Fonts.FontName.COURIER_OBLIQUE,
            Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);

    private final String familyName;
    private final Standard14Fonts.FontName regular;
    private final Standard14Fonts.FontName bold;
    private final Standard14Fonts.FontName italic;
    private final Standard14Fonts.FontName boldItalic;

    Standard14FontFamily(String familyName,
                         Standard14Fonts.FontName regular,
                         Standard14Fonts.FontName bold,
                         Standard14Fonts.FontName italic,
                         Standard14Fonts.FontName boldItalic) {
        this.familyName = familyName;
        this.regular = regular;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
    }

    /** Returns the human-readable family name (e.g.&nbsp;{@code "Helvetica"}). */
    public String getFamilyName() { return familyName; }

    /**
     * Creates a new {@link FontSet} from this font family's four variants.
     *
     * @return a fully populated FontSet
     */
    public FontSet toFontSet() {
        return new FontSet(familyName,
                new PDType1Font(regular),
                new PDType1Font(bold),
                new PDType1Font(italic),
                new PDType1Font(boldItalic));
    }
}

