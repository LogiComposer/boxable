package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * A {@link ContentElement} that renders a heading (Header1 or Header2) within
 * the content flow of a {@link RichTextBlock}.
 * <p>
 * Unlike the block-level header set via
 * {@link RichTextBlock.Builder#header(HeaderFont, float, String, TextAlignment)},
 * a {@code HeaderContentElement} is an inline content element that can appear
 * anywhere in the content list — allowing multiple headings at different levels
 * within a single block.
 * </p>
 *
 * <h3>Rendering behaviour</h3>
 * <ul>
 *   <li>Text is rendered in <strong>bold</strong> at the size specified by
 *       the {@link TextType} (or overridden via the builder).</li>
 *   <li>A thin underline is drawn beneath each wrapped header line.</li>
 *   <li>Alignment defaults to {@link TextAlignment#LEFT} but can be
 *       configured.</li>
 *   <li>Extra vertical spacing is added after the heading.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ContentElement h1 = new HeaderContentElement.Builder("Chapter One", TextType.HEADER1)
 *         .fontFamily(HeaderFont.TIMES_ROMAN)
 *         .alignment(TextAlignment.LEFT)
 *         .build();
 *
 * ContentElement h2 = new HeaderContentElement.Builder("Section 1.1", TextType.HEADER2)
 *         .build();
 * }</pre>
 */
public final class HeaderContentElement implements ContentElement {

    private static final float LINE_SPACING = 1.4f;
    private static final float UNDERLINE_THICKNESS = 1.0f;
    private static final float UNDERLINE_OFFSET = -2f;

    private final String text;
    private final TextType textType;
    private final float fontSize;
    private final HeaderFont fontFamily;
    private final TextAlignment alignment;

    private HeaderContentElement(Builder builder) {
        this.text = builder.text;
        this.textType = builder.textType;
        this.fontSize = builder.fontSize;
        this.fontFamily = builder.fontFamily;
        this.alignment = builder.alignment;
    }

    // ── ContentElement contract ──────────────────────────────────────────

    @Override
    public float estimateHeight(float availableWidth) throws IOException {
        PDFont font = resolveFont();
        List<String> wrapped = WordWrapUtil.wrap(text, font, fontSize, availableWidth);
        float lineHeight = fontSize * LINE_SPACING;
        // Each wrapped line + extra spacing after heading
        return lineHeight * wrapped.size() + fontSize * 0.3f;
    }

    @Override
    public void render(RenderContext ctx) throws IOException {
        if (text == null || text.isEmpty()) return;

        PDFont font = resolveFont();
        List<String> wrappedLines = WordWrapUtil.wrap(text, font, fontSize,
                ctx.getInnerWidth());

        for (String line : wrappedLines) {
            float lineHeight = fontSize * LINE_SPACING;
            if (!ctx.hasSpace(lineHeight)) {
                ctx.markOverflow();
                return;
            }

            ctx.advanceCursor(fontSize);

            float textWidth = WordWrapUtil.textWidth(line, font, fontSize);
            float xPos = computeAlignedX(textWidth, ctx.getLeft(),
                    ctx.getInnerWidth(), alignment);

            PageContentStreamOptimized stream = ctx.getStream();
            stream.setNonStrokingColor(Color.BLACK);
            stream.setFont(font, fontSize);
            stream.newLineAt(xPos, ctx.getCursorY());
            stream.showText(line);

            // Draw underline beneath the header text
            stream.endText();
            stream.setStrokingColor(Color.BLACK);
            stream.setLineWidth(UNDERLINE_THICKNESS);
            stream.moveTo(xPos, ctx.getCursorY() + UNDERLINE_OFFSET);
            stream.lineTo(xPos + textWidth, ctx.getCursorY() + UNDERLINE_OFFSET);
            stream.stroke();

            ctx.advanceCursor(fontSize * 0.3f);
        }

        // Extra spacing after header
        ctx.advanceCursor(fontSize * 0.3f);
    }

    // ── Internals ────────────────────────────────────────────────────────

    private PDFont resolveFont() {
        EnumSet<TextStyle> styles = textType.isBold()
                ? EnumSet.of(TextStyle.BOLD)
                : EnumSet.noneOf(TextStyle.class);
        return FontResolver.resolveHeader(fontFamily, styles.contains(TextStyle.BOLD), false);
    }

    private float computeAlignedX(float contentWidth, float regionStart,
                                   float regionWidth, TextAlignment align) {
        switch (align) {
            case CENTER: return regionStart + (regionWidth - contentWidth) / 2f;
            case RIGHT:  return regionStart + regionWidth - contentWidth;
            default:     return regionStart;
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public String getText()             { return text; }
    public TextType getTextType()       { return textType; }
    public float getFontSize()          { return fontSize; }
    public HeaderFont getFontFamily()   { return fontFamily; }
    public TextAlignment getAlignment() { return alignment; }

    // ════════════════════════════════════════════════════════════════════════
    //  BUILDER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Fluent builder for {@link HeaderContentElement}.
     */
    public static final class Builder {
        private final String text;
        private final TextType textType;
        private float fontSize;
        private HeaderFont fontFamily = HeaderFont.HELVETICA;
        private TextAlignment alignment = TextAlignment.LEFT;

        /**
         * @param text     the heading text
         * @param textType the heading level ({@link TextType#HEADER1} or
         *                 {@link TextType#HEADER2})
         */
        public Builder(String text, TextType textType) {
            this.text = text;
            this.textType = textType != null ? textType : TextType.HEADER1;
            this.fontSize = this.textType.getDefaultFontSize();
        }

        /** Overrides the default font size for this header. */
        public Builder fontSize(float fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        /** Sets the font family (default {@link HeaderFont#HELVETICA}). */
        public Builder fontFamily(HeaderFont fontFamily) {
            this.fontFamily = fontFamily != null ? fontFamily : HeaderFont.HELVETICA;
            return this;
        }

        /** Sets horizontal alignment (default {@link TextAlignment#LEFT}). */
        public Builder alignment(TextAlignment alignment) {
            this.alignment = alignment != null ? alignment : TextAlignment.LEFT;
            return this;
        }

        /** Builds the immutable {@link HeaderContentElement}. */
        public HeaderContentElement build() {
            return new HeaderContentElement(this);
        }
    }
}
