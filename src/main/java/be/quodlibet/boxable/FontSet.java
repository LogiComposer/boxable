package be.quodlibet.boxable;

import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * Container for a set of four font variants: Regular, Bold, Italic, and Bold-Italic.
 * This class provides a unified way to manage font families and ensures consistency
 * across table elements.
 * 
 * @author Boxable
 */
public class FontSet {
    
    private final PDFont regular;
    private final PDFont bold;
    private final PDFont italic;
    private final PDFont boldItalic;
    private final String familyName;
    
    /**
     * Creates a new FontSet with the specified font variants.
     * 
     * @param familyName The name of the font family
     * @param regular The regular/normal font
     * @param bold The bold font
     * @param italic The italic font
     * @param boldItalic The bold italic font
     */
    public FontSet(String familyName, PDFont regular, PDFont bold, PDFont italic, PDFont boldItalic) {
        if (regular == null || bold == null || italic == null || boldItalic == null) {
            throw new IllegalArgumentException("All font variants must be non-null");
        }
        this.familyName = familyName;
        this.regular = regular;
        this.bold = bold;
        this.italic = italic;
        this.boldItalic = boldItalic;
    }
    
    /**
     * Gets the font for the specified style.
     * 
     * @param style The font style to retrieve
     * @return The font for the specified style
     */
    public PDFont getFont(FontStyle style) {
        switch (style) {
            case REGULAR:
                return regular;
            case BOLD:
                return bold;
            case ITALIC:
                return italic;
            case BOLD_ITALIC:
                return boldItalic;
            default:
                return regular;
        }
    }
    
    /**
     * Gets the regular/normal font.
     * 
     * @return The regular font
     */
    public PDFont getRegular() {
        return regular;
    }
    
    /**
     * Gets the bold font.
     * 
     * @return The bold font
     */
    public PDFont getBold() {
        return bold;
    }
    
    /**
     * Gets the italic font.
     * 
     * @return The italic font
     */
    public PDFont getItalic() {
        return italic;
    }
    
    /**
     * Gets the bold italic font.
     * 
     * @return The bold italic font
     */
    public PDFont getBoldItalic() {
        return boldItalic;
    }
    
    /**
     * Gets the family name of this font set.
     * 
     * @return The font family name
     */
    public String getFamilyName() {
        return familyName;
    }
}