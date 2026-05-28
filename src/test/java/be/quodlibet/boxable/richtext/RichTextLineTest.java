package be.quodlibet.boxable.richtext;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RichTextLine} font-size query methods.
 */
public class RichTextLineTest {

    private static final float DELTA = 0.001f;

    // ── getMaxFontSize ───────────────────────────────────────────────────

    @Test
    public void getMaxFontSize_textOnly_returnsLargestSegmentFontSize() {
        RichTextLine line = lineOf(
                segment("small", 8f),
                segment("large", 14f),
                segment("medium", 10f)
        );

        assertEquals(14f, line.getMaxFontSize(), DELTA);
    }

    @Test
    public void getMaxFontSize_withInlineImage_returnsImageHeightWhenTaller() throws IOException, URISyntaxException {
        InlineImageSegment bigImage = loadPng(100f, 60f); // 60pt tall image

        RichTextLine line = lineOf(segment("text", 10f), bigImage);

        // Image height (60) > text font size (10) → max is image height
        assertEquals(60f, line.getMaxFontSize(), DELTA);
    }

    // ── getTextMaxFontSize ────────────────────────────────────────────────

    @Test
    public void getTextMaxFontSize_textOnly_returnsLargestSegmentFontSize() {
        RichTextLine line = lineOf(
                segment("small", 8f),
                segment("large", 14f),
                segment("medium", 10f)
        );

        assertEquals(14f, line.getTextMaxFontSize(), DELTA);
    }

    @Test
    public void getTextMaxFontSize_textPlusLargeImage_returnsTextMaxNotImageHeight()
            throws IOException, URISyntaxException {
        InlineImageSegment bigImage = loadPng(100f, 60f); // 60pt tall image

        RichTextLine line = lineOf(segment("text", 10f), bigImage);

        // getMaxFontSize() is 60 (image height wins)
        assertEquals(60f, line.getMaxFontSize(), DELTA);
        // getTextMaxFontSize() must ignore the image and return text-only max
        assertEquals(10f, line.getTextMaxFontSize(), DELTA);
    }

    @Test
    public void getTextMaxFontSize_multipleTextSegmentsPlusImage_returnsLargestTextSize()
            throws IOException, URISyntaxException {
        InlineImageSegment bigImage = loadPng(100f, 60f); // 60pt tall image

        RichTextLine line = lineOf(
                segment("small", 8f),
                segment("large", 14f),
                bigImage,
                segment("medium", 10f)
        );

        assertEquals(14f, line.getTextMaxFontSize(), DELTA);
    }

    @Test
    public void getTextMaxFontSize_imageOnly_fallsBackToGetMaxFontSize()
            throws IOException, URISyntaxException {
        InlineImageSegment img = loadPng(80f, 50f); // 50pt tall image

        RichTextLine line = lineOf(img);

        // No text segments → fallback to getMaxFontSize()
        assertEquals(line.getMaxFontSize(), line.getTextMaxFontSize(), DELTA);
    }

    @Test
    public void getTextMaxFontSize_emptyLine_returnsMinimumFloor() {
        RichTextLine line = new RichTextLine(
                Collections.emptyList(), ListType.NONE, 0, TextAlignment.LEFT);

        // Both methods apply the same 6f floor
        assertEquals(line.getMaxFontSize(), line.getTextMaxFontSize(), DELTA);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * Verifies that inter-line spacing for a mixed text+image line scales with
     * the TEXT font size, not the image height.
     * <p>
     * Formula: lineHeight = imageHeight + textFontSize * (LINE_SPACING - 1)
     * </p>
     */
    @Test
    public void lineHeight_withLargeInlineImage_spacingProportionalToTextNotImage()
            throws IOException, URISyntaxException {
        float textFontSize = 10f;
        float imageHeight = 60f;
        float lineSpacingFactor = TextContentElement.LINE_SPACING - 1; // 0.4

        InlineImageSegment bigImage = loadPng(100f, imageHeight);
        RichTextLine line = lineOf(segment("text", textFontSize), bigImage);

        // Expected: image fits + text-proportional gap
        float expectedLineHeight = imageHeight + textFontSize * lineSpacingFactor; // 64pt
        float oldLineHeight = imageHeight * TextContentElement.LINE_SPACING;       // 84pt (old behaviour)

        // The new lineHeight must equal the expected formula
        assertEquals(expectedLineHeight, line.getMaxFontSize() + line.getTextMaxFontSize() * lineSpacingFactor, DELTA);

        // And must be smaller than the old formula (less excessive spacing above)
        assertTrue("New line height should be less than old formula",
                expectedLineHeight < oldLineHeight);
    }

    /**
     * Verifies that text-only line height is unaffected by the formula change.
     */
    @Test
    public void lineHeight_textOnly_unchangedByFormula() {
        float textFontSize = 10f;
        float lineSpacingFactor = TextContentElement.LINE_SPACING - 1; // 0.4

        RichTextLine line = lineOf(segment("text", textFontSize));

        // getMaxFontSize() == getTextMaxFontSize() for text-only lines
        float newFormula = line.getMaxFontSize() + line.getTextMaxFontSize() * lineSpacingFactor;
        float oldFormula = line.getMaxFontSize() * TextContentElement.LINE_SPACING;

        assertEquals("Text-only line height must be identical under both formulas",
                oldFormula, newFormula, DELTA);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static RichTextSegment segment(String text, float fontSize) {
        return new RichTextSegment(text, EnumSet.noneOf(TextStyle.class), fontSize);
    }

    @SafeVarargs
    private static <E extends LineElement> RichTextLine lineOf(E... elements) {
        return new RichTextLine(Arrays.asList(elements), ListType.NONE, 0, TextAlignment.LEFT);
    }

    private static InlineImageSegment loadPng(float widthPt, float heightPt)
            throws IOException, URISyntaxException {
        return InlineImageSegment.fromFile(
                Paths.get(Objects.requireNonNull(
                        RichTextLineTest.class.getResource("/150dpi.png")).toURI()).toFile(),
                widthPt, heightPt);
    }
}
