package be.quodlibet.boxable;

/**
 * Enumeration of supported font families available in the Boxable library.
 * Each font family includes regular, bold, italic, and bold-italic variants.
 * 
 * @author Boxable
 */
public enum SupportedFont {
    
    /**
     * FreeSans font family - a modern sans-serif font
     */
    FREE_SANS("FreeSans", 
              "fonts/FreeSans.ttf",
              "fonts/FreeSansBold.ttf", 
              "fonts/FreeSansOblique.ttf",
              "fonts/FreeSansBoldOblique.ttf"),
              
    /**
     * FreeSerif font family - a traditional serif font
     */
    FREE_SERIF("FreeSerif",
               "fonts/FreeSerif.ttf",
               "fonts/FreeSerifBold.ttf",
               "fonts/FreeSerifItalic.ttf", 
               "fonts/FreeSerifBoldItalic.ttf"),
               
    /**
     * FreeMono font family - a monospaced font
     */
    FREE_MONO("FreeMono",
              "fonts/FreeMono.ttf",
              "fonts/FreeMonoBold.ttf",
              "fonts/FreeMonoOblique.ttf",
              "fonts/FreeMonoBoldOblique.ttf"),
              
    /**
     * SourceSans3 font family - a modern sans-serif font from Adobe
     */
    SOURCE_SANS_3("SourceSans3",
                  "fonts/SourceSans3-Regular.ttf",
                  "fonts/SourceSans3-Bold.ttf",
                  "fonts/SourceSans3-It.ttf",
                  "fonts/SourceSans3-BoldIt.ttf");
    
    private final String familyName;
    private final String regularPath;
    private final String boldPath;
    private final String italicPath;
    private final String boldItalicPath;
    
    /**
     * Creates a new SupportedFont enum value.
     * 
     * @param familyName The name of the font family
     * @param regularPath Path to the regular font file
     * @param boldPath Path to the bold font file  
     * @param italicPath Path to the italic font file
     * @param boldItalicPath Path to the bold italic font file
     */
    SupportedFont(String familyName, String regularPath, String boldPath, 
                  String italicPath, String boldItalicPath) {
        this.familyName = familyName;
        this.regularPath = regularPath;
        this.boldPath = boldPath;
        this.italicPath = italicPath;
        this.boldItalicPath = boldItalicPath;
    }
    
    /**
     * Gets the font family name.
     * 
     * @return The font family name
     */
    public String getFamilyName() {
        return familyName;
    }
    
    /**
     * Gets the path to the regular font file.
     * 
     * @return The regular font file path
     */
    public String getRegularPath() {
        return regularPath;
    }
    
    /**
     * Gets the path to the bold font file.
     * 
     * @return The bold font file path
     */
    public String getBoldPath() {
        return boldPath;
    }
    
    /**
     * Gets the path to the italic font file.
     * 
     * @return The italic font file path
     */
    public String getItalicPath() {
        return italicPath;
    }
    
    /**
     * Gets the path to the bold italic font file.
     * 
     * @return The bold italic font file path
     */
    public String getBoldItalicPath() {
        return boldItalicPath;
    }
}