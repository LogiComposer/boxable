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
 * Comprehensive documentation-driven tests for {@link RichTextBlock} extracted
 * from the RICHTEXTBLOCK.md documentation sections.  Covers builder validation,
 * text styling, alignment, overflow, images, inline images, layout, and more.
 * <p>
 * All 51 individual verification scenarios are consolidated into three test
 * methods that group logically related assertions.
 */
public class RichTextBlockDocTest {

    // ──────────────────────────────────────────────────────────────────────
    //  Helper utilities (duplicated from RichTextBlockTest – private scope)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Creates a spacer {@link TextContentElement} consisting of {@code lines}
     * blank visual lines at the given font size.
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
    //  Test 1 – Builder Validation & API Contracts
    //  Covers: builder constraints, defaults, getters, immutability,
    //  addAllContent, TextType, TextAlignment.parse, RichTextSegment/Line
    //  properties, InlineImageSegment quality & cache-key validation,
    //  ImageContentElement quality validation.
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void testBuilderValidationAndApiContracts() throws IOException, URISyntaxException {

        // ── Builder rejects zero width ──
        try {
            RichTextBlock.builder().at(0, 0).size(0, 300).build();
            fail("Expected IllegalStateException for zero width");
        } catch (IllegalStateException expected) { }

        // ── Builder rejects zero height ──
        try {
            RichTextBlock.builder().at(0, 0).size(400, 0).build();
            fail("Expected IllegalStateException for zero height");
        } catch (IllegalStateException expected) { }

        // ── Builder rejects negative width ──
        try {
            RichTextBlock.builder().at(0, 0).size(-100, 300).build();
            fail("Expected IllegalStateException for negative width");
        } catch (IllegalStateException expected) { }

        // ── Builder rejects negative height ──
        try {
            RichTextBlock.builder().at(0, 0).size(400, -100).build();
            fail("Expected IllegalStateException for negative height");
        } catch (IllegalStateException expected) { }

        // ── Builder rejects negative padding ──
        try {
            RichTextBlock.builder().at(0, 0).size(400, 300).blockPadding(-5f).build();
            fail("Expected IllegalArgumentException for negative padding");
        } catch (IllegalArgumentException expected) { }

        // ── Builder rejects padding too large for width (2*pad >= width) ──
        try {
            RichTextBlock.builder().at(0, 0).size(100, 300).blockPadding(50f).build();
            fail("Expected IllegalStateException for padding too large for width");
        } catch (IllegalStateException expected) { }

        // ── Builder rejects padding too large for height (2*pad >= height) ──
        try {
            RichTextBlock.builder().at(0, 0).size(400, 40).blockPadding(20f).build();
            fail("Expected IllegalStateException for padding too large for height");
        } catch (IllegalStateException expected) { }

        // ── Builder defaults ──
        RichTextBlock defaults = RichTextBlock.builder().build();
        assertEquals("Default blockX", 0f, defaults.getBlockX(), 0.001f);
        assertEquals("Default blockY", 0f, defaults.getBlockY(), 0.001f);
        assertEquals("Default blockWidth", 400f, defaults.getBlockWidth(), 0.001f);
        assertEquals("Default blockHeight", 300f, defaults.getBlockHeight(), 0.001f);
        assertEquals("Default blockPadding", 4f, defaults.getBlockPadding(), 0.001f);
        assertNotNull("Content list should be non-null", defaults.getContent());
        assertTrue("Content list should be empty by default", defaults.getContent().isEmpty());

        // ── Block getters ──
        RichTextLine testLine = new RichTextLine(Collections.singletonList(
                new RichTextSegment("Test", EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0, TextAlignment.LEFT);

        RichTextBlock getterBlock = RichTextBlock.builder()
                .at(100, 80).size(400, 300).blockPadding(8f)
                .addContent(new TextContentElement(testLine))
                .build();

        assertEquals("blockX", 100f, getterBlock.getBlockX(), 0.001f);
        assertEquals("blockY", 80f, getterBlock.getBlockY(), 0.001f);
        assertEquals("blockWidth", 400f, getterBlock.getBlockWidth(), 0.001f);
        assertEquals("blockHeight", 300f, getterBlock.getBlockHeight(), 0.001f);
        assertEquals("blockPadding", 8f, getterBlock.getBlockPadding(), 0.001f);
        assertEquals("content size", 1, getterBlock.getContent().size());

        // ── Content list is immutable ──
        try {
            getterBlock.getContent().add(new TextContentElement(testLine));
            fail("Expected UnsupportedOperationException for immutable content list");
        } catch (UnsupportedOperationException expected) { }

        // ── addAllContent ──
        RichTextLine line1 = new RichTextLine(Collections.singletonList(
                new RichTextSegment("First paragraph.",
                        EnumSet.noneOf(TextStyle.class), 10f)),
                ListType.NONE, 0, TextAlignment.LEFT);
        RichTextLine line2 = new RichTextLine(Collections.singletonList(
                new RichTextSegment("Second paragraph.",
                        EnumSet.of(TextStyle.BOLD), 10f)),
                ListType.NONE, 0, TextAlignment.LEFT);

        List<ContentElement> elements = Arrays.asList(
                new TextContentElement(line1), new TextContentElement(line2));

        RichTextBlock addAllBlock = RichTextBlock.builder()
                .at(40, 30).size(500, 300)
                .addAllContent(elements)
                .build();

        assertEquals("Should have 2 content elements", 2, addAllBlock.getContent().size());

        try (PDDocument doc = new PDDocument()) {
            float finalY = addAllBlock.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("addAllContent render() should return positive Y", finalY > 0);
        }

        // ── TextType defaults ──
        assertEquals("BODY default font size", 10f,
                TextType.BODY.getDefaultFontSize(), 0.001f);
        assertEquals("HEADER1 default font size", 18f,
                TextType.HEADER1.getDefaultFontSize(), 0.001f);
        assertEquals("HEADER2 default font size", 14f,
                TextType.HEADER2.getDefaultFontSize(), 0.001f);
        assertTrue("HEADER1 should be bold", TextType.HEADER1.isBold());
        assertTrue("HEADER2 should be bold", TextType.HEADER2.isBold());
        assertFalse("BODY should not be bold", TextType.BODY.isBold());

        // ── TextAlignment.parse ──
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

        // ── RichTextSegment properties ──
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

        RichTextSegment defaultColor = new RichTextSegment("Text",
                EnumSet.noneOf(TextStyle.class), 10f);
        assertEquals("Default color should be BLACK", Color.BLACK, defaultColor.getColor());

        // ── RichTextLine properties ──
        RichTextSegment seg = new RichTextSegment("Test",
                EnumSet.noneOf(TextStyle.class), 12f);
        RichTextLine richLine = new RichTextLine(
                Collections.singletonList(seg),
                ListType.BULLETED, 3, TextAlignment.CENTER);

        assertEquals("Alignment", TextAlignment.CENTER, richLine.getAlignment());
        assertEquals("ListType", ListType.BULLETED, richLine.getListType());
        assertEquals("ListIndex", 3, richLine.getListIndex());
        assertEquals("Elements count", 1, richLine.getElements().size());
        assertEquals("Max font size", 12f, richLine.getMaxFontSize(), 0.001f);

        RichTextLine defaultAlign = new RichTextLine(
                Collections.singletonList(seg), ListType.NONE, 0);
        assertEquals("Default alignment should be LEFT",
                TextAlignment.LEFT, defaultAlign.getAlignment());

        // ── InlineImageSegment quality rejects zero ──
        BufferedImage smallImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        InlineImageSegment inlineImg = InlineImageSegment.of(smallImg, 36, 14);
        try {
            inlineImg.withQuality(0f);
            fail("Expected IllegalArgumentException for zero quality");
        } catch (IllegalArgumentException expected) { }

        // ── InlineImageSegment withCacheKey ──
        InlineImageSegment copy = inlineImg.withCacheKey("custom-key");
        assertEquals("Cache key should be custom", "custom-key", copy.getCacheKey());
        assertEquals("Width should be preserved", 36f, copy.getWidth(), 0.001f);
        assertEquals("Height should be preserved", 14f, copy.getHeight(), 0.001f);

        // ── ImageContentElement.Builder quality rejects zero ──
        File jpgFile = Paths.get(
                Objects.requireNonNull(
                        RichTextBlockDocTest.class.getResource("/app_development.jpg")).toURI()
        ).toFile();

        try {
            new ImageContentElement.Builder(jpgFile).quality(0f).build();
            fail("Expected IllegalArgumentException for quality 0");
        } catch (IllegalArgumentException expected) { }

        // ── ImageContentElement.Builder quality rejects > 1 ──
        try {
            new ImageContentElement.Builder(jpgFile).quality(1.5f).build();
            fail("Expected IllegalArgumentException for quality > 1");
        } catch (IllegalArgumentException expected) { }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Test 2 – Text Styling, Alignment & Layout
    //  Covers: plain text, colored text, custom padding, block headers
    //  (center/right/wrapping), no header, justify alignment, height
    //  estimation, overflow handling, paragraph breaks, debug border,
    //  column layout, page sizes, existing page rendering, empty block,
    //  render return value, quick-start example, header content element
    //  builder options.
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void testTextStylingAlignmentAndLayout() throws IOException {

        // ── Plain text rendering ──
        try (PDDocument doc = new PDDocument()) {
            RichTextLine plainLine = new RichTextLine(Collections.singletonList(
                    new RichTextSegment("Plain body text.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(500, 300)
                    .addContent(new TextContentElement(plainLine))
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("Plain text render() should return a positive final Y", finalY > 0);
        }

        // ── Colored text rendering ──
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
                assertTrue("Colored text render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }

        // ── Custom block padding ──
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
            assertTrue("Custom padding render() should return a positive final Y", finalY > 0);
        }

        // ── Block-level header CENTER aligned ──
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
                assertTrue("Center header render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }

        // ── Block-level header RIGHT aligned ──
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
                assertTrue("Right header render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }

        // ── Block-level header word wrapping ──
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

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(200, 400)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 16,
                                "This Is A Very Long Block Level Header That Should Wrap Multiple Times",
                                TextAlignment.CENTER)
                        .addContent(new TextContentElement(body))
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("Word-wrapping header render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }

        // ── Block without header ──
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
            assertTrue("No-header render() should return a positive final Y", finalY > 0);
        }

        // ── JUSTIFY alignment ──
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
                assertTrue("Justify render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }

        // ── Content height estimation ──
        {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment(
                            "This paragraph is used to test height estimation. It must be " +
                            "long enough to produce several wrapped lines so the estimate " +
                            "is meaningful and non-trivial.",
                            EnumSet.noneOf(TextStyle.class), 10f)),
                    ListType.NONE, 0, TextAlignment.LEFT);

            float width = 400f;
            RichTextBlock.Builder b = RichTextBlock.builder()
                    .at(40, 30).size(width, 9999f).blockPadding(8f)
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 13,
                            "Auto-sized Block", TextAlignment.LEFT)
                    .addContent(new TextContentElement(line));

            float estimatedHeight = b.build().estimateContentHeight();
            assertTrue("Estimated height should be positive", estimatedHeight > 0);

            RichTextBlock block = b.size(width, estimatedHeight + 10f).build();
            assertEquals("Block height should match estimated + buffer",
                    estimatedHeight + 10f, block.getBlockHeight(), 0.001f);

            try (PDDocument doc = new PDDocument()) {
                float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
                assertTrue("Height-estimation render() should return positive Y", finalY > 0);
            }
        }

        // ── Content height estimation includes header ──
        {
            float width = 400f;

            RichTextBlock withHeader = RichTextBlock.builder()
                    .at(0, 0).size(width, 9999f)
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                            "Header Text", TextAlignment.LEFT)
                    .build();

            RichTextBlock withoutHeader = RichTextBlock.builder()
                    .at(0, 0).size(width, 9999f)
                    .build();

            assertTrue("Height with header should exceed height without header",
                    withHeader.estimateContentHeight() > withoutHeader.estimateContentHeight());
        }

        // ── Overflow indicator enabled ──
        try (PDDocument doc = new PDDocument()) {
            RichTextBlock.Builder builder = RichTextBlock.builder()
                    .at(50, 50).size(400, 80)
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
            assertTrue("Overflow-enabled render() should return a positive final Y", finalY > 0);
        }

        // ── Overflow indicator disabled ──
        try (PDDocument doc = new PDDocument()) {
            RichTextBlock.Builder builder = RichTextBlock.builder()
                    .at(50, 50).size(400, 80)
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                            "Overflow Disabled", TextAlignment.CENTER)
                    .showOverflowIndicator(false);

            for (int i = 1; i <= 20; i++) {
                RichTextLine line = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Line " + i + ": Lorem ipsum dolor sit amet.",
                                EnumSet.noneOf(TextStyle.class), 9f)),
                        ListType.NONE, 0, TextAlignment.LEFT);
                builder.addContent(new TextContentElement(line));
            }

            float finalY = builder.build().renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("Overflow-disabled render() should return a positive final Y", finalY > 0);
        }

        // ── Paragraph breaks ──
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
                assertTrue("Paragraph-breaks render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }

        // ── Debug border enabled ──
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
            assertTrue("Debug-border-enabled render() should return a positive final Y", finalY > 0);
        }

        // ── Debug border disabled ──
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
            assertTrue("Debug-border-disabled render() should return a positive final Y", finalY > 0);
        }

