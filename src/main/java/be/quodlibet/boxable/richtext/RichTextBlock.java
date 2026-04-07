package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.FontSet;
import be.quodlibet.boxable.Standard14FontFamily;
import be.quodlibet.boxable.utils.FontUtils;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * A bounded rich-text area that positions mixed content (styled text, images,
 * lists) within a fixed rectangle on the page.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * RichTextBlock block = RichTextBlock.builder()
 *         .at(100, 80)
 *         .size(400, 300)
 *         .header(FontUtils.getFontSet(Standard14FontFamily.HELVETICA), 16, "Report", TextAlignment.CENTER)
 *         .addContent(new TextContentElement(...))
 *         .addContent(new ImageContentElement.Builder(img).alignment(TextAlignment.CENTER).build())
 *         .addContent(new ListContentElement(ListType.BULLETED, items))
 *         .showOverflowIndicator(true)
 *         .build();
 *
 * block.render(document, contentStream, pageHeight);
 * // finalY is the PDF Y-coordinate where content ended — use it to
 * // position the next element below this block.
 * }</pre>
 *
 * <h3>Design</h3>
 * <ul>
 *   <li><strong>Builder pattern</strong> — fluent, readable construction.</li>
 *   <li><strong>Composite</strong> — content elements are polymorphic.</li>
 *   <li><strong>Strategy</strong> — alignment is delegated per-line.</li>
 *   <li><strong>SRP</strong> — this class owns bounds and header; content
 *       elements own their own rendering logic.</li>
 * </ul>
 */
public final class RichTextBlock {

    /** Default internal padding between the block edge and content (in points). */
    private static final float DEFAULT_BLOCK_PADDING = 4f;
    private static final float LINE_SPACING = 1.4f;
    private static final float UNDERLINE_THICKNESS = 1.0f;
    private static final float UNDERLINE_OFFSET = -2f;

    // ── Block bounds (logical, top-down) ─────────────────────────────────
    private final float blockX;
    private final float blockY;
    private final float blockWidth;
    private final float blockHeight;
    private final float blockPadding;


    // ── Header ───────────────────────────────────────────────────────────
    private final FontSet headerFont;
    private final float headerFontSize;
    private final String headerText;
    private final TextAlignment headerAlignment;

    // ── Content ──────────────────────────────────────────────────────────
    private final List<ContentElement> content;
    private final boolean showOverflowIndicator;

    // ── Debug ────────────────────────────────────────────────────────────
    private final boolean drawBorder;

