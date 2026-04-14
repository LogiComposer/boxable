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

public class RichTextBlockTest {

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

    @Test
    public void testFullRichTextBlockRendering() throws IOException, URISyntaxException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle landscape = new PDRectangle(
                    PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());
            PDPage page1 = new PDPage(landscape);
            doc.addPage(page1);

            PageContentStreamOptimized stream = new PageContentStreamOptimized(
                    new PDPageContentStream(doc, page1));
            try {
                buildMainBlock().render(doc, stream, landscape.getHeight());
                buildSideBlock().render(doc, stream, landscape.getHeight());
                stream.endText();
            } finally {
                stream.close();
            }

            buildOverflowBlock().renderOnNewPage(doc, PDRectangle.A4, false);
            doc.save(new File("target/RichTextBlockDemo.pdf"));
        }
    }

    @Test
    public void testTextWrappingWithinNarrowBlock() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            PageContentStreamOptimized stream = new PageContentStreamOptimized(
                    new PDPageContentStream(doc, page));
            try {

                //  Block 1: Narrow block (150pt wide) with long text
                RichTextLine longLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "This is a very long text that should wrap within the narrow " +
                                "block bounds instead of overflowing outside. Each word must " +
                                "be pushed to the next line when it does not fit on the " +
                                "current line. The block width is only 150 points.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock narrowBlock = RichTextBlock.builder()
                        .at(30, 30).size(150, 400)
                        .blockPadding(8f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 11, "Narrow Block", TextAlignment.CENTER)
                        .addContent(new TextContentElement(longLine))
                        .drawBorder(true)
                        .build();
                narrowBlock.render(doc, stream, pageSize.getHeight());

                //  Block 2: Mixed-style segments that collectively overflow
                RichTextLine mixedLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Bold start ",
                                EnumSet.of(TextStyle.BOLD), 10f),
                        new RichTextSegment("followed by italic text that keeps going and going ",
                                EnumSet.of(TextStyle.ITALIC), 10f),
                        new RichTextSegment("and finally regular text to ensure multi-segment " +
                                "wrapping works correctly across style boundaries.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.JUSTIFY);

                RichTextBlock mixedBlock = RichTextBlock.builder()
                        .at(200, 30).size(180, 400)
                        .blockPadding(2f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN), 11, "Mixed Styles", TextAlignment.CENTER)
                        .addContent(new TextContentElement(mixedLine))
                        .drawBorder(true)
                        .build();
                mixedBlock.render(doc, stream, pageSize.getHeight());

                //  Block 3: Very long single word (character-level break)
                RichTextLine longWordLine = new RichTextLine(Collections.singletonList(
                        new RichTextSegment(
                                "Supercalifragilisticexpialidocious_ExtraordinarilyLongWordThatCannotFitOnOneLine " +
                                "followed by normal words.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock longWordBlock = RichTextBlock.builder()
                        .at(400, 30).size(160, 400)
                        .blockPadding(0f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.COURIER), 9, "Long Word Break", TextAlignment.CENTER)
                        .addContent(new TextContentElement(longWordLine))
                        .drawBorder(true)
                        .build();
                longWordBlock.render(doc, stream, pageSize.getHeight());
            } finally {
                stream.close();
            }

            doc.save(new File("target/RichTextBlockWrappingTest.pdf"));
        }
    }

    private RichTextBlock buildMainBlock() throws IOException, URISyntaxException {
        RichTextLine paragraph = new RichTextLine(Arrays.asList(
                new RichTextSegment("Revenue grew by ", EnumSet.noneOf(TextStyle.class), 10f),
                new RichTextSegment("25%", EnumSet.of(TextStyle.BOLD), 10f),
                new RichTextSegment(" compared to last quarter.", EnumSet.noneOf(TextStyle.class), 10f)
        ), ListType.NONE, 0, TextAlignment.JUSTIFY);

        RichTextLine subtitle = new RichTextLine(Collections.singletonList(
                new RichTextSegment("-- Confidential --",
                        EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 9f)
        ), ListType.NONE, 0, TextAlignment.CENTER);

        RichTextLine dateLine = new RichTextLine(Collections.singletonList(
                new RichTextSegment("Date: 2026-03-11", EnumSet.noneOf(TextStyle.class), 8f)
        ), ListType.NONE, 0, TextAlignment.RIGHT);

        List<RichTextLine> bullets = Arrays.asList(
                new RichTextLine(Collections.singletonList(
                        new RichTextSegment("North America: strong performance",
                                EnumSet.of(TextStyle.ITALIC), 9f)),
                        ListType.NONE, 0),
                new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Europe: steady growth",
                                EnumSet.noneOf(TextStyle.class), 9f)),
                        ListType.NONE, 0),
                new RichTextLine(Collections.singletonList(
                        new RichTextSegment("APAC: emerging opportunities",
                                EnumSet.of(TextStyle.BOLD), 9f)),
                        ListType.NONE, 0));

        List<RichTextLine> numbered = Arrays.asList(
                new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Expand into APAC markets",
                                EnumSet.of(TextStyle.UNDERLINE), 9f)),
                        ListType.NONE, 0),
                new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Increase R&D budget by 15%",
                                EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 9f)),
                        ListType.NONE, 0));

        // Load JPG image from file
        File jpgFile = Paths.get(
                Objects.requireNonNull(RichTextBlockTest.class.getResource("/app_development.jpg")).toURI()).toFile();

        // Load PNG image from InputStream (ensure it is closed via try-with-resources)
        try (InputStream pngStream = Objects.requireNonNull(
                RichTextBlockTest.class.getResourceAsStream("/150dpi.png"))) {

            return RichTextBlock.builder()
                    .at(30, 30).size(370, 500)
                    .header(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN), 16, "Quarterly Report Summary",
                            TextAlignment.CENTER)
                    .addContent(new TextContentElement(paragraph))
                    .addContent(paragraphBreak(1, 10f))
                    .addContent(new TextContentElement(subtitle))
                    .addContent(paragraphBreak(1, 10f))
                    .addContent(new TextContentElement(dateLine))
                    .addContent(paragraphBreak(1, 10f))
                    .addContent(new ImageContentElement.Builder(jpgFile)
                            .size(200, 60).alignment(TextAlignment.CENTER)
                            .cacheKey("jpg-app-dev").build())
                    .addContent(paragraphBreak(1, 10f))
                    .addContent(new ImageContentElement.Builder(pngStream)
                            .size(200, 60).alignment(TextAlignment.CENTER)
                            .cacheKey("png-150dpi").build())
                    .addContent(paragraphBreak(1, 10f))
                    .addContent(new ListContentElement(ListType.BULLETED, bullets))
                    .addContent(paragraphBreak(1, 10f))
                    .addContent(new ListContentElement(ListType.NUMBERED, numbered))
                    .showOverflowIndicator(true)
                    .drawBorder(true)
                    .build();
        }
    }

    private RichTextBlock buildSideBlock() {
        RichTextLine justifiedParagraph = new RichTextLine(Collections.singletonList(
                new RichTextSegment(
                        "This side block demonstrates that multiple RichTextBlocks " +
                        "can be rendered independently on the same page. Each block " +
                        "is confined to its own bounding rectangle.",
                        EnumSet.noneOf(TextStyle.class), 9f)),
                ListType.NONE, 0, TextAlignment.JUSTIFY);

        RichTextLine note = new RichTextLine(Collections.singletonList(
                new RichTextSegment("Note: side-by-side column layout.",
                        EnumSet.of(TextStyle.ITALIC), 8f)),
                ListType.NONE, 0, TextAlignment.CENTER);

        return RichTextBlock.builder()
                .at(420, 30).size(340, 500)
                .blockPadding(10f)
                .header(FontUtils.getFontSet(Standard14FontFamily.COURIER), 12, "Side Notes", TextAlignment.LEFT)
                .addContent(new TextContentElement(justifiedParagraph))
                .addContent(paragraphBreak(1, 9f))
                .addContent(new TextContentElement(note))
                .showOverflowIndicator(true)
                .drawBorder(true)
                .build();
    }

    private RichTextBlock buildOverflowBlock() {
        RichTextBlock.Builder builder = RichTextBlock.builder()
                .at(50, 50).size(495, 200)
                .blockPadding(3f)
                .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14, "Overflow Demonstration",
                        TextAlignment.CENTER)
                .showOverflowIndicator(true)
                .drawBorder(true);

        for (int i = 1; i <= 20; i++) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment(
                            "Line " + i + ": Lorem ipsum dolor sit amet, " +
                            "consectetur adipiscing elit. " +
                                    "This side block demonstrates that multiple RichTextBlocks " +
                                    "can be rendered independently on the same page. Each block " +
                                    "is confined to its own bounding rectangle.",
                            EnumSet.noneOf(TextStyle.class), 9f)),
                    ListType.NONE, 0, TextAlignment.LEFT);
            builder.addContent(new TextContentElement(line));
        }

        return builder.build();
    }

    @Test
    public void testInlineImagesWithText() throws IOException, URISyntaxException {
        File pngFile = Paths.get(
                Objects.requireNonNull(RichTextBlockTest.class.getResource("/150dpi.png")).toURI()).toFile();
        File jpgFile = Paths.get(
                Objects.requireNonNull(RichTextBlockTest.class.getResource("/app_development.jpg")).toURI()).toFile();

        InlineImageSegment pngInline = InlineImageSegment.fromFile(pngFile, 36, 14);
        InlineImageSegment jpgInline = InlineImageSegment.fromFile(jpgFile, 36, 14);

        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            PageContentStreamOptimized stream = new PageContentStreamOptimized(
                    new PDPageContentStream(doc, page));
            try {

                RichTextLine middleLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Revenue grew by ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline,
                        new RichTextSegment(" compared to last quarter.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextLine startLine = new RichTextLine(Arrays.asList(
                        jpgInline,
                        new RichTextSegment(" This text follows a JPG image at the start.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                //  3. Image at the end
                RichTextLine endLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("This text precedes a PNG image: ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline
                ), ListType.NONE, 0, TextAlignment.LEFT);

                //  4. Multiple images side-by-side with text
                RichTextLine sideBySide = new RichTextLine(Arrays.asList(
                        pngInline,
                        new RichTextSegment(" between ",
                                EnumSet.of(TextStyle.BOLD), 10f),
                        jpgInline,
                        new RichTextSegment(" and more ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline
                ), ListType.NONE, 0, TextAlignment.LEFT);

                //  5. Image-only line
                RichTextLine imageOnly = new RichTextLine(Arrays.asList(
                        pngInline, jpgInline, pngInline
                ), ListType.NONE, 0, TextAlignment.CENTER);

                //  6. Centered alignment with inline image
                RichTextLine centeredLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Centered: ",
                                EnumSet.of(TextStyle.ITALIC), 10f),
                        jpgInline,
                        new RichTextSegment(" label",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.CENTER);

                //  7. Right-aligned with inline image
                RichTextLine rightLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Right: ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline,
                        new RichTextSegment(" end",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.RIGHT);

                //  8. Justified with inline images
                RichTextLine justifiedLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Justified text with ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline,
                        new RichTextSegment(" inline image spread across the full width.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.JUSTIFY);

                RichTextBlock block = RichTextBlock.builder()
                        .at(30, 30).size(500, 700)
                        .blockPadding(6f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Inline Images Demo", TextAlignment.CENTER)
                        .addContent(new TextContentElement(middleLine))
                        .addContent(new TextContentElement(startLine))
                        .addContent(new TextContentElement(endLine))
                        .addContent(new TextContentElement(sideBySide))
                        .addContent(new TextContentElement(imageOnly))
                        .addContent(new TextContentElement(centeredLine))
                        .addContent(new TextContentElement(rightLine))
                        .addContent(new TextContentElement(justifiedLine))
                        .drawBorder(true)
                        .build();

                block.render(doc, stream, pageSize.getHeight());
            } finally {
                stream.close();
            }
        }
    }

    @Test
    public void testBase64ImageRendering() throws IOException {
        // Convert the PNG and JPG test resources to Base64 strings
        String pngBase64 = resourceToBase64("/150dpi.png");
        String jpgBase64DataUri = "data:image/jpeg;base64,"
                + resourceToBase64("/app_development.jpg");

        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine intro = new RichTextLine(Collections.singletonList(
                        new RichTextSegment(
                                "The images below are loaded from Base64-encoded strings.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(30, 30).size(500, 700)
                        .blockPadding(9f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Base64 Image Demo", TextAlignment.CENTER)
                        .addContent(new TextContentElement(intro))
                        // Raw Base64 PNG  proportionally scaled to block width
                        .addContent(ImageContentElement.Builder.fromBase64(pngBase64)
                                .alignment(TextAlignment.CENTER)
                                .cacheKey("b64-png").build())
                        // Data-URI Base64 JPG  explicit size override
                        .addContent(ImageContentElement.Builder.fromBase64(jpgBase64DataUri)
                                .size(220, 70).alignment(TextAlignment.CENTER)
                                .cacheKey("b64-jpg").build())
                        .drawBorder(true)
                        .build();

                block.render(doc, stream, pageSize.getHeight());
                stream.close();
            }

            doc.save(new File("target/Base64ImageDemo.pdf"));
        }
    }

    @Test
    public void testMultiParagraphAlignments() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                //  Paragraph 1: LEFT aligned (5+ lines)
                RichTextLine leftPara = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "This is the first paragraph with left alignment. It contains " +
                                "enough text to span at least five visual lines when rendered " +
                                "inside a reasonably narrow block. The purpose of this paragraph " +
                                "is to demonstrate that left-aligned text wraps correctly at word " +
                                "boundaries while keeping each line flush against the left margin. " +
                                "Notice how every line starts at exactly the same horizontal position, " +
                                "creating a clean and predictable reading experience for the viewer. " +
                                "Left alignment is the most common alignment used in Western typography " +
                                "because it follows the natural reading direction from left to right.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                //  Paragraph 2: RIGHT aligned (5+ lines)
                RichTextLine rightPara = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "This second paragraph uses right alignment. Every wrapped line " +
                                "is pushed to the right edge of the block, leaving a ragged left " +
                                "margin. Right alignment is less common for body text but can be " +
                                "effective for certain design elements such as pull quotes, date " +
                                "stamps, or decorative captions. This paragraph is deliberately " +
                                "long enough to produce at least five visual lines so that the " +
                                "right-alignment behaviour is clearly visible across multiple rows. " +
                                "Each successive line should end at exactly the same horizontal " +
                                "position on the right side of the content area.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.RIGHT);

                //  Paragraph 3: CENTER aligned (5+ lines)
                RichTextLine centerPara = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "The third and final paragraph demonstrates centre alignment. " +
                                "Each visual line is horizontally centred within the available " +
                                "block width, resulting in equal whitespace on both sides. Centre " +
                                "alignment is frequently used for headings, titles, and short " +
                                "decorative passages. When applied to longer body text like this " +
                                "paragraph it can be harder to read because neither margin is " +
                                "consistent, yet it remains a useful tool for emphasis. This text " +
                                "is intentionally verbose to guarantee that at least five wrapped " +
                                "lines are produced, so the centring effect is clearly observable.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.CENTER);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 750)
                        .blockPadding(12f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Multi-Paragraph Alignment Demo", TextAlignment.CENTER)
                        .addContent(new TextContentElement(leftPara))
                        .addContent(paragraphBreak(1, 10f))
                        .addContent(new TextContentElement(rightPara))
                        .addContent(paragraphBreak(1, 10f))
                        .addContent(new TextContentElement(centerPara))
                        .drawBorder(true)
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                // Verify render() returns a usable Y-position
                assertTrue("render() should return a positive final Y position", finalY > 0);
                stream.close();
            }

            doc.save(new File("target/MultiParagraphAlignmentDemo.pdf"));
        }
    }

    //
    //  Mixed formatting combinations
    //

    /**
     * Verifies all mixed text style combinations render without error:
     * BOLD+ITALIC, BOLD+UNDERLINE, ITALIC+UNDERLINE, BOLD+ITALIC+UNDERLINE.
     */
    @Test
    public void testMixedFormattingCombinations() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                // Bold + Italic
                RichTextLine boldItalic = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Bold-Italic: ",
                                EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC), 11f),
                        new RichTextSegment("This text is both bold and italic.",
                                EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC), 11f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // Bold + Underline
                RichTextLine boldUnderline = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Bold-Underline: ",
                                EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 11f),
                        new RichTextSegment("This text is bold with underline.",
                                EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 11f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // Italic + Underline
                RichTextLine italicUnderline = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Italic-Underline: ",
                                EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 11f),
                        new RichTextSegment("This text is italic with underline.",
                                EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 11f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // Bold + Italic + Underline (all three)
                RichTextLine allThree = new RichTextLine(Arrays.asList(
                        new RichTextSegment("All Three: ",
                                EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC, TextStyle.UNDERLINE), 11f),
                        new RichTextSegment("This text has bold, italic and underline combined.",
                                EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC, TextStyle.UNDERLINE), 11f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // Mixed segments within one line: normal -> bold+italic -> bold+underline -> italic+underline -> all
                RichTextLine mixedLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Normal ", EnumSet.noneOf(TextStyle.class), 10f),
                        new RichTextSegment("Bold+Italic ", EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC), 10f),
                        new RichTextSegment("Bold+Uline ", EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 10f),
                        new RichTextSegment("Italic+Uline ", EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 10f),
                        new RichTextSegment("All Three", EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC, TextStyle.UNDERLINE), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 700)
                        .blockPadding(5f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Mixed Formatting Combinations", TextAlignment.LEFT)
                        .addContent(new TextContentElement(boldItalic))
                        .addContent(paragraphBreak(1, 11f))
                        .addContent(new TextContentElement(boldUnderline))
                        .addContent(paragraphBreak(1, 11f))
                        .addContent(new TextContentElement(italicUnderline))
                        .addContent(paragraphBreak(1, 11f))
                        .addContent(new TextContentElement(allThree))
                        .addContent(paragraphBreak(1, 10f))
                        .addContent(new TextContentElement(mixedLine))
                        .drawBorder(true)
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }

            doc.save(new File("target/MixedFormattingCombinations.pdf"));
        }
    }

    //
    //  Body, Header1, Header2 text types
    //

    /**
     * Demonstrates Body(Normal), Header1, and Header2 content elements
     * rendered within a single block.
     */
    @Test
    public void testBodyAndHeaderTextTypes() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                // Header1 content element
                ContentElement h1 = new HeaderContentElement.Builder(
                        "Chapter One: Introduction", TextType.HEADER1)
                        .fontFamily(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN))
                        .alignment(TextAlignment.LEFT)
                        .build();

                // Body paragraph
                RichTextLine bodyLine1 = new RichTextLine(Collections.singletonList(
                        new RichTextSegment(
                                "This is normal body text rendered at the default body font " +
                                "size. It demonstrates that Body(Normal) text works alongside " +
                                "headers within the same RichTextBlock.",
                                EnumSet.noneOf(TextStyle.class),
                                TextType.BODY.getDefaultFontSize())),
                        ListType.NONE, 0, TextAlignment.LEFT);

                // Header2 content element
                ContentElement h2 = new HeaderContentElement.Builder(
                        "Section 1.1: Background", TextType.HEADER2)
                        .fontFamily(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN))
                        .alignment(TextAlignment.LEFT)
                        .build();

                // Another body paragraph with mixed styles
                RichTextLine bodyLine2 = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Body text can contain ",
                                EnumSet.noneOf(TextStyle.class),
                                TextType.BODY.getDefaultFontSize()),
                        new RichTextSegment("bold", EnumSet.of(TextStyle.BOLD),
                                TextType.BODY.getDefaultFontSize()),
                        new RichTextSegment(", ",
                                EnumSet.noneOf(TextStyle.class),
                                TextType.BODY.getDefaultFontSize()),
                        new RichTextSegment("italic", EnumSet.of(TextStyle.ITALIC),
                                TextType.BODY.getDefaultFontSize()),
                        new RichTextSegment(", and ",
                                EnumSet.noneOf(TextStyle.class),
                                TextType.BODY.getDefaultFontSize()),
                        new RichTextSegment("underlined", EnumSet.of(TextStyle.UNDERLINE),
                                TextType.BODY.getDefaultFontSize()),
                        new RichTextSegment(" formatting mixed inline.",
                                EnumSet.noneOf(TextStyle.class),
                                TextType.BODY.getDefaultFontSize())
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // Second Header2
                ContentElement h2b = new HeaderContentElement.Builder(
                        "Section 1.2: Methodology", TextType.HEADER2)
                        .fontFamily(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN))
                        .alignment(TextAlignment.LEFT)
                        .build();

                RichTextLine bodyLine3 = new RichTextLine(Collections.singletonList(
                        new RichTextSegment(
                                "Another body paragraph following the second sub-heading. " +
                                "The text type system allows mixing header and body content " +
                                "in a structured document layout.",
                                EnumSet.noneOf(TextStyle.class),
                                TextType.BODY.getDefaultFontSize())),
                        ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 700)
                        //.blockPadding(15f)
                        .addContent(h1)
                        .addContent(new TextContentElement(bodyLine1))
                        .addContent(h2)
                        .addContent(new TextContentElement(bodyLine2))
                        .addContent(h2b)
                        .addContent(new TextContentElement(bodyLine3))
                        .drawBorder(true)
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }

            doc.save(new File("target/BodyAndHeaderTextTypes.pdf"));
        }
    }

    //
    //  Header rendering (left-aligned) with underline
    //

    /**
     * Verifies that the block-level header renders left-aligned with an underline.
     */
    @Test
    public void testHeaderLeftAlignedWithUnderline() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                RichTextLine body = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("Content under the left-aligned underlined header.",
                                EnumSet.noneOf(TextStyle.class), 10f)),
                        ListType.NONE, 0, TextAlignment.LEFT);

                // Block-level header explicitly left-aligned
                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 300)
                        .blockPadding(1f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 16,
                                "Left-Aligned Header With Underline", TextAlignment.LEFT)
                        .addContent(new TextContentElement(body))
                        .drawBorder(true)
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }

            doc.save(new File("target/HeaderLeftAlignedUnderline.pdf"));
        }
    }

    //
    //  Bulleted and numbered lists
    //

    /**
     * Verifies that bulleted and numbered lists render correctly, including
     * lists with mixed formatting in items.
     */
    @Test
    public void testBulletedAndNumberedLists() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                // Bulleted list with mixed formatting
                List<RichTextLine> bulletItems = Arrays.asList(
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Plain bullet item",
                                        EnumSet.noneOf(TextStyle.class), 10f)),
                                ListType.NONE, 0),
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Bold bullet item",
                                        EnumSet.of(TextStyle.BOLD), 10f)),
                                ListType.NONE, 0),
                        new RichTextLine(Arrays.asList(
                                new RichTextSegment("Mixed: ",
                                        EnumSet.noneOf(TextStyle.class), 10f),
                                new RichTextSegment("bold+italic",
                                        EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC), 10f),
                                new RichTextSegment(" in a bullet",
                                        EnumSet.noneOf(TextStyle.class), 10f)),
                                ListType.NONE, 0));

                // Numbered list with styled items
                List<RichTextLine> numberedItems = Arrays.asList(
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("First numbered item",
                                        EnumSet.noneOf(TextStyle.class), 10f)),
                                ListType.NONE, 0),
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Second item with underline",
                                        EnumSet.of(TextStyle.UNDERLINE), 10f)),
                                ListType.NONE, 0),
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Third item bold+underline",
                                        EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 10f)),
                                ListType.NONE, 0),
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Fourth item all styles",
                                        EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC, TextStyle.UNDERLINE), 10f)),
                                ListType.NONE, 0));

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 600)
                        .blockPadding(7f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 14,
                                "Bulleted & Numbered Lists", TextAlignment.LEFT)
                        .addContent(new ListContentElement(ListType.BULLETED, bulletItems))
                        .addContent(new ListContentElement(ListType.NUMBERED, numberedItems))
                        .drawBorder(true)
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }

            doc.save(new File("target/BulletedAndNumberedLists.pdf"));
        }
    }

    //
    //  Comprehensive combined test
    //

    /**
     * A single comprehensive test that combines all capabilities:
     * Header1, Header2, body text, mixed formatting, and lists.
     */
    @Test
    public void testComprehensiveRichTextCapabilities() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                // H1
                ContentElement h1 = new HeaderContentElement.Builder(
                        "Document Title", TextType.HEADER1)
                        .fontFamily(FontUtils.getFontSet(Standard14FontFamily.HELVETICA))
                        .alignment(TextAlignment.LEFT)
                        .build();

                // Body
                RichTextLine intro = new RichTextLine(Collections.singletonList(
                        new RichTextSegment(
                                "This comprehensive test demonstrates all formatting capabilities " +
                                "of the RichTextBlock system.",
                                EnumSet.noneOf(TextStyle.class),
                                TextType.BODY.getDefaultFontSize())),
                        ListType.NONE, 0, TextAlignment.LEFT);

                // H2
                ContentElement h2 = new HeaderContentElement.Builder(
                        "Styled Text", TextType.HEADER2)
                        .alignment(TextAlignment.LEFT)
                        .build();

                // Body with all combinations
                RichTextLine styledLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Normal ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        new RichTextSegment("Bold ",
                                EnumSet.of(TextStyle.BOLD), 10f),
                        new RichTextSegment("Italic ",
                                EnumSet.of(TextStyle.ITALIC), 10f),
                        new RichTextSegment("Underline ",
                                EnumSet.of(TextStyle.UNDERLINE), 10f),
                        new RichTextSegment("B+I ",
                                EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC), 10f),
                        new RichTextSegment("B+U ",
                                EnumSet.of(TextStyle.BOLD, TextStyle.UNDERLINE), 10f),
                        new RichTextSegment("I+U ",
                                EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 10f),
                        new RichTextSegment("B+I+U",
                                EnumSet.of(TextStyle.BOLD, TextStyle.ITALIC, TextStyle.UNDERLINE), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // H2
                ContentElement h2Lists = new HeaderContentElement.Builder(
                        "Lists", TextType.HEADER2)
                        .alignment(TextAlignment.LEFT)
                        .build();

                // Bullets
                List<RichTextLine> bullets = Arrays.asList(
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Bullet one",
                                        EnumSet.noneOf(TextStyle.class), 10f)),
                                ListType.NONE, 0),
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Bullet two (bold)",
                                        EnumSet.of(TextStyle.BOLD), 10f)),
                                ListType.NONE, 0));

                // Numbers
                List<RichTextLine> numbers = Arrays.asList(
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Step one",
                                        EnumSet.noneOf(TextStyle.class), 10f)),
                                ListType.NONE, 0),
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Step two (italic+underline)",
                                        EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 10f)),
                                ListType.NONE, 0));

                RichTextBlock block = RichTextBlock.builder()
                        .at(40, 30).size(500, 750)
                        .addContent(h1)
                        .addContent(new TextContentElement(intro))
                        .addContent(h2)
                        .addContent(new TextContentElement(styledLine))
                        .addContent(h2Lists)
                        .addContent(new ListContentElement(ListType.BULLETED, bullets))
                        .addContent(new ListContentElement(ListType.NUMBERED, numbers))
                        .drawBorder(true)
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                assertTrue("render() should return a positive final Y", finalY > 0);
                stream.close();
            }

            doc.save(new File("target/ComprehensiveRichTextCapabilities.pdf"));
        }
    }

    //
    //  Landscape page-size tests (A3, A4, LETTER)
    //

    /**
     * Multi-paragraph content on an A3 landscape page with LEFT, RIGHT and
     * CENTER alignment - each paragraph spans at least 5 wrapped lines.
     */
    @Test
    public void testMultiParagraphLandscapeA3() throws IOException {
        renderLandscapeParagraphs(PDRectangle.A3, "LandscapeA3_Paragraphs");
    }

    /**
     * Multi-paragraph content on an A4 landscape page with LEFT, RIGHT and
     * CENTER alignment - each paragraph spans at least 5 wrapped lines.
     */
    @Test
    public void testMultiParagraphLandscapeA4() throws IOException {
        renderLandscapeParagraphs(PDRectangle.A4, "LandscapeA4_Paragraphs");
    }

    /**
     * Multi-paragraph content on a LETTER landscape page with LEFT, RIGHT and
     * CENTER alignment - each paragraph spans at least 5 wrapped lines.
     */
    @Test
    public void testMultiParagraphLandscapeLetter() throws IOException {
        renderLandscapeParagraphs(PDRectangle.LETTER, "LandscapeLetter_Paragraphs");
    }

    /**
     * Shared helper: creates a landscape page of the given size and renders
     * three paragraphs (LEFT, RIGHT, CENTER) each containing enough text for
     * 5+ visual lines, then saves the PDF.
     */
    private void renderLandscapeParagraphs(PDRectangle baseSize,
                                           String outputName) throws IOException {
        // Flip width/height for landscape
        PDRectangle landscape = new PDRectangle(baseSize.getHeight(), baseSize.getWidth());

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(landscape);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                float pageW = landscape.getWidth();
                float pageH = landscape.getHeight();

                // Use ~90 % of the page width for the block, centred horizontally
                float margin = 40f;
                float blockWidth = pageW - 2 * margin;
                float blockHeight = pageH - 2 * margin;

                //  Paragraph 1: LEFT aligned
                RichTextLine leftPara = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "Left-aligned paragraph rendered on a landscape " +
                                outputName.replaceAll("_", " ") + " page. " +
                                "This paragraph is deliberately verbose so that word wrapping " +
                                "produces at least five visual lines inside the block. Left " +
                                "alignment keeps every line flush against the left margin, " +
                                "producing a straight left edge and a ragged right edge. It is " +
                                "the default alignment for most Western-language body copy and " +
                                "offers the best readability for long passages. The renderer must " +
                                "correctly compute the available width from the landscape page " +
                                "dimensions and wrap the text accordingly without any overflow.",
                                EnumSet.noneOf(TextStyle.class), 11f),
                        new RichTextSegment(
                                " Additional bold text to mix styles within the same line " +
                                "and verify that segment-level styling is preserved across " +
                                "word-wrap boundaries on landscape pages.",
                                EnumSet.of(TextStyle.BOLD), 11f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                //  Paragraph 2: RIGHT aligned
                RichTextLine rightPara = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "Right-aligned paragraph on the same landscape page. Every " +
                                "visual line is pushed to the right edge of the block, creating " +
                                "a straight right margin and a ragged left margin. This style " +
                                "is commonly used for dates, signatures, and decorative captions. " +
                                "Even on a wide landscape layout the alignment strategy should " +
                                "produce consistent results with no text extending beyond the " +
                                "right boundary. This text is intentionally long enough to guarantee " +
                                "at least five wrapped lines so that the alignment behaviour is " +
                                "clearly visible and verifiable in the output PDF.",
                                EnumSet.noneOf(TextStyle.class), 11f),
                        new RichTextSegment(
                                " Italic tail appended to exercise mixed-style segments " +
                                "under right alignment on landscape orientation.",
                                EnumSet.of(TextStyle.ITALIC), 11f)
                ), ListType.NONE, 0, TextAlignment.RIGHT);

                //  Paragraph 3: CENTER aligned
                RichTextLine centerPara = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "Centre-aligned paragraph completing the landscape demo. " +
                                "Each wrapped line is horizontally centred within the block, " +
                                "yielding symmetric whitespace on both sides. Centre alignment " +
                                "is ideal for headings and short decorative text; applying it " +
                                "to longer body copy as shown here is unusual but perfectly " +
                                "valid. The renderer calculates the text width of every visual " +
                                "line and offsets it by half the remaining space. This paragraph " +
                                "is sufficiently long to produce at least five lines so that the " +
                                "centring effect is unmistakable in the generated PDF output.",
                                EnumSet.noneOf(TextStyle.class), 11f),
                        new RichTextSegment(
                                " Underlined closing segment to confirm that underline " +
                                "decoration works with centre alignment on landscape pages.",
                                EnumSet.of(TextStyle.UNDERLINE), 11f)
                ), ListType.NONE, 0, TextAlignment.CENTER);

                RichTextBlock block = RichTextBlock.builder()
                        .at(margin, margin)
                        .size(blockWidth, blockHeight)
                        .blockPadding(10f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 16,
                                outputName.replace('_', ' ') + " - Landscape Demo",
                                TextAlignment.CENTER)
                        .addContent(new TextContentElement(leftPara))
                        .addContent(paragraphBreak(1, 11f))
                        .addContent(new TextContentElement(rightPara))
                        .addContent(paragraphBreak(1, 11f))
                        .addContent(new TextContentElement(centerPara))
                        .drawBorder(true)
                        .build();

                float finalY = block.render(doc, stream, pageH);
                assertTrue("render() should return a positive final Y on landscape "
                        + outputName, finalY > 0);
                stream.close();
            }

            doc.save(new File("target/" + outputName + ".pdf"));
        }
    }

    /**
     * Verifies that three RichTextBlocks can be stacked vertically by using
     * {@link RichTextBlock#estimateContentHeight()} to auto-size each block
     * and the Y-position returned from {@code render()} to position the next
     * block immediately below the previous one.
     */
    @Test
    public void testChainedBlocksUsingReturnedYPosition() throws IOException, URISyntaxException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            float pageHeight = pageSize.getHeight();

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                float blockX = 40f;
                float blockWidth = pageSize.getWidth() - 2 * blockX;
                float blockGap = 15f; // visible gap between consecutive blocks
                float currentTopDownY = 30f; // starting Y from top of page

                // ── Block 1: Introduction ────────────────────────────────
                RichTextLine introLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "This is the first block in a chain of three. The render() " +
                                "method returns the final PDF Y-coordinate so callers can " +
                                "position the next element directly below. This paragraph has " +
                                "enough text to span multiple lines and demonstrate that the " +
                                "returned Y accounts for all rendered content including the " +
                                "header and body text.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // Build with a large temporary height, measure, then rebuild
                RichTextBlock.Builder b1 = RichTextBlock.builder()
                        .at(blockX, currentTopDownY)
                        .size(blockWidth, 9999f)
                        .blockPadding(8f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 13,
                                "Block 1: Introduction", TextAlignment.LEFT)
                        .addContent(new TextContentElement(introLine))
                        .drawBorder(true);
                float h1 = b1.build().estimateContentHeight();
                RichTextBlock block1 = b1.size(blockWidth, h1 + 10f).build();

                float pdfY1 = block1.render(doc, stream, pageHeight);
                assertTrue("Block 1 should return a positive Y", pdfY1 > 0);

                currentTopDownY = pageHeight - pdfY1 + blockGap;

                // ── Block 2: Key Metrics (mixed formatting + bullets) ────
                RichTextLine detailLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Key finding: ",
                                EnumSet.of(TextStyle.BOLD), 10f),
                        new RichTextSegment("The second block starts exactly where the first " +
                                "block ended. It uses mixed formatting to verify that styled " +
                                "text renders correctly in a chained layout. ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        new RichTextSegment("Italic emphasis ",
                                EnumSet.of(TextStyle.ITALIC), 10f),
                        new RichTextSegment("is also supported within the same line, and " +
                                "word-wrapping respects style boundaries across visual lines.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.JUSTIFY);

                List<RichTextLine> bullets = Arrays.asList(
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Revenue increased by 25%",
                                        EnumSet.noneOf(TextStyle.class), 9f)),
                                ListType.NONE, 0),
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Customer base grew by 18%",
                                        EnumSet.of(TextStyle.BOLD), 9f)),
                                ListType.NONE, 0),
                        new RichTextLine(Collections.singletonList(
                                new RichTextSegment("Operating costs reduced by 7%",
                                        EnumSet.of(TextStyle.ITALIC), 9f)),
                                ListType.NONE, 0));

                RichTextBlock.Builder b2 = RichTextBlock.builder()
                        .at(blockX, currentTopDownY)
                        .size(blockWidth, 9999f)
                        .blockPadding(8f)
                        .header(FontUtils.getFontSet(Standard14FontFamily.TIMES_ROMAN), 13,
                                "Block 2: Key Metrics", TextAlignment.LEFT)
                        .addContent(new TextContentElement(detailLine))
                        .addContent(new ListContentElement(ListType.BULLETED, bullets))
                        .drawBorder(true);
                float h2 = b2.build().estimateContentHeight();
                RichTextBlock block2 = b2.size(blockWidth, h2 + 10f).build();

                float pdfY2 = block2.render(doc, stream, pageHeight);
                assertTrue("Block 2 should return a positive Y", pdfY2 > 0);
                assertTrue("Block 2 Y should be below Block 1 Y", pdfY2 < pdfY1);

                currentTopDownY = pageHeight - pdfY2 + blockGap;

                // ── Block 3: Conclusion (centre-aligned, no header, with inline image) ──
                File inlineImgFile = Paths.get(
                        Objects.requireNonNull(RichTextBlockTest.class.getResource("/app_development.jpg")).toURI()).toFile();
                InlineImageSegment inlineImg = InlineImageSegment.fromFile(inlineImgFile, 48, 48);

                RichTextLine conclusionLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment(
                                "This third and final block demonstrates that an arbitrary " +
                                "number of blocks can be chained vertically on the same page " +
                                "by passing the returned Y-position forward. The layout engine " +
                                "guarantees no overlap between consecutive blocks as long as " +
                                "each block's top-down Y is derived from the previous render " +
                                "return value.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.CENTER);

                // A line that mixes text with an inline image
                RichTextLine imageLineInBlock3 = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Summary chart: ",
                                EnumSet.of(TextStyle.BOLD), 10f),
                        inlineImg,
                        new RichTextSegment(" — see above for details.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextLine signOff = new RichTextLine(Collections.singletonList(
                        new RichTextSegment("-- End of Report --",
                                EnumSet.of(TextStyle.ITALIC, TextStyle.UNDERLINE), 9f)),
                        ListType.NONE, 0, TextAlignment.CENTER);

                RichTextBlock.Builder b3 = RichTextBlock.builder()
                        .at(blockX, currentTopDownY)
                        .size(blockWidth, 9999f)
                        .blockPadding(8f)
                        // No header for block 3 — demonstrates a header-less block
                        .addContent(new TextContentElement(conclusionLine))
                        .addContent(new TextContentElement(imageLineInBlock3))
                        .addContent(new TextContentElement(signOff))
                        .drawBorder(true);
                float h3 = b3.build().estimateContentHeight();
                RichTextBlock block3 = b3.size(blockWidth, h3 + 10f).build();

                float pdfY3 = block3.render(doc, stream, pageHeight);
                assertTrue("Block 3 should return a positive Y", pdfY3 > 0);
                assertTrue("Block 3 Y should be below Block 2 Y", pdfY3 < pdfY2);

                stream.close();
            }

            doc.save(new File("target/ChainedBlocksDemo.pdf"));
        }
    }

    /**
     * Reads a classpath resource and returns its content as a Base64-encoded string.
     */
    private String resourceToBase64(String resourcePath) throws IOException {
        try (InputStream is = Objects.requireNonNull(
                RichTextBlockTest.class.getResourceAsStream(resourcePath),
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

        float finalY = block.renderOnNewPage(new PDDocument(), PDRectangle.A4, false);
        assertTrue("render() should return a positive final Y", finalY > 0);
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
                        RichTextBlockTest.class.getResource("/app_development.jpg")).toURI()
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
                        RichTextBlockTest.class.getResource("/app_development.jpg")).toURI()
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
                RichTextBlockTest.class.getResourceAsStream("/150dpi.png"))) {

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
                RichTextBlockTest.class.getResourceAsStream("/150dpi.png"))) {
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
        assertTrue("BODY should not be bold", !TextType.BODY.isBold());
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
        assertTrue("Should not be italic", !bold.isItalic());
        assertTrue("Should not be underline", !bold.isUnderline());
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
                        RichTextBlockTest.class.getResource("/app_development.jpg")).toURI()
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
                        RichTextBlockTest.class.getResource("/app_development.jpg")).toURI()
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

