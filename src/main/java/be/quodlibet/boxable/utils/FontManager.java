package be.quodlibet.boxable.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * FontManager provides centralized, thread-safe font management for Boxable.
 * It loads Google Source Sans 3 fonts (Regular, Bold, Italic, Bold Italic) once
 * and provides efficient access to font instances for PDF generation.
 * </p>
 * 
 * <p>
 * The FontManager follows the Singleton pattern with lazy initialization and 
 * uses ConcurrentHashMap for thread-safe operations. Font data is loaded once
 * from resources and cached in memory to avoid file system reads.
 * </p>
 * 
 * <p>
 * Since PDType0Font instances are tied to specific PDDocument instances in PDFBox,
 * this manager creates document-specific font instances efficiently from cached
 * font data while ensuring thread safety.
 * </p>
 * 
 * @author Boxable Contributors
 */
public final class FontManager {

    private static final Logger logger = LoggerFactory.getLogger(FontManager.class);

    /**
     * Singleton instance with lazy initialization.
     */
    private static volatile FontManager instance;

    /**
     * Thread-safe cache for font data (bytes) loaded from resources.
     * Key: font file path, Value: font file content as byte array
     */
    private final ConcurrentHashMap<String, byte[]> fontDataCache;

    /**
     * Thread-safe cache for document-specific font instances.
     * Key: document hash + font path, Value: PDFont instance
     */
    private final ConcurrentHashMap<String, PDFont> fontInstanceCache;

    /**
     * Supported font styles for Google Source Sans 3.
     */
    public enum FontStyle {
        REGULAR("fonts/SourceSans3-Regular.ttf"),
        BOLD("fonts/SourceSans3-Bold.ttf"),
        ITALIC("fonts/SourceSans3-Italic.ttf"),
        BOLD_ITALIC("fonts/SourceSans3-BoldItalic.ttf");

        private final String fontPath;

        FontStyle(String fontPath) {
            this.fontPath = fontPath;
        }

        public String getFontPath() {
            return fontPath;
        }
    }

    /**
     * Private constructor to enforce singleton pattern.
     */
    private FontManager() {
        this.fontDataCache = new ConcurrentHashMap<>();
        this.fontInstanceCache = new ConcurrentHashMap<>();
    }

    /**
     * <p>
     * Gets the singleton instance of FontManager using double-checked locking pattern
     * for thread-safe lazy initialization.
     * </p>
     * 
     * @return The FontManager singleton instance
     */
    public static FontManager getInstance() {
        if (instance == null) {
            synchronized (FontManager.class) {
                if (instance == null) {
                    instance = new FontManager();
                }
            }
        }
        return instance;
    }

    /**
     * <p>
     * Gets a font instance for the specified document and font style.
     * Font data is cached after first load to avoid file system reads.
     * Font instances are cached per document to optimize performance.
     * </p>
     * 
     * @param document The PDDocument for which to get the font
     * @param fontStyle The font style to retrieve
     * @return PDFont instance for the specified style and document
     * @throws IOException if font loading fails
     */
    public PDFont getFont(PDDocument document, FontStyle fontStyle) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("PDDocument cannot be null");
        }
        if (fontStyle == null) {
            throw new IllegalArgumentException("FontStyle cannot be null");
        }

        // Create unique key for this document + font combination
        String cacheKey = document.hashCode() + "_" + fontStyle.getFontPath();
        
        // Try to get cached font instance first
        PDFont cachedFont = fontInstanceCache.get(cacheKey);
        if (cachedFont != null) {
            return cachedFont;
        }

        // Load font data if not already cached
        byte[] fontData = fontDataCache.computeIfAbsent(fontStyle.getFontPath(), this::loadFontData);
        
        // Create font instance from cached data
        PDType0Font font = PDType0Font.load(document, new ByteArrayInputStream(fontData));
        
        // Cache the font instance for this document
        fontInstanceCache.put(cacheKey, font);
        
        logger.debug("Loaded font {} for document {}", fontStyle, document.hashCode());
        return font;
    }

    /**
     * <p>
     * Loads all four Google Source Sans 3 font variants for the specified document
     * and returns them in a map compatible with existing FontUtils API.
     * </p>
     * 
     * @param document The PDDocument for which to load fonts
     * @return Map containing all four font variants with standard keys
     * @throws IOException if any font loading fails
     */
    public Map<String, PDFont> loadSourceSans3Fonts(PDDocument document) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("PDDocument cannot be null");
        }
        
        Map<String, PDFont> fonts = new ConcurrentHashMap<>();
        
        fonts.put("font", getFont(document, FontStyle.REGULAR));
        fonts.put("fontBold", getFont(document, FontStyle.BOLD));
        fonts.put("fontItalic", getFont(document, FontStyle.ITALIC));
        fonts.put("fontBoldItalic", getFont(document, FontStyle.BOLD_ITALIC));
        
        logger.info("Loaded all Source Sans 3 font variants for document {}", document.hashCode());
        return fonts;
    }

    /**
     * <p>
     * Loads font data from resources. This method reads the font file once
     * and caches the byte array to avoid subsequent file system access.
     * </p>
     * 
     * @param fontPath Path to the font file in resources
     * @return Font file content as byte array
     * @throws RuntimeException if font file cannot be loaded
     */
    private byte[] loadFontData(String fontPath) {
        try {
            return FontUtils.readFontFileToByteArray(fontPath);
        } catch (IOException e) {
            logger.error("Failed to load font data from path: {}", fontPath, e);
            throw new RuntimeException("Failed to load font file: " + fontPath, e);
        }
    }

    /**
     * <p>
     * Clears all cached font instances. This should be called when documents are closed
     * to prevent memory leaks. Font data cache is preserved to maintain performance.
     * </p>
     * 
     * @param document The document whose font instances should be cleared
     */
    public void clearDocumentFonts(PDDocument document) {
        if (document == null) {
            return;
        }

        String documentPrefix = document.hashCode() + "_";
        fontInstanceCache.entrySet().removeIf(entry -> entry.getKey().startsWith(documentPrefix));
        
        logger.debug("Cleared font instances for document {}", document.hashCode());
    }

    /**
     * <p>
     * Clears all cached data. This method is primarily for testing purposes
     * and should not be called in production code.
     * </p>
     */
    public void clearAllCaches() {
        fontDataCache.clear();
        fontInstanceCache.clear();
        logger.debug("Cleared all font caches");
    }

    /**
     * <p>
     * Gets the number of cached font data entries. Useful for monitoring and testing.
     * </p>
     * 
     * @return Number of cached font data entries
     */
    public int getFontDataCacheSize() {
        return fontDataCache.size();
    }

    /**
     * <p>
     * Gets the number of cached font instances. Useful for monitoring and testing.
     * </p>
     * 
     * @return Number of cached font instances
     */
    public int getFontInstanceCacheSize() {
        return fontInstanceCache.size();
    }
}