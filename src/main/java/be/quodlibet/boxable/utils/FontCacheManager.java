package be.quodlibet.boxable.utils;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Manages font caching for PDDocument instances. Provides thread-safe caching
 * of font data and PDType0Font objects with proper memory management.
 * </p>
 * 
 * @author generated
 */
public class FontCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(FontCacheManager.class);

    /**
     * <p>
     * Cache for font file data to avoid frequent loading from files.
     * The key is the font path and the value is the font file data as byte array.
     * </p>
     */
    private static final Map<String, byte[]> fontDataCache = new ConcurrentHashMap<>();

    /**
     * <p>
     * Cache for PDType0Font objects per document to avoid unnecessary
     * PDType0Font object creation. Uses WeakHashMap to allow garbage collection of documents.
     * The outer map uses PDDocument as key, inner map uses font path as key and is concurrent.
     * </p>
     */
    private static final Map<PDDocument, Map<String, PDType0Font>> documentFontCache = new WeakHashMap<>();

    private FontCacheManager() {
        // Utility class
    }

    /**
     * <p>
     * Retrieves cached font data for the given font path.
     * </p>
     * 
     * @param fontPath the path to the font file
     * @return cached font data as byte array, or null if not cached
     */
    public static byte[] getCachedFontData(String fontPath) {
        return fontDataCache.get(fontPath);
    }

    /**
     * <p>
     * Caches font data for the given font path.
     * </p>
     * 
     * @param fontPath the path to the font file
     * @param fontData the font data as byte array
     */
    public static void cacheFontData(String fontPath, byte[] fontData) {
        fontDataCache.put(fontPath, fontData);
        logger.debug("Cached font data for: " + fontPath);
    }

    /**
     * <p>
     * Retrieves a cached PDType0Font for the given document and font path.
     * </p>
     * 
     * @param document the PDDocument
     * @param fontPath the font path
     * @return cached PDType0Font or null if not found
     */
    public static PDType0Font getCachedFont(PDDocument document, String fontPath) {
        synchronized (documentFontCache) {
            Map<String, PDType0Font> docFonts = documentFontCache.get(document);
            if (docFonts != null) {
                PDType0Font cachedFont = docFonts.get(fontPath);
                if (cachedFont != null) {
                    logger.debug("Using cached PDType0Font for document and path: " + fontPath);
                    return cachedFont;
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * Caches a PDType0Font for the given document and font path.
     * </p>
     * 
     * @param document the PDDocument
     * @param fontPath the font path
     * @param font the PDType0Font to cache
     */
    public static void cacheFont(PDDocument document, String fontPath, PDType0Font font) {
        if (font != null) {
            synchronized (documentFontCache) {
                Map<String, PDType0Font> docFonts = documentFontCache.computeIfAbsent(document, 
                    k -> new ConcurrentHashMap<>());
                docFonts.put(fontPath, font);
                logger.debug("Cached PDType0Font for document and path: " + fontPath);
            }
        }
    }

    /**
     * <p>
     * Clears the font cache for a specific document.
     * </p>
     * 
     * @param document the PDDocument for which to clear the font cache
     */
    public static void clearDocumentFontCache(PDDocument document) {
        if (document != null) {
            synchronized (documentFontCache) {
                Map<String, PDType0Font> removedFonts = documentFontCache.remove(document);
                if (removedFonts != null) {
                    logger.debug("Cleared font cache for document, removed " + removedFonts.size() + " cached fonts");
                }
            }
        }
    }

    /**
     * <p>
     * Gets the number of documents currently in the font cache.
     * </p>
     * 
     * @return the number of documents with cached fonts
     */
    public static int getDocumentCacheSize() {
        synchronized (documentFontCache) {
            return documentFontCache.size();
        }
    }

    /**
     * <p>
     * Gets the number of fonts cached for a specific document.
     * </p>
     * 
     * @param document the PDDocument to check
     * @return the number of fonts cached for this document, or 0 if none
     */
    public static int getFontCacheSize(PDDocument document) {
        if (document == null) {
            return 0;
        }
        synchronized (documentFontCache) {
            Map<String, PDType0Font> docFonts = documentFontCache.get(document);
            return docFonts != null ? docFonts.size() : 0;
        }
    }
}