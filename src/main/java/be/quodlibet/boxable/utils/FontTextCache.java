package be.quodlibet.boxable.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Cache class for font text operations to improve performance by avoiding repeated
 * calculations and object creation.
 * </p>
 * 
 * <p>
 * This class maintains three types of caches:
 * <ul>
 * <li>Character display cache - stores whether a font can display specific characters</li>
 * <li>String width cache - stores calculated widths for text strings at specific font sizes</li>
 * <li>Character string cache - stores pre-computed character string representations to avoid repeated object creation</li>
 * </ul>
 * </p>
 * 
 * @author Boxable Team
 */
public class FontTextCache {
    
    // Cache for character validation results to avoid repeated font.getStringWidth() calls
    // Key format: "fontName|codePoint"
    private final Map<String, Boolean> characterDisplayCache = new ConcurrentHashMap<>();
    
    // Cache for string width calculations to avoid repeated calculations
    // Key format: "fontName|text|fontSize"
    private final Map<String, Float> stringWidthCache = new ConcurrentHashMap<>();
    
    // Pre-computed character strings to avoid repeated String creation
    private final Map<Integer, String> characterStringCache = new ConcurrentHashMap<>();
    
    /**
     * <p>
     * Gets a cached character display result.
     * </p>
     * 
     * @param fontName The name of the font
     * @param codePoint The Unicode code point
     * @return The cached boolean result, or null if not cached
     */
    public Boolean getCharacterDisplayResult(String fontName, int codePoint) {
        String key = fontName + "|" + codePoint;
        return characterDisplayCache.get(key);
    }
    
    /**
     * <p>
     * Gets a cached character display result using legacy format.
     * Maintained for backward compatibility.
     * </p>
     * 
     * @param codePoint The Unicode code point
     * @return The cached boolean result, or null if not cached
     * @deprecated Use {@link #getCharacterDisplayResult(String, int)} instead
     */
    @Deprecated
    public Boolean getCharacterDisplayResult(int codePoint) {
        // For backward compatibility, try to find any cached result for this codepoint
        // This is less efficient but maintains compatibility
        for (String key : characterDisplayCache.keySet()) {
            if (key.endsWith("|" + codePoint)) {
                return characterDisplayCache.get(key);
            }
        }
        return null;
    }
    
    /**
     * <p>
     * Caches a character display result.
     * </p>
     * 
     * @param fontName The name of the font
     * @param codePoint The Unicode code point
     * @param canDisplay Whether the font can display this character
     */
    public void putCharacterDisplayResult(String fontName, int codePoint, boolean canDisplay) {
        String key = fontName + "|" + codePoint;
        characterDisplayCache.put(key, canDisplay);
    }
    
    /**
     * <p>
     * Caches a character display result using legacy format.
     * Maintained for backward compatibility.
     * </p>
     * 
     * @param codePoint The Unicode code point
     * @param canDisplay Whether the font can display this character
     * @deprecated Use {@link #putCharacterDisplayResult(String, int, boolean)} instead
     */
    @Deprecated
    public void putCharacterDisplayResult(int codePoint, boolean canDisplay) {
        // For backward compatibility, use a generic key
        String key = "legacy|" + codePoint;
        characterDisplayCache.put(key, canDisplay);
    }
    
    /**
     * <p>
     * Gets a cached string width result.
     * </p>
     * 
     * @param fontName The name of the font
     * @param text The text content
     * @param fontSize The font size
     * @return The cached float result, or null if not cached
     */
    public Float getStringWidth(String fontName, String text, float fontSize) {
        String key = fontName + "|" + text + "|" + fontSize;
        return stringWidthCache.get(key);
    }
    
    /**
     * <p>
     * Gets a cached string width result using legacy cache key format.
     * Maintained for backward compatibility.
     * </p>
     * 
     * @param cacheKey The cache key (typically text + "|" + fontSize)
     * @return The cached float result, or null if not cached
     * @deprecated Use {@link #getStringWidth(String, String, float)} instead
     */
    @Deprecated
    public Float getStringWidth(String cacheKey) {
        return stringWidthCache.get(cacheKey);
    }
    
    /**
     * <p>
     * Caches a string width result.
     * </p>
     * 
     * @param fontName The name of the font
     * @param text The text content
     * @param fontSize The font size
     * @param width The calculated width
     */
    public void putStringWidth(String fontName, String text, float fontSize, float width) {
        String key = fontName + "|" + text + "|" + fontSize;
        stringWidthCache.put(key, width);
    }
    
    /**
     * <p>
     * Caches a string width result using legacy cache key format.
     * Maintained for backward compatibility.
     * </p>
     * 
     * @param cacheKey The cache key (typically text + "|" + fontSize)
     * @param width The calculated width
     * @deprecated Use {@link #putStringWidth(String, String, float, float)} instead
     */
    @Deprecated
    public void putStringWidth(String cacheKey, float width) {
        stringWidthCache.put(cacheKey, width);
    }
    
    /**
     * <p>
     * Gets or computes a character string representation, using caching to avoid 
     * repeated String object creation for the same characters.
     * </p>
     * 
     * @param codePoint The Unicode code point
     * @return The string representation of the character
     */
    public String getCharacterString(int codePoint) {
        return characterStringCache.computeIfAbsent(codePoint, cp -> new String(Character.toChars(cp)));
    }
    
    /**
     * <p>
     * Clears all caches to free memory. This can be useful when processing large amounts
     * of text or when memory usage becomes a concern.
     * </p>
     */
    public void clearCaches() {
        characterDisplayCache.clear();
        stringWidthCache.clear();
        characterStringCache.clear();
    }
    
    /**
     * <p>
     * Returns the current size of the character display cache.
     * Useful for monitoring cache performance and memory usage.
     * </p>
     * 
     * @return The number of cached character display results
     */
    public int getCharacterDisplayCacheSize() {
        return characterDisplayCache.size();
    }
    
    /**
     * <p>
     * Returns the current size of the string width cache.
     * Useful for monitoring cache performance and memory usage.
     * </p>
     * 
     * @return The number of cached string width results
     */
    public int getStringWidthCacheSize() {
        return stringWidthCache.size();
    }
    
    /**
     * <p>
     * Returns the current size of the character string cache.
     * Useful for monitoring cache performance and memory usage.
     * </p>
     * 
     * @return The number of cached character string objects
     */
    public int getCharacterStringCacheSize() {
        return characterStringCache.size();
    }
}