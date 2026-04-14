package be.quodlibet.boxable.richtext;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import be.quodlibet.boxable.Standard14FontFamily;
import be.quodlibet.boxable.utils.FontUtils;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Comprehensive unit tests for RichTextBlock covering all RICHTEXTBLOCK.md
 * documentation sections. These tests were extracted from RichTextBlockTest
 * into a dedicated test file.
 */
public class RichTextBlockDocTest {

    /**
     * Creates a spacer {@link TextContentElement} consisting of {@code lines}
     * blank visual lines at the given font size.  Insert this between content
     * elements to simulate paragraph breaks without modifying
     * {@link RichTextBlock} itself.
     *
     * @param lines    the number of blank lines (each adds {@code fontSize × 1.4} pt of vertical space)
     * @param fontSize the font size that controls line height
     */
    private static TextContentElement paragraphBreak(int lines, float fontSize) {
        List<RichTextLine> spacerLines = new ArrayList<>();
        for (int i = 0; i < lines; i++) {
            spacerLines.add(new RichTextLine(
                    Collections.singletonList(
                            new RichTextSegment(" ", EnumSet.noneOf(TextStyle.class), fontSize)),
                    ListType.NONE, 0, TextAlignment.LEFT));
        }
        return new TextContentElement(spacerLines);
    }

