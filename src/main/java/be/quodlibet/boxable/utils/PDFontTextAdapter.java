package be.quodlibet.boxable.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Utility/wrapper class around PDFont to provide safe text handling, measurement,
 * and rendering support for Boxable PDF generation.
 * </p>
 * 
 * @author Boxable Team
 */
public class PDFontTextAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PDFontTextAdapter.class);
    
    private final PDFont font;
    private static final String REPLACEMENT_CHARACTER = "?";
    private static final String ELLIPSIS = "...";
    
    /**
     * <p>
     * Creates a new PDFontTextAdapter wrapping the specified PDFont.
     * </p>
     * 
     * @param font The PDFont to wrap for text operations
     * @throws IllegalArgumentException if font is null
     */
    public PDFontTextAdapter(PDFont font) {
        if (font == null) {
            throw new IllegalArgumentException("Font cannot be null");
        }
        this.font = font;
    }
    
    /**
     * <p>
     * Sanitizes text by replacing unsupported glyphs with "?".
     * </p>
     * 
     * @param text The text to sanitize
     * @return Sanitized text with unsupported characters replaced
     */
    public String sanitizeText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            
            // Skip control characters except for space and common whitespace
            if (Character.isISOControl(codePoint) && codePoint != ' ' && codePoint != '\t') {
                // Replace control characters with space
                sanitized.append(' ');
            } else if (canDisplayCharacter(codePoint)) {
                sanitized.appendCodePoint(codePoint);
            } else {
                sanitized.append(REPLACEMENT_CHARACTER);
            }
            
            // Handle surrogate pairs properly
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++; // Skip the next char as it's part of the surrogate pair
            }
        }
        
        return sanitized.toString();
    }
    
    /**
     * <p>
     * Validates if the font can display a specific character.
     * </p>
     * 
     * @param codePoint The Unicode code point to check
     * @return true if the font can display the character, false otherwise
     */
    public boolean canDisplayCharacter(int codePoint) {
        try {
            String character = new String(Character.toChars(codePoint));
            font.getStringWidth(character);
            return true;
        } catch (IOException | IllegalArgumentException e) {
            // Font cannot display this character
            logger.debug("Font {} cannot display character with code point {}", font.getName(), codePoint);
            return false;
        }
    }
    
    /**
     * <p>
     * Returns the width of a string at the given font size.
     * </p>
     * 
     * @param text The text to measure
     * @param fontSize The font size
     * @return The width of the text in points
     */
    public float getStringWidth(String text, float fontSize) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        text = sanitizeText(text);        
        return FontUtils.getStringWidth(font, text, fontSize);
    }
    
    /**
     * <p>
     * Calculates the approximate line height using font metrics.
     * </p>
     * 
     * @param fontSize The font size
     * @return The line height in points
     */
    public float getLineHeight(float fontSize) {
        return FontUtils.getHeight(font, fontSize);
    }
    
    /**
     * <p>
     * Truncates text to fit within a maximum width, appending ellipsis (...) if truncation occurs.
     * </p>
     * 
     * @param text The text to truncate
     * @param maxWidth The maximum width in points
     * @param fontSize The font size
     * @return The truncated text with ellipsis if needed
     */
    public String truncateToWidth(String text, float maxWidth, float fontSize) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String sanitizedText = sanitizeText(text);
        float textWidth = getStringWidth(sanitizedText, fontSize);
        
        if (textWidth <= maxWidth) {
            return sanitizedText;
        }
        
        // Reserve space for ellipsis
        float ellipsisWidth = getStringWidth(ELLIPSIS, fontSize);
        float availableWidth = maxWidth - ellipsisWidth;
        
        if (availableWidth <= 0) {
            return ELLIPSIS;
        }
        
        // Binary search for the longest string that fits
        int left = 0;
        int right = sanitizedText.length();
        String result = ELLIPSIS;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            String candidate = sanitizedText.substring(0, mid);
            float candidateWidth = getStringWidth(candidate, fontSize);
            
            if (candidateWidth <= availableWidth) {
                result = candidate + ELLIPSIS;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return result;
    }
    
    /**
     * <p>
     * Wraps text into multiple lines within the specified width constraints.
     * </p>
     * 
     * @param text The text to wrap
     * @param maxWidth The maximum width per line in points
     * @param fontSize The font size
     * @return A list of text lines that fit within the width constraint
     */
    public List<String> wrapText(String text, float maxWidth, float fontSize) {
        List<String> lines = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return lines;
        }
        
        String sanitizedText = sanitizeText(text);
        String[] words = sanitizedText.split("\\s+");
        
        if (words.length == 0) {
            return lines;
        }
        
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            // Check if word itself is too long for a line
            if (getStringWidth(word, fontSize) > maxWidth) {
                // Add current line if it has content
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                // Truncate the long word and add it as a separate line
                String truncatedWord = truncateToWidth(word, maxWidth, fontSize);
                lines.add(truncatedWord);
                continue;
            }
            
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float lineWidth = getStringWidth(testLine, fontSize);
            
            if (lineWidth <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                // Current line is full, add it and start a new line with the current word
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        
        // Add the last line if it has content
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * <p>
     * Provides access to the underlying PDFont.
     * </p>
     * 
     * @return The wrapped PDFont instance
     */
    public PDFont getFont() {
        return font;
    }
}
