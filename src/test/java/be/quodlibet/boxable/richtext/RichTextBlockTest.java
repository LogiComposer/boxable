package be.quodlibet.boxable.richtext;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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

    private RichTextBlock buildMainBlock() {
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

        BufferedImage testImg = createTestImage(300, 80, Color.BLUE);

        return RichTextBlock.builder()
                .at(30, 30).size(370, 500)
                .header(HeaderFont.TIMES_ROMAN, 16, "Quarterly Report Summary",
                        TextAlignment.CENTER)
                .addContent(new TextContentElement(paragraph))
                .addContent(new TextContentElement(subtitle))
                .addContent(new TextContentElement(dateLine))
                .addContent(new ImageContentElement.Builder(testImg)
                        .size(200, 60).alignment(TextAlignment.CENTER)
                        .cacheKey("blue-gradient").build())
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

    private BufferedImage createTestImage(int w, int h, Color base) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, base.brighter(), w, h, base.darker()));
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Test Image", 10, h / 2 + 5);
        g.dispose();
        return img;
    }
}

