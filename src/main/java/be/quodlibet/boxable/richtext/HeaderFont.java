package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.FontSet;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Predefined header font families.
 * Each variant exposes a full {@link FontSet} (regular, bold, italic, bold-italic).
 */
public enum HeaderFont {

    HELVETICA("Helvetica",
            new PDType1Font(Standard14Fonts.FontName.HELVETICA),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE)),

    TIMES_ROMAN("Times-Roman",
            new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN),
            new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD),
            new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC),
            new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC)),

    COURIER("Courier",
            new PDType1Font(Standard14Fonts.FontName.COURIER),
            new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD),
            new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE),
            new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE));

    private final FontSet fontSet;

    HeaderFont(String familyName, PDFont regular, PDFont bold, PDFont italic, PDFont boldItalic) {
        this.fontSet = new FontSet(familyName, regular, bold, italic, boldItalic);
    }

    /**
     * Returns the full {@link FontSet} for this header font family.
     *
     * @return font set with all four style variants
     */
    public FontSet getFontSet() {
        return fontSet;
    }
}