        // ── Side-by-side column layout ──
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

                RichTextBlock leftBlock = RichTextBlock.builder()
                        .at(30, 30).size(250, 400)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Left Column", TextAlignment.CENTER)
                        .addContent(new TextContentElement(leftContent))
                        .drawBorder(true)
                        .build();

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

        // ── renderOnNewPage portrait ──
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
            assertTrue("Portrait render() should return a positive final Y", finalY > 0);
            assertEquals("Document should have 1 page", 1, doc.getNumberOfPages());
        }

        // ── renderOnNewPage landscape ──
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
            assertTrue("Landscape render() should return a positive final Y", finalY > 0);
            assertEquals("Document should have 1 page", 1, doc.getNumberOfPages());
        }

        // ── renderOnNewPage LETTER ──
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
            assertTrue("LETTER render() should return a positive final Y", finalY > 0);
        }

        // ── Render on existing page ──
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
                assertTrue("Existing-page finalY should be positive", finalY > 0);
                assertTrue("Existing-page finalY should be less than page height",
                        finalY < PDRectangle.A4.getHeight());
            } finally {
                stream.close();
            }
        }

        // ── HeaderContentElement builder options ──
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                ContentElement h1Custom = new HeaderContentElement.Builder(
                        "Custom H1", TextType.HEADER1)
                        .fontSize(22f)
                        .fontFamily(FontUtils.getFontSet(Standard14FontFamily.COURIER))
                        .alignment(TextAlignment.CENTER)
                        .build();

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
                assertTrue("Header-options render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }

        // ── Quick Start example ──
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
                    .at(40, 30).size(500, 300)
                    .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA),
                            14, "My First Block", TextAlignment.CENTER)
                    .addContent(new TextContentElement(paragraph))
                    .build();

            float finalY = block.render(doc, stream, pageSize.getHeight());
            assertTrue("Quick-start render() should return a positive final Y", finalY > 0);
            stream.close();
        }

        // ── Render return value within bounds ──
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

        // ── Empty block renders ──
        try (PDDocument doc = new PDDocument()) {
            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(400, 300)
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("Empty-block render() should return a positive final Y", finalY > 0);
        }

        // ── renderOnNewPage A3 landscape ──
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
            assertTrue("A3-landscape render() should return a positive final Y", finalY > 0);
        }

        // ── Manual landscape page creation ──
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
                assertTrue("Manual-landscape render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Test 3 – Image & Inline Image Rendering
    //  Covers: image spacing/quality options, auto-scaling, InputStream
    //  images, inline images from BufferedImage, InputStream, and Base64.
    // ══════════════════════════════════════════════════════════════════════

    @Test
    public void testImageAndInlineImageRendering() throws IOException, URISyntaxException {

        // ── Image spacing and quality options ──
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
                        .addContent(new ImageContentElement.Builder(jpgFile)
                                .alignment(TextAlignment.CENTER)
                                .cacheKey("spacing-default")
                                .build())
                        .addContent(new ImageContentElement.Builder(jpgFile)
                                .size(150, 50)
                                .spacingBefore(20f)
                                .spacingAfter(20f)
                                .alignment(TextAlignment.LEFT)
                                .cacheKey("spacing-custom")
                                .build())
                        .addContent(new ImageContentElement.Builder(jpgFile)
                                .size(150, 50)
                                .quality(0.5f)
                                .alignment(TextAlignment.RIGHT)
                                .cacheKey("quality-custom")
                                .build())
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("Image-options render() should return a positive final Y", finalY > 0);
                stream.close();
            }
        }

        // ── Image auto-scaling to block width ──
        try (PDDocument doc = new PDDocument()) {
            RichTextBlock block = RichTextBlock.builder()
                    .at(40, 30).size(300, 500)
                    .addContent(new ImageContentElement.Builder(jpgFile)
                            .alignment(TextAlignment.CENTER)
                            .cacheKey("auto-scaled")
                            .build())
                    .build();

            float finalY = block.renderOnNewPage(doc, PDRectangle.A4, false);
            assertTrue("Auto-scaled image render() should return a positive final Y", finalY > 0);
        }

        // ── Image from InputStream ──
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
                assertTrue("InputStream image render() should return a positive final Y", finalY > 0);
            }
        }

        // ── Inline image from BufferedImage ──
        {
            BufferedImage img = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
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
                    assertTrue("Inline-BufferedImage render() should return a positive final Y", finalY > 0);
                    stream.close();
                }
            }
        }

        // ── Inline image from InputStream ──
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
                    assertTrue("Inline-InputStream render() should return a positive final Y", finalY > 0);
                    stream.close();
                }
            }
        }

        // ── Inline image from Base64 ──
        {
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
                    assertTrue("Inline-Base64 render() should return a positive final Y", finalY > 0);
                    stream.close();
                }
            }
        }
    }
}