    private RichTextBlock(Builder builder) {
        this.blockX = builder.blockX;
        this.blockY = builder.blockY;
        this.blockWidth = builder.blockWidth;
        this.blockHeight = builder.blockHeight;
        this.blockPadding = builder.blockPadding;
        this.headerFont = builder.headerFont;
        this.headerFontSize = builder.headerFontSize;
        this.headerText = builder.headerText;
        this.headerAlignment = builder.headerAlignment;
        this.content = Collections.unmodifiableList(new ArrayList<>(builder.content));
        this.showOverflowIndicator = builder.showOverflowIndicator;
        this.drawBorder = builder.drawBorder;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  RENDERING
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Renders this block onto an existing page via the provided content stream.
     *
     * @param document   the PDF document (needed for image/font creation)
     * @param stream     the optimized content stream for the target page
     * @param pageHeight the height of the current page in points (needed for
     *                   coordinate conversion from logical top-down to PDF bottom-up)
     * @return the final cursor Y-position (in PDF bottom-up coordinates) after
     *         rendering completes; callers can use this to position subsequent
     *         elements below this block
     * @throws IOException if writing to the stream fails
     */
    public float render(PDDocument document, PageContentStreamOptimized stream,
                       float pageHeight) throws IOException {

        // Convert logical (top-down) coordinates to PDF (bottom-up)
        float pdfLeft   = blockX + blockPadding;
        float pdfTop    = pageHeight - blockY - blockPadding;
        float pdfRight  = blockX + blockWidth - blockPadding;
        float pdfBottom = pageHeight - blockY - blockHeight + blockPadding;

        RenderContext ctx = new RenderContext(document, stream,
                pdfLeft, pdfRight, pdfTop, pdfBottom);

        // Optional debug border
        if (drawBorder) {
            drawBlockBorder(stream, blockX,
                    pageHeight - blockY - blockHeight, blockWidth, blockHeight);
        }

        // ── Header ───────────────────────────────────────────────────────
        renderHeader(ctx);

        // ── Content elements ─────────────────────────────────────────────
        for (ContentElement element : content) {
            if (ctx.isOverflow()) break;
            element.render(ctx);
        }

        // ── Overflow indicator ───────────────────────────────────────────
        if (ctx.isOverflow() && showOverflowIndicator) {
            renderOverflowIndicator(ctx, pdfBottom);
        }

        // Ensure text mode is closed
        stream.endText();

        // Return the lower of cursor position and block bottom so that
        // chained blocks never overlap even when content is shorter than
        // the declared block height.
        return Math.min(ctx.getCursorY(), pdfBottom);
    }

    /**
     * Convenience method: creates a new page, renders this block, and closes
     * the content stream.
     *
     * @param document  the PDF document
     * @param pageSize  the page rectangle (e.g., {@code PDRectangle.LETTER})
     * @param landscape whether to use landscape orientation
     * @return the final cursor Y-position (in PDF bottom-up coordinates) after
     *         rendering completes
     * @throws IOException if writing fails
     */
    public float renderOnNewPage(PDDocument document, PDRectangle pageSize,
                                boolean landscape) throws IOException {
        PDRectangle effective = landscape
                ? new PDRectangle(pageSize.getHeight(), pageSize.getWidth())
                : pageSize;
        PDPage page = new PDPage(effective);
        document.addPage(page);

        PageContentStreamOptimized stream =
                new PageContentStreamOptimized(new PDPageContentStream(document, page));
        try {
            float finalY = render(document, stream, effective.getHeight());
            return finalY;
        } finally {
            stream.close();
        }
    }

    // ── Header rendering ─────────────────────────────────────────────────

    private void renderHeader(RenderContext ctx) throws IOException {
        if (headerText == null || headerText.isEmpty()) return;

        PDFont font = FontResolver.resolveHeader(headerFont, true, false);
        float fontSize = headerFontSize;
        TextAlignment align = headerAlignment != null ? headerAlignment : TextAlignment.LEFT;

        List<String> headerLines = WordWrapUtil.wrap(headerText, font, fontSize,
                ctx.getInnerWidth());

        for (int i = 0; i < headerLines.size(); i++) {
            String line = headerLines.get(i);
            float lineHeight = fontSize * LINE_SPACING;
            if (!ctx.hasSpace(lineHeight)) {
                ctx.markOverflow();
                return;
            }

            ctx.advanceCursor(fontSize);

            float textWidth = WordWrapUtil.textWidth(line, font, fontSize);
            float xPos = computeAlignedX(textWidth, ctx.getLeft(),
                    ctx.getInnerWidth(), align);

            PageContentStreamOptimized stream = ctx.getStream();
            stream.setNonStrokingColor(Color.BLACK);
            stream.setFont(font, fontSize);
            stream.newLineAt(xPos, ctx.getCursorY());
            stream.showText(line);

            // Underline the header
            stream.endText();
            stream.setStrokingColor(Color.BLACK);
            stream.setLineWidth(UNDERLINE_THICKNESS);
            stream.moveTo(xPos, ctx.getCursorY() + UNDERLINE_OFFSET);
            stream.lineTo(xPos + textWidth, ctx.getCursorY() + UNDERLINE_OFFSET);
            stream.stroke();

            // Inter-line spacing only between consecutive wrapped lines
            if (i < headerLines.size() - 1) {
                ctx.advanceCursor(fontSize * 0.3f);
            }
        }

        // Extra spacing after header
        ctx.advanceCursor(headerFontSize * 0.3f);
    }

    // ── Overflow indicator ───────────────────────────────────────────────

    private void renderOverflowIndicator(RenderContext ctx, float bottomY) throws IOException {
        float indicatorSize = 8f;
        PDFont font = FontResolver.resolve(EnumSet.noneOf(TextStyle.class));
        PageContentStreamOptimized stream = ctx.getStream();
        stream.setFont(font, indicatorSize);
        stream.setNonStrokingColor(Color.GRAY);
        stream.newLineAt(ctx.getLeft(), bottomY);
        stream.showText("…");
    }

    // ── Debug border ─────────────────────────────────────────────────────

    private void drawBlockBorder(PageContentStreamOptimized stream,
                                 float x, float y, float w, float h) throws IOException {
        stream.endText();
        stream.setStrokingColor(new Color(200, 200, 200));
        stream.setLineWidth(0.5f);
        stream.addRect(x, y, w, h);
        stream.stroke();
        stream.setStrokingColor(Color.BLACK);
    }

    // ── Alignment helper ─────────────────────────────────────────────────

    private float computeAlignedX(float contentWidth, float regionStart,
                                  float regionWidth, TextAlignment align) {
        switch (align) {
            case CENTER: return regionStart + (regionWidth - contentWidth) / 2f;
            case RIGHT:  return regionStart + regionWidth - contentWidth;
            default:     return regionStart;
        }
    }

    // ── Content height estimation ────────────────────────────────────────

    /**
     * Estimates the total height this block's content (header + body elements +
     * padding) would consume if there were no height limit.
     * <p>
     * Callers can use this to size the block to exactly fit its content:
     * <pre>{@code
     * RichTextBlock.Builder b = RichTextBlock.builder()
     *         .at(x, y).size(width, 9999f) // temporary large height
     *         .addContent(...);
     * float h = b.build().estimateContentHeight();
     * RichTextBlock block = b.size(width, h).build();
     * }</pre>
     *
     * @return the estimated content height in points (including top and bottom padding)
     * @throws IOException if font metrics cannot be read
     */
    public float estimateContentHeight() throws IOException {
        float innerWidth = blockWidth - 2 * blockPadding;
        float total = 0;

        // Header contribution
        if (headerText != null && !headerText.isEmpty()) {
            PDFont font = FontResolver.resolveHeader(headerFont, true, false);
            List<String> headerLines = WordWrapUtil.wrap(headerText, font, headerFontSize, innerWidth);
            // Each wrapped line contributes fontSize of text height
            total += headerFontSize * headerLines.size();
            // Inter-line spacing between consecutive wrapped lines only
            total += headerFontSize * 0.3f * Math.max(0, headerLines.size() - 1);
            // Post-header gap
            total += headerFontSize * 0.3f;
        }

        // Content elements contribution
        for (ContentElement element : content) {
            total += element.estimateHeight(innerWidth);
        }

        // Top + bottom padding
        total += 2 * blockPadding;

        return total;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public float getBlockX()       { return blockX; }
    public float getBlockY()       { return blockY; }
    public float getBlockWidth()   { return blockWidth; }
    public float getBlockHeight()  { return blockHeight; }
    public float getBlockPadding() { return blockPadding; }
    public List<ContentElement> getContent() { return content; }

    // ============================== BUILDER ===============================
    /**
     * Creates a new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link RichTextBlock}.
     */
    public static final class Builder {
        private float blockX;
        private float blockY;
        private float blockWidth = 400;
        private float blockHeight = 300;
        private float blockPadding = DEFAULT_BLOCK_PADDING;
        private FontSet headerFont = FontUtils.getFontSet(Standard14FontFamily.HELVETICA);
        private float headerFontSize = 14f;
        private String headerText;
        private TextAlignment headerAlignment = TextAlignment.LEFT;
        private final List<ContentElement> content = new ArrayList<>();
        private boolean showOverflowIndicator = true;
        private boolean drawBorder = false;

        private Builder() {}

        /**
         * Sets the top-left position of the block (logical top-down coordinates).
         *
         * @param x distance from the left edge of the page
         * @param y distance from the top edge of the page
         */
        public Builder at(float x, float y) {
            this.blockX = x;
            this.blockY = y;
            return this;
        }

        /**
         * Sets the block dimensions.
         *
         * @param width  block width in points
         * @param height block height in points
         */
        public Builder size(float width, float height) {
            this.blockWidth = width;
            this.blockHeight = height;
            return this;
        }

        /**
         * Sets the internal padding between the block edge and content.
         * Defaults to {@value DEFAULT_BLOCK_PADDING} points if not specified.
         *
         * @param padding padding in points (must be non-negative)
         */
        public Builder blockPadding(float padding) {
            if (padding < 0) {
                throw new IllegalArgumentException("Block padding must be non-negative");
            }
            this.blockPadding = padding;
            return this;
        }


        /**
         * Configures the block header.
         *
         * @param font      the header font set
         * @param fontSize  font size in points
         * @param text      the header text
         * @param alignment horizontal alignment of the header
         */
        public Builder header(FontSet font, float fontSize, String text,
                              TextAlignment alignment) {
            this.headerFont = font;
            this.headerFontSize = fontSize;
            this.headerText = text;
            this.headerAlignment = alignment;
            return this;
        }

        /**
         * Adds a content element (text, image, or list).
         */
        public Builder addContent(ContentElement element) {
            this.content.add(element);
            return this;
        }

        /**
         * Adds all content elements from the given list.
         */
        public Builder addAllContent(List<? extends ContentElement> elements) {
            this.content.addAll(elements);
            return this;
        }

        /**
         * Whether to show an ellipsis (…) when content overflows.
         */
        public Builder showOverflowIndicator(boolean show) {
            this.showOverflowIndicator = show;
            return this;
        }

        /**
         * Whether to draw a light-gray debug border around the block bounds.
         */
        public Builder drawBorder(boolean draw) {
            this.drawBorder = draw;
            return this;
        }

        /**
         * Builds the immutable {@link RichTextBlock}.
         */
        public RichTextBlock build() {
            if (blockWidth <= 0 || blockHeight <= 0) {
                throw new IllegalStateException("Block width and height must be positive");
            }
            return new RichTextBlock(this);
        }
    }
}
