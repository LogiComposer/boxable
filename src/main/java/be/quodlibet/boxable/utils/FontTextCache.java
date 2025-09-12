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
    private final Map<Integer, Boolean> characterDisplayCache = new ConcurrentHashMap<>();
    
    // Cache for string width calculations to avoid repeated calculations
    private final Map<String, Float> stringWidthCache = new ConcurrentHashMap<>();
    
    // Pre-computed character strings to avoid repeated String creation
    private final Map<Integer, String> characterStringCache = new ConcurrentHashMap<>();
    
    /**
     * <p>
     * Gets a cached character display result.
     * </p>
     * 
     * @param codePoint The Unicode code point
     * @return The cached boolean result, or null if not cached
     */
    public Boolean getCharacterDisplayResult(int codePoint) {
        return characterDisplayCache.get(codePoint);
    }
    
    /**
     * <p>
     * Caches a character display result.
     * </p>
     * 
     * @param codePoint The Unicode code point
     * @param canDisplay Whether the font can display this character
     */
    public void putCharacterDisplayResult(int codePoint, boolean canDisplay) {
        characterDisplayCache.put(codePoint, canDisplay);
    }
    
    /**
     * <p>
     * Gets a cached string width result.
     * </p>
     * 
     * @param cacheKey The cache key (typically text + "|" + fontSize)
     * @return The cached float result, or null if not cached
     */
    public Float getStringWidth(String cacheKey) {
        return stringWidthCache.get(cacheKey);
    }
    
    /**
     * <p>
     * Caches a string width result.
     * </p>
     * 
     * @param cacheKey The cache key (typically text + "|" + fontSize)
     * @param width The calculated width
     */
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