    /**
     * Reads a classpath resource and returns its content as a Base64-encoded string.
     */
    private String resourceToBase64(String resourcePath) throws IOException {
        try (InputStream is = Objects.requireNonNull(
                RichTextBlockDocTest.class.getResourceAsStream(resourcePath),
                "Resource not found: " + resourcePath)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Plain Text (doc: "Text Styling > Plain Text")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that a simple plain text segment renders without error.
     */
    @Test
    public void testPlainTextRendering() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;

            RichTextLine plainLine = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Plain body text.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .addContent(new TextContentElement(plainLine))
                    .build();

            float finalY = block.renderOnNewPage(doc, pageSize, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Colored Text (doc: "Text Styling > Colored Text")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies rendering of colored text segments using the 4-arg
     * RichTextSegment constructor with explicit Color.
     */
    @Test
    public void testColoredTextRendering() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine coloredLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Red text ",
                                EnumSet.of(TextStyle.BOLD), 10f, Color.RED),
                        new RichTextSegment("Blue text ",
                                EnumSet.noneOf(TextStyle.class), 10f, Color.BLUE),
                        new RichTextSegment("Green text ",
                                EnumSet.of(TextStyle.ITALIC), 10f, Color.GREEN),
                        new RichTextSegment("Default black text",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Colored Text Demo", TextAlignment.CENTER)
                        .addContent(new TextContentElement(coloredLine))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Builder Validation (doc: "Builder API Reference" – build() constraints)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that build() rejects zero or negative block width.
     */
    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsZeroWidth() {
        RichTextBlock.builder()
                .at(0, 0).size(0, 300)
                .build();
    }

    /**
     * Verifies that build() rejects zero or negative block height.
     */
    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsZeroHeight() {
        RichTextBlock.builder()
                .at(0, 0).size(400, 0)
                .build();
    }

    /**
     * Verifies that build() rejects negative block width.
     */
    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsNegativeWidth() {
        RichTextBlock.builder()
                .at(0, 0).size(-100, 300)
                .build();
    }

    /**
     * Verifies that build() rejects negative block height.
     */
    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsNegativeHeight() {
        RichTextBlock.builder()
                .at(0, 0).size(400, -100)
                .build();
    }

    /**
     * Verifies that blockPadding() rejects negative values.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsNegativePadding() {
        RichTextBlock.builder()
                .at(0, 0).size(400, 300)
                .blockPadding(-5f)
                .build();
    }

    /**
     * Verifies that build() rejects padding too large for the width
     * (2 * padding >= width).
     */
    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsPaddingTooLargeForWidth() {
        RichTextBlock.builder()
                .at(0, 0).size(100, 300)
                .blockPadding(50f) // 2 * 50 = 100 >= 100
                .build();
    }

    /**
     * Verifies that build() rejects padding too large for the height
     * (2 * padding >= height).
     */
    @Test(expected = IllegalStateException.class)
    public void testBuilderRejectsPaddingTooLargeForHeight() {
        RichTextBlock.builder()
                .at(0, 0).size(400, 40)
                .blockPadding(20f) // 2 * 20 = 40 >= 40
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Builder Defaults (doc: "Builder API Reference" – default values)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies the default builder values match the documented defaults:
     * position (0,0), size 400×300, padding 4pt.
     */
    @Test
    public void testBuilderDefaults() {
        RichTextBlock block = RichTextBlock.builder().build();
        assertEquals("Default blockX", 0f, block.getBlockX(), 0.001f);
        assertEquals("Default blockY", 0f, block.getBlockY(), 0.001f);
        assertEquals("Default blockWidth", 400f, block.getBlockWidth(), 0.001f);
        assertEquals("Default blockHeight", 300f, block.getBlockHeight(), 0.001f);
        assertEquals("Default blockPadding", 4f, block.getBlockPadding(), 0.001f);
        assertNotNull("Content list should be non-null", block.getContent());
        assertTrue("Content list should be empty by default", block.getContent().isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Block Getters (doc: "Key Classes" – RichTextBlock owns position, size)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that the block getters return the configured values.
     */
    @Test
    public void testBlockGetters() {
        RichTextLine line = new RichTextLine(Collections.singletonList(
                new RichTextSegment("Test", EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0, TextAlignment.LEFT);

        RichTextBlock block = RichTextBlock.builder()
                .at(100, 80)
                .size(400, 300)
                .blockPadding(8f)
                .addContent(new TextContentElement(line))
                .build();

        assertEquals("blockX", 100f, block.getBlockX(), 0.001f);
        assertEquals("blockY", 80f, block.getBlockY(), 0.001f);
        assertEquals("blockWidth", 400f, block.getBlockWidth(), 0.001f);
        assertEquals("blockHeight", 300f, block.getBlockHeight(), 0.001f);
        assertEquals("blockPadding", 8f, block.getBlockPadding(), 0.001f);
        assertEquals("content size", 1, block.getContent().size());
    }

    /**
     * Verifies that the content list returned by getContent() is immutable.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testContentListIsImmutable() {
        RichTextLine line = new RichTextLine(Collections.singletonList(
                new RichTextSegment("Test", EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0, TextAlignment.LEFT);

        RichTextBlock block = RichTextBlock.builder()
                .at(0, 0).size(400, 300)
                .addContent(new TextContentElement(line))
                .build();

        // Should throw UnsupportedOperationException
        block.getContent().add(new TextContentElement(line));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  addAllContent (doc: "Builder API Reference" – addAllContent)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that addAllContent adds multiple content elements at once.
     */
    @Test
    public void testAddAllContent() throws IOException {
        RichTextLine line1 = new RichTextLine(Collections.singletonList(
                new RichTextSegment("First paragraph.",
                        EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0, TextAlignment.LEFT);

        RichTextLine line2 = new RichTextLine(Collections.singletonList(
                new RichTextSegment("Second paragraph.",
                        EnumSet.of(TextStyle.BOLD), 10f)),
                ListType.NONE, 0, TextAlignment.LEFT);

        List<ContentElement> elements = Arrays.asList(
                new TextContentElement(line1),
                new TextContentElement(line2));

        RichTextBlock block = RichTextBlock.builder()
                .at(40, 30).size(500, 300)
                .addAllContent(elements)
                .build();

        assertEquals("Should have 2 content elements", 2, block.getContent().size());

        try (PDDocument doc = new PDDocument()) {
            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Custom Block Padding (doc: "Builder API Reference" – blockPadding)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that a custom block padding value is applied and rendering works.
     */
    @Test
    public void testCustomBlockPadding() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Content with large padding.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .blockPadding(20f)
                    .addContent(new TextContentElement(line))
                    .build();

            assertEquals("Padding should be 20", 20f, block.getBlockPadding(), 0.001f);

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Block-Level Header Alignments (doc: "Block-Level Header")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that the block-level header renders correctly with CENTER alignment.
     */
    @Test
    public void testBlockLevelHeaderCenterAligned() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine body = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Content under center-aligned header.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 16,
                                "Center-Aligned Header", TextAlignment.CENTER)
                        .addContent(new TextContentElement(body))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    /**
     * Verifies that the block-level header renders correctly with RIGHT alignment.
     */
    @Test
    public void testBlockLevelHeaderRightAligned() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine body = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Content under right-aligned header.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 16,
                                "Right-Aligned Header", TextAlignment.RIGHT)
                        .addContent(new TextContentElement(body))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    /**
     * Verifies that a long block-level header text is word-wrapped within the block.
     */
    @Test
    public void testBlockLevelHeaderWordWrapping() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine body = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Content after a very long header.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                // Use a narrow block width to force the header to wrap
                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(200, 400)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 16,
                                "This Is A Very Long Block Level Header That Should Wrap Multiple Times",
                                TextAlignment.CENTER)
                        .addContent(new TextContentElement(body))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  No Header (doc: implied – header is optional)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies rendering works without a block-level header.
     */
    @Test
    public void testBlockWithoutHeader() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Content without a header.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .addContent(new TextContentElement(line))
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  JUSTIFY Alignment (doc: "Text Alignment > Left, Right, Center, and Justify")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies justified text alignment renders without error.
     * The last visual line should fall back to left alignment.
     */
    @Test
    public void testJustifyAlignment() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine justified = new RichTextLine(Collections.singletonList(
                        new RichTextSegment(
                                "This paragraph uses justify alignment which spreads words " +
                                "evenly across the full width of the block on every line except " +
                                "the last. The last line falls back to left alignment. This text " +
                                "is long enough to produce multiple wrapped lines to exercise the " +
                                "justify behaviour thoroughly.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.JUSTIFY);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(400, 400)
                        .addContent(new TextContentElement(justified))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Content Height Estimation (doc: "Content Height Estimation")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that estimateContentHeight() returns a positive value and
     * can be used to size the block to exactly fit its content.
     */
    @Test
    public void testEstimateContentHeight() throws IOException {
        RichTextLine line = new RichTextLine(Collections.singletonList(
                new RichTextSegment(
                        "This paragraph is used to test height estimation. It must be " +
                        "long enough to produce several wrapped lines so the estimate " +
                        "is meaningful and non-trivial.",
                        EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0, TextAlignment.LEFT);

        float width = 400f;
        RichTextBlock.Builder b = RichTextBlock.builder()
                .at(40, 30)
                .size(width, 9999f) // temporary large height
                .blockPadding(8f)
                .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 13,
                        "Auto-sized Block", TextAlignment.LEFT)
                .addContent(new TextContentElement(line));

        float estimatedHeight = b.build().estimateContentHeight();
        assertTrue("Estimated height should be positive", estimatedHeight > 0);

        // Rebuild with the estimated height + buffer, render, and verify
        RichTextBlock block = b.size(width, estimatedHeight + 10f).build();
        assertEquals("Block height should match estimated + buffer",
                estimatedHeight + 10f, block.getBlockHeight(), 0.001f);

        try (PDDocument doc = new PDDocument()) {
            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    /**
     * Verifies that estimateContentHeight() includes header contribution.
     */
    @Test
    public void testEstimateContentHeightWithHeader() throws IOException {
        float width = 400f;

        // Block with header only (no body content)
        RichTextBlock withHeader = RichTextBlock.builder()
                .at(0, 0).size(width, 9999f)
                .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                        "Header Text", TextAlignment.LEFT)
                .build();

        // Block without header or body content
        RichTextBlock withoutHeader = RichTextBlock.builder()
                .at(0, 0).size(width, 9999f)
                .build();

        float heightWithHeader = withHeader.estimateContentHeight();
        float heightWithoutHeader = withoutHeader.estimateContentHeight();

        assertTrue("Height with header should exceed height without header",
                heightWithHeader > heightWithoutHeader);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Overflow Handling (doc: "Overflow Handling")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that overflow rendering works with the overflow indicator enabled.
     */
    @Test
    public void testOverflowIndicatorEnabled() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextBlock.Builder builder = RichTextBlock.builder()
                    .at(50, 50).size(400, 80) // very small height to force overflow
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                            "Overflow Enabled", TextAlignment.CENTER)
                    .showOverflowIndicator(true);

            for (int i = 1; i <= 20; i++) {
                RichTextLine line = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Line " + i + ": Lorem ipsum dolor sit amet.",
                                EnumSet.noneOf(TextStyle.class), 9f)),
                        ListType.NONE, 0, TextAlignment.LEFT);
                builder.addContent(new TextContentElement(line));
            }

            float finalY = builder.build().renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    /**
     * Verifies that overflow rendering works with the overflow indicator disabled.
     */
    @Test
    public void testOverflowIndicatorDisabled() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextBlock.Builder builder = RichTextBlock.builder()
                    .at(50, 50).size(400, 80) // very small height to force overflow
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                            "Overflow Disabled", TextAlignment.CENTER)
                    .showOverflowIndicator(false); // silently truncate

            for (int i = 1; i <= 20; i++) {
                RichTextLine line = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Line " + i + ": Lorem ipsum dolor sit amet.",
                                EnumSet.noneOf(TextStyle.class), 9f)),
                        ListType.NONE, 0, TextAlignment.LEFT);
                builder.addContent(new TextContentElement(line));
            }

            float finalY = builder.build().renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Paragraph Breaks (doc: "Paragraph Breaks")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that paragraph breaks (blank spacer lines) add visible
     * vertical space between content elements.
     */
    @Test
    public void testParagraphBreaks() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine para1 = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("First paragraph.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextLine para2 = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Second paragraph after a break.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .addContent(new TextContentElement(para1))
                        .addContent(paragraphBreak(1, 10f))
                        .addContent(new TextContentElement(para2))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Debug Border (doc: "Debug Border")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that drawBorder(true) renders without error, producing a
     * light-gray border around the block.
     */
    @Test
    public void testDebugBorderEnabled() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Content with debug border.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .drawBorder(true)
                    .addContent(new TextContentElement(line))
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    /**
     * Verifies that drawBorder(false) renders without a border.
     */
    @Test
    public void testDebugBorderDisabled() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Content without debug border.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .drawBorder(false)
                    .addContent(new TextContentElement(line))
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Side-by-Side Blocks (doc: "Side-by-Side Blocks (Column Layout)")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that two blocks at different X positions render independently
     * on the same page without interfering with each other.
     */
    @Test
    public void testSideBySideColumnLayout() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine leftContent = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("This is the left column content.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextLine rightContent = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("This is the right column content.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                // Left column
                RichTextBlock leftBlock = RichTextBlock.builder()
                        .at(30, 30).size(250, 400)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Left Column", TextAlignment.CENTER)
                        .addContent(new TextContentElement(leftContent))
                        .drawBorder(true)
                        .build();

                // Right column
                RichTextBlock rightBlock = RichTextBlock.builder()
                        .at(300, 30).size(250, 400)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Right Column", TextAlignment.CENTER)
                        .addContent(new TextContentElement(rightContent))
                        .drawBorder(true)
                        .build();

                float leftY = leftBlock.render(doc, stream, pageSize.getHeight());
                float rightY = rightBlock.render(doc, stream, pageSize.getHeight());

                assertTrue("Left block Y should be positive", leftY > 0);
                assertTrue("Right block Y should be positive", rightY > 0);
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  renderOnNewPage (doc: "Rendering on a New Page")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies renderOnNewPage works in portrait mode.
     */
    @Test
    public void testRenderOnNewPagePortrait() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Portrait page content.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                            "Portrait Demo", TextAlignment.CENTER)
                    .addContent(new TextContentElement(line))
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
            assertEquals("Document should have 1 page", 1, doc.getNumberOfPages());
        }
    }

    /**
     * Verifies renderOnNewPage works in landscape mode.
     */
    @Test
    public void testRenderOnNewPageLandscape() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Landscape page content.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                            "Landscape Demo", TextAlignment.CENTER)
                    .addContent(new TextContentElement(line))
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, true);
            assertTrue("render() should return a positive final Y", finalY > 0);
            assertEquals("Document should have 1 page", 1, doc.getNumberOfPages());
        }
    }

    /**
     * Verifies renderOnNewPage works with LETTER page size.
     */
    @Test
    public void testRenderOnNewPageLetter() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Letter page content.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .addContent(new TextContentElement(line))
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.LETTER, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Rendering on Existing Page (doc: "Rendering on an Existing Page")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies render() on a pre-created page returns a usable Y position.
     */
    @Test
    public void testRenderOnExistingPage() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PageContentStreamOptimized stream = new PageContentStreamOptimized(
                    new PDPageContentStream(doc, page));
            try {
                RichTextLine line = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Content on existing page.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .addContent(new TextContentElement(line))
                        .build();

                float finalY = block.render(doc, stream, PDRectangle.A4.getHeight());
                assertTrue("finalY should be positive", finalY > 0);
                assertTrue("finalY should be less than page height",
                        finalY < PDRectangle.A4.getHeight());
            } finally {
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Image spacing and quality options
    //  (doc: "Block-Level Images > ImageContentElement.Builder Options")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that spacingBefore, spacingAfter, and quality builder options
     * are accepted and rendering succeeds.
     */
    @Test
    public void testImageSpacingAndQualityOptions() throws IOException, URISyntaxException {
        File jpgFile = Paths.get(
                Objects.requireNonNull(
                        RichTextBlockDocTest.class.getResource("/app_development.jpg")).toURI()
        ).toFile();

        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 600)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Image Options Test", TextAlignment.CENTER)
                        // Default spacing
                        .addContent(new ImageContentElement.Builder(jpgFile)
                                .alignment(TextAlignment.CENTER)
                                .cacheKey("spacing-default")
                                .build())
                        // Custom spacing
                        .addContent(new ImageContentElement.Builder(jpgFile)
                                .size(150, 50)
                                .spacingBefore(20f)
                                .spacingAfter(20f)
                                .alignment(TextAlignment.LEFT)
                                .cacheKey("spacing-custom")
                                .build())
                        // Custom quality
                        .addContent(new ImageContentElement.Builder(jpgFile)
                                .size(150, 50)
                                .quality(0.5f)
                                .alignment(TextAlignment.RIGHT)
                                .cacheKey("quality-custom")
                                .build())
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    /**
     * Verifies that ImageContentElement auto-scales proportionally to
     * block width when no explicit size is provided.
     */
    @Test
    public void testImageAutoScalingToBlockWidth() throws IOException, URISyntaxException {
        File jpgFile = Paths.get(
                Objects.requireNonNull(
                        RichTextBlockDocTest.class.getResource("/app_development.jpg")).toURI()
        ).toFile();

        try (PDDocument doc = new PDDocument()) {
            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(300, 500)
                    // No .size() call – proportionally scaled to block width
                    .addContent(new ImageContentElement.Builder(jpgFile)
                            .alignment(TextAlignment.CENTER)
                            .cacheKey("auto-scaled")
                            .build())
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Image from InputStream (doc: "Block-Level Images > From InputStream")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that an image loaded from an InputStream renders correctly.
     */
    @Test
    public void testImageFromInputStream() throws IOException {
        try (InputStream pngStream = Objects.requireNonNull(
                RichTextBlockDocTest.class.getResourceAsStream("/150dpi.png"))) {

            try (PDDocument doc = new PDDocument()) {
                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 400)
                        .addContent(new ImageContentElement.Builder(pngStream)
                                .size(200, 60)
                                .alignment(TextAlignment.CENTER)
                                .cacheKey("inputstream-png")
                                .build())
                        .build();

                float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
                assertTrue("render() should return a positive final Y", finalY > 0);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Inline Image Factory Methods
    //  (doc: "Inline Images > Inline Image Factory Methods")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies InlineImageSegment.of(BufferedImage, w, h) factory method.
     */
    @Test
    public void testInlineImageFromBufferedImage() throws IOException {
        BufferedImage img = new BufferedImage(100, 50,
                BufferedImage.TYPE_INT_RGB);

        InlineImageSegment inline = InlineImageSegment.of(img, 36, 14);

        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine line = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Text before ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        inline,
                        new RichTextSegment(" text after",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .addContent(new TextContentElement(line))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    /**
     * Verifies InlineImageSegment.fromStream(InputStream, w, h) factory method.
     */
    @Test
    public void testInlineImageFromStream() throws IOException {
        try (InputStream is = Objects.requireNonNull(
                RichTextBlockDocTest.class.getResourceAsStream("/150dpi.png"))) {
            InlineImageSegment inline = InlineImageSegment.fromStream(is, 36, 14);

            try (PDDocument doc = new PDDocument()) {
                PDRectangle pageSize = PDRectangle.A4;
                PDPage page = new PDPage(pageSize);
                doc.addPage(page);

                try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                    PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                    RichTextLine line = new RichTextLine(Arrays.asList(
                            new RichTextSegment("Text before ",
                                    EnumSet.noneOf(TextStyle.class), 10f),
                            inline,
                            new RichTextSegment(" text after",
                                    EnumSet.noneOf(TextStyle.class), 10f)
                    ), ListType.NONE, 0, TextAlignment.LEFT);

                    RichTextBlock block = RichTextBlock.builder()
                            .at(40, 30).size(500, 300)
                            .addContent(new TextContentElement(line))
                            .build();

                    float finalY = block.render(doc, stream, pageSize.getHeight());
                    assertTrue("render() should return a positive final Y", finalY > 0);
                    stream.close();
                }
            }
        }
    }

    /**
     * Verifies InlineImageSegment.fromBase64(String, w, h) factory method.
     */
    @Test
    public void testInlineImageFromBase64() throws IOException {
        String pngBase64 = resourceToBase64("/150dpi.png");
        InlineImageSegment inline = InlineImageSegment.fromBase64(pngBase64, 36, 14);

        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine line = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Text before ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        inline,
                        new RichTextSegment(" text after",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .addContent(new TextContentElement(line))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TextType Reference (doc: "Content Headers > TextType Reference")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies TextType enum defaults match the documented values.
     */
    @Test
    public void testTextTypeDefaults() {
        assertEquals("BODY default font size", 10f,
                TextType.BODY.getDefaultFontSize(), 0.001f);
        assertEquals("HEADER1 default font size", 18f,
                TextType.HEADER1.getDefaultFontSize(), 0.001f);
        assertEquals("HEADER2 default font size", 14f,
                TextType.HEADER2.getDefaultFontSize(), 0.001f);

        assertTrue("HEADER1 should be bold", TextType.HEADER1.isBold());
        assertTrue("HEADER2 should be bold", TextType.HEADER2.isBold());
        assertFalse("BODY should not be bold", TextType.BODY.isBold());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HeaderContentElement Builder options
    //  (doc: "Content Headers > HeaderContentElement.Builder supports")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies HeaderContentElement.Builder options: fontSize override,
     * fontFamily, alignment.
     */
    @Test
    public void testHeaderContentElementBuilderOptions() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                // H1 with custom font size and center alignment
                ContentElement h1Custom = new HeaderContentElement.Builder(
                        "Custom H1", TextType.HEADER1)
                        .fontSize(22f)
                        .fontFamily(FontUtils.getFontSet(Standard14FontFamily.COURIER))
                        .alignment(TextAlignment.CENTER)
                        .build();

                // H2 with right alignment
                ContentElement h2Right = new HeaderContentElement.Builder(
                        "Right-Aligned H2", TextType.HEADER2)
                        .alignment(TextAlignment.RIGHT)
                        .build();

                RichTextLine body = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Body text after custom headers.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 400)
                        .addContent(h1Custom)
                        .addContent(new TextContentElement(body))
                        .addContent(h2Right)
                        .addContent(new TextContentElement(body))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Quick Start example (doc: "Quick Start")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies the Quick Start example from the documentation compiles
     * and renders without error.
     */
    @Test
    public void testQuickStartExample() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            PageContentStreamOptimized stream = new PageContentStreamOptimized(
                    new PDPageContentStream(doc, page));

            RichTextLine paragraph = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Hello, RichTextBlock!",
                            EnumSet.noneOf(TextStyle.class), 12f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30)
                    .size(500, 300)
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA),
                            14, "My First Block", TextAlignment.CENTER)
                    .addContent(new TextContentElement(paragraph))
                    .build();

            float finalY = block.render(doc, stream, pageSize.getHeight());
            assertTrue("render() should return a positive final Y", finalY > 0);
            stream.close();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TextAlignment.parse (doc: "Text Alignment" – implicit)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies TextAlignment.parse for all values and edge cases.
     */
    @Test
    public void testTextAlignmentParse() {
        assertEquals(TextAlignment.LEFT, TextAlignment.parse(null));
        assertEquals(TextAlignment.LEFT, TextAlignment.parse("left"));
        assertEquals(TextAlignment.LEFT, TextAlignment.parse("LEFT"));
        assertEquals(TextAlignment.CENTER, TextAlignment.parse("center"));
        assertEquals(TextAlignment.CENTER, TextAlignment.parse("CENTER"));
        assertEquals(TextAlignment.RIGHT, TextAlignment.parse("right"));
        assertEquals(TextAlignment.RIGHT, TextAlignment.parse("RIGHT"));
        assertEquals(TextAlignment.JUSTIFY, TextAlignment.parse("justify"));
        assertEquals(TextAlignment.JUSTIFY, TextAlignment.parse("JUSTIFY"));
        assertEquals(TextAlignment.LEFT, TextAlignment.parse("unknown"));
        assertEquals(TextAlignment.LEFT, TextAlignment.parse(""));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RichTextSegment properties (doc: "Text Styling")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies RichTextSegment getters and convenience methods.
     */
    @Test
    public void testRichTextSegmentProperties() throws IOException {
        RichTextSegment bold = new RichTextSegment("Bold",
                EnumSet.of(TextStyle.BOLD), 12f, Color.RED);
        assertTrue("Should be bold", bold.isBold());
        assertFalse("Should not be italic", bold.isItalic());
        assertFalse("Should not be underline", bold.isUnderline());
        assertEquals("Text", "Bold", bold.getText());
        assertEquals("Font size", 12f, bold.getFontSize(), 0.001f);
        assertEquals("Color", Color.RED, bold.getColor());
        assertNotNull("Styles should not be null", bold.getStyles());
        assertTrue("Width should be positive", bold.getWidth() > 0);
        assertEquals("Height should equal font size", 12f, bold.getHeight(), 0.001f);

        // Default color (black)
        RichTextSegment defaultColor = new RichTextSegment("Text",
                EnumSet.noneOf(TextStyle.class), 10f);
        assertEquals("Default color should be BLACK", Color.BLACK, defaultColor.getColor());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RichTextLine properties (doc: "Key Classes – RichTextLine")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies RichTextLine getters and convenience constructors.
     */
    @Test
    public void testRichTextLineProperties() {
        RichTextSegment seg = new RichTextSegment("Test",
                EnumSet.noneOf(TextStyle.class), 12f);
        RichTextLine line = new RichTextLine(
                Collections.singletonList(seg),
                ListType.BULLETED, 3, TextAlignment.CENTER);

        assertEquals("Alignment", TextAlignment.CENTER, line.getAlignment());
        assertEquals("ListType", ListType.BULLETED, line.getListType());
        assertEquals("ListIndex", 3, line.getListIndex());
        assertEquals("Elements count", 1, line.getElements().size());
        assertEquals("Max font size", 12f, line.getMaxFontSize(), 0.001f);

        // Convenience constructor (defaults to LEFT alignment)
        RichTextLine defaultAlign = new RichTextLine(
                Collections.singletonList(seg), ListType.NONE, 0);
        assertEquals("Default alignment should be LEFT",
                TextAlignment.LEFT, defaultAlign.getAlignment());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Render return value used for chaining
    //  (doc: "Chaining Multiple Blocks" – render() returns final Y)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies render() return value is within the page bounds and can be
     * used for positioning the next block.
     */
    @Test
    public void testRenderReturnValueWithinBounds() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine line = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Short text.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .addContent(new TextContentElement(line))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("finalY should be positive", finalY > 0);
                assertTrue("finalY should be less than page height",
                        finalY < pageSize.getHeight());
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Empty block (edge case)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies rendering an empty block (no header, no content) completes
     * without error.
     */
    @Test
    public void testEmptyBlockRenders() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(400, 300)
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ImageContentElement.Builder quality validation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies that quality(0) is rejected.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testImageQualityRejectsZero() throws IOException, URISyntaxException {
        File jpgFile = Paths.get(
                Objects.requireNonNull(
                        RichTextBlockDocTest.class.getResource("/app_development.jpg")).toURI()
        ).toFile();

        new ImageContentElement.Builder(jpgFile)
                .quality(0f)
                .build();
    }

    /**
     * Verifies that quality > 1 is rejected.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testImageQualityRejectsAboveOne() throws IOException, URISyntaxException {
        File jpgFile = Paths.get(
                Objects.requireNonNull(
                        RichTextBlockDocTest.class.getResource("/app_development.jpg")).toURI()
        ).toFile();

        new ImageContentElement.Builder(jpgFile)
                .quality(1.5f)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  InlineImageSegment quality validation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies InlineImageSegment.withQuality rejects invalid values.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInlineImageQualityRejectsZero() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        InlineImageSegment inline = InlineImageSegment.of(img, 36, 14);
        inline.withQuality(0f);
    }

    /**
     * Verifies InlineImageSegment.withCacheKey returns a copy with the new key.
     */
    @Test
    public void testInlineImageWithCacheKey() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        InlineImageSegment inline = InlineImageSegment.of(img, 36, 14);
        InlineImageSegment copy = inline.withCacheKey("custom-key");
        assertEquals("Cache key should be custom", "custom-key", copy.getCacheKey());
        assertEquals("Width should be preserved", 36f, copy.getWidth(), 0.001f);
        assertEquals("Height should be preserved", 14f, copy.getHeight(), 0.001f);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Landscape page sizes: A3, A4, LETTER
    //  (doc: "Landscape Orientation")
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Verifies renderOnNewPage with A3 landscape.
     */
    @Test
    public void testRenderOnNewPageA3Landscape() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("A3 landscape content.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .addContent(new TextContentElement(line))
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A3, true);
            assertTrue("render() should return a positive final Y", finalY > 0);
        }
    }

    /**
     * Verifies manual landscape page creation by swapping width/height.
     */
    @Test
    public void testManualLandscapePageCreation() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle landscape = new PDRectangle(
                    PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());
            PDPage page = new PDPage(landscape);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine line = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Manual landscape content.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(600, 300)
                        .addContent(new TextContentElement(line))
                        .build();

                float finalY = block.render(doc, stream, landscape.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

}
