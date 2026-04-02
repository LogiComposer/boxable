package be.quodlibet.boxable.richtext;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class RichTextBlockTest {

    @Test
    public void testFullRichTextBlockRendering() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDRectangle landscape = new PDRectangle(
                    PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());
            PDPage page1 = new PDPage(landscape);
            doc.addPage(page1);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page1)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);
                buildMainBlock().render(doc, stream, landscape.getHeight());
                buildSideBlock().render(doc, stream, landscape.getHeight());
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

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                // ── Block 1: Narrow block (150pt wide) with long text ────
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
                        .header(HeaderFont.HELVETICA, 11, "Narrow Block", TextAlignment.CENTER)
                        .addContent(new TextContentElement(longLine))
                        .drawBorder(true)
                        .build();
                narrowBlock.render(doc, stream, pageSize.getHeight());

                // ── Block 2: Mixed-style segments that collectively overflow ─
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
                        .header(HeaderFont.TIMES_ROMAN, 11, "Mixed Styles", TextAlignment.CENTER)
                        .addContent(new TextContentElement(mixedLine))
                        .drawBorder(true)
                        .build();
                mixedBlock.render(doc, stream, pageSize.getHeight());

                // ── Block 3: Very long single word (character-level break) ──
                RichTextLine longWordLine = new RichTextLine(Collections.singletonList(
                        new RichTextSegment(
                                "Supercalifragilisticexpialidocious_ExtraordinarilyLongWordThatCannotFitOnOneLine " +
                                "followed by normal words.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                RichTextBlock longWordBlock = RichTextBlock.builder()
                        .at(400, 30).size(160, 400)
                        .header(HeaderFont.COURIER, 9, "Long Word Break", TextAlignment.CENTER)
                        .addContent(new TextContentElement(longWordLine))
                        .drawBorder(true)
                        .build();
                longWordBlock.render(doc, stream, pageSize.getHeight());

                stream.close();
            }

            doc.save(new File("target/RichTextBlockWrappingTest.pdf"));
        }
    }

    private RichTextBlock buildMainBlock() throws IOException {
        RichTextLine paragraph = new RichTextLine(Arrays.asList(
                new RichTextSegment("Revenue grew by ", EnumSet.noneOf(TextStyle.class), 10f),
                new RichTextSegment("25%", EnumSet.of(TextStyle.BOLD), 10f),
                new RichTextSegment(" compared to last quarter.", EnumSet.noneOf(TextStyle.class), 10f)
        ), ListType.NONE, 0, TextAlignment.JUSTIFY);

        RichTextLine subtitle = new RichTextLine(Collections.singletonList(
                new RichTextSegment("— Confidential —",
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
        File jpgFile = new File(
                Objects.requireNonNull(RichTextBlockTest.class.getResource("/app_development.jpg")).getFile());

        // Load PNG image from InputStream
        InputStream pngStream = Objects.requireNonNull(
                RichTextBlockTest.class.getResourceAsStream("/150dpi.png"));

        return RichTextBlock.builder()
                .at(30, 30).size(370, 500)
                .header(HeaderFont.TIMES_ROMAN, 16, "Quarterly Report Summary",
                        TextAlignment.CENTER)
                .addContent(new TextContentElement(paragraph))
                .addContent(new TextContentElement(subtitle))
                .addContent(new TextContentElement(dateLine))
                .addContent(new ImageContentElement.Builder(jpgFile)
                        .size(200, 60).alignment(TextAlignment.CENTER)
                        .cacheKey("jpg-app-dev").build())
                .addContent(new ImageContentElement.Builder(pngStream)
                        .size(200, 60).alignment(TextAlignment.CENTER)
                        .cacheKey("png-150dpi").build())
                .addContent(new ListContentElement(ListType.BULLETED, bullets))
                .addContent(new ListContentElement(ListType.NUMBERED, numbered))
                .showOverflowIndicator(true)
                .drawBorder(true)
                .build();
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
                .header(HeaderFont.COURIER, 12, "Side Notes", TextAlignment.LEFT)
                .addContent(new TextContentElement(justifiedParagraph))
                .addContent(new TextContentElement(note))
                .showOverflowIndicator(true)
                .drawBorder(true)
                .build();
    }

    private RichTextBlock buildOverflowBlock() {
        RichTextBlock.Builder builder = RichTextBlock.builder()
                .at(50, 50).size(495, 200)
                .header(HeaderFont.HELVETICA, 14, "Overflow Demonstration",
                        TextAlignment.CENTER)
                .showOverflowIndicator(true)
                .drawBorder(true);

        for (int i = 1; i <= 20; i++) {
            RichTextLine line = new RichTextLine(Collections.singletonList(
                    new RichTextSegment(
                            "Line " + i + ": Lorem ipsum dolor sit amet, " +
                            "consectetur adipiscing elit."+
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
    public void testInlineImagesWithText() throws IOException {
        File pngFile = new File(
                Objects.requireNonNull(RichTextBlockTest.class.getResource("/150dpi.png")).getFile());
        File jpgFile = new File(
                Objects.requireNonNull(RichTextBlockTest.class.getResource("/app_development.jpg")).getFile());

        InlineImageSegment pngInline = InlineImageSegment.fromFile(pngFile, 30, 12);
        InlineImageSegment jpgInline = InlineImageSegment.fromFile(jpgFile, 30, 12);

        try (PDDocument doc = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            try (PDPageContentStream raw = new PDPageContentStream(doc, page)) {
                PageContentStreamOptimized stream = new PageContentStreamOptimized(raw);

                // ── 1. Text + image + text (image in the middle) ─────────
                RichTextLine middleLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Revenue grew by ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline,
                        new RichTextSegment(" compared to last quarter.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // ── 2. Image at the start ────────────────────────────────
                RichTextLine startLine = new RichTextLine(Arrays.asList(
                        jpgInline,
                        new RichTextSegment(" This text follows a JPG image at the start.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // ── 3. Image at the end ──────────────────────────────────
                RichTextLine endLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("This text precedes a PNG image: ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // ── 4. Multiple images side-by-side with text ────────────
                RichTextLine sideBySide = new RichTextLine(Arrays.asList(
                        pngInline,
                        new RichTextSegment(" between ",
                                EnumSet.of(TextStyle.BOLD), 10f),
                        jpgInline,
                        new RichTextSegment(" and more ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline
                ), ListType.NONE, 0, TextAlignment.LEFT);

                // ── 5. Image-only line ───────────────────────────────────
                RichTextLine imageOnly = new RichTextLine(Arrays.asList(
                        pngInline, jpgInline, pngInline
                ), ListType.NONE, 0, TextAlignment.CENTER);

                // ── 6. Centered alignment with inline image ──────────────
                RichTextLine centeredLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Centered: ",
                                EnumSet.of(TextStyle.ITALIC), 10f),
                        jpgInline,
                        new RichTextSegment(" label",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.CENTER);

                // ── 7. Right-aligned with inline image ───────────────────
                RichTextLine rightLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Right: ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline,
                        new RichTextSegment(" end",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.RIGHT);

                // ── 8. Justified with inline images ──────────────────────
                RichTextLine justifiedLine = new RichTextLine(Arrays.asList(
                        new RichTextSegment("Justified text with ",
                                EnumSet.noneOf(TextStyle.class), 10f),
                        pngInline,
                        new RichTextSegment(" inline image spread across the full width.",
                                EnumSet.noneOf(TextStyle.class), 10f)
                ), ListType.NONE, 0, TextAlignment.JUSTIFY);

                RichTextBlock block = RichTextBlock.builder()
                        .at(30, 30).size(500, 700)
                        .header(HeaderFont.HELVETICA, 14,
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
                stream.close();
            }

            doc.save(new File("target/InlineImageDemo.pdf"));
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
                        .header(HeaderFont.HELVETICA, 14,
                                "Base64 Image Demo", TextAlignment.CENTER)
                        .addContent(new TextContentElement(intro))
                        // Raw Base64 PNG
                        .addContent(ImageContentElement.Builder.fromBase64(pngBase64)
                                .size(200, 60).alignment(TextAlignment.CENTER)
                                .cacheKey("b64-png").build())
                        // Data-URI Base64 JPG
                        .addContent(ImageContentElement.Builder.fromBase64(jpgBase64DataUri)
                                .size(200, 60).alignment(TextAlignment.CENTER)
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

                // ── Paragraph 1: LEFT aligned (5+ lines) ────────────────
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

                // ── Paragraph 2: RIGHT aligned (5+ lines) ───────────────
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

                // ── Paragraph 3: CENTER aligned (5+ lines) ──────────────
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
                        .header(HeaderFont.HELVETICA, 14,
                                "Multi-Paragraph Alignment Demo", TextAlignment.CENTER)
                        .addContent(new TextContentElement(leftPara))
                        .addContent(new TextContentElement(rightPara))
                        .addContent(new TextContentElement(centerPara))
                        .drawBorder(true)
                        .build();

                float finalY = block.render(doc, stream, pageSize.getHeight());
                // Verify render() returns a usable Y-position
                assert finalY > 0 : "render() should return a positive final Y position";
                stream.close();
            }

            doc.save(new File("target/MultiParagraphAlignmentDemo.pdf"));
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

}


