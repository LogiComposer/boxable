package be.quodlibet.boxable.richtext;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for wrapping text to fit within a given width using the metrics of
 * a specific {@link PDFont} at a given size.
 * <p>
 * <strong>Single Responsibility:</strong> Only computes line breaks — does not render.
 * </p>
 */
public final class WordWrapUtil {

    private WordWrapUtil() {
        // utility class
    }

    /**
     * Splits {@code text} into lines that each fit within {@code maxWidth} points
     * when rendered with {@code font} at {@code fontSize}.
     *
     * @param text     the text to wrap
     * @param font     the font used for width measurement
     * @param fontSize the font size in points
     * @param maxWidth the maximum line width in points
     * @return a list of wrapped lines (never empty — contains at least one element)
     * @throws IOException if font metrics cannot be read
     */
    public static List<String> wrap(String text, PDFont font, float fontSize,
                                    float maxWidth) throws IOException {
        if (text == null || text.isEmpty()) {
            return Collections.singletonList("");
        }
        if (maxWidth <= 0) {
            return Collections.singletonList(text);
        }

        List<String> lines = new ArrayList<>();
        // Split on whitespace boundaries while keeping trailing whitespace with each word
        String[] words = text.split("(?<=\\s)");

        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine + word;
            float candidateWidth = textWidth(candidate.trim(), font, fontSize);

            if (candidateWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder(word);
            } else {
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    /**
     * Measures the width of {@code text} in points when rendered with the given font and size.
     *
     * @param text     the text to measure
     * @param font     the font
     * @param fontSize the font size
     * @return width in points
     * @throws IOException if font metrics cannot be read
     */
    public static float textWidth(String text, PDFont font, float fontSize) throws IOException {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        return font.getStringWidth(text) / 1000f * fontSize;
    }
}

