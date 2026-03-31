package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.richtext.alignment.AlignmentStrategy;
import be.quodlibet.boxable.richtext.alignment.AlignmentStrategyFactory;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * A {@link ContentElement} representing one or more logical lines of styled text.
 * <p>
 * Word-wrapping is performed lazily against the available width during
 * {@link #estimateHeight} / {@link #render}.
 * </p>
 * <p>
 * Alignment is delegated to an {@link AlignmentStrategy} (Strategy pattern).
 * </p>
 */
public final class TextContentElement implements ContentElement {

    /** Standard line-spacing multiplier (1.4 × font size). */
    static final float LINE_SPACING = 1.4f;

    /** Indentation applied to list item text (in points). */
    static final float LIST_INDENT = 15f;
    /** Horizontal offset for the bullet character from the indent start. */
    static final float BULLET_OFFSET = 5f;

    private final List<RichTextLine> lines;

    /**
     * Creates a text element from one or more pre-built lines.
     *
     * @param lines the rich text lines
     */
    public TextContentElement(List<RichTextLine> lines) {
        this.lines = lines != null
                ? Collections.unmodifiableList(new ArrayList<>(lines))
                : Collections.emptyList();
    }

    /** Convenience constructor for a single line. */
    public TextContentElement(RichTextLine line) {
        this(Collections.singletonList(line));
    }

    // ── ContentElement contract ────────────────────────────────────────��─

    @Override
    public float estimateHeight(float availableWidth) throws IOException {
        float total = 0;
        for (RichTextLine line : lines) {
            total += estimateLineHeight(line, availableWidth);
        }
        return total;
    }

    @Override
    public void render(RenderContext ctx) throws IOException {
        for (RichTextLine line : lines) {
            if (ctx.isOverflow()) return;
            renderLine(ctx, line);
        }
    }

    // ── Internals ────────────────────────────────────────────────────────

    private float estimateLineHeight(RichTextLine line, float availableWidth) throws IOException {
        float fontSize = line.getMaxFontSize();
        float lineHeight = fontSize * LINE_SPACING;

        // Approximate the number of visual lines after wrapping
        float totalWidth = 0;
        for (LineElement el : line.getElements()) {
            totalWidth += el.getWidth();
        }
        int visualLines = Math.max(1, (int) Math.ceil(totalWidth / availableWidth));
        return lineHeight * visualLines;
    }

    private void renderLine(RenderContext ctx, RichTextLine line) throws IOException {
        float fontSize = line.getMaxFontSize();
        float lineHeight = fontSize * LINE_SPACING;
        float contentStartX = ctx.getLeft();
        float contentWidth = ctx.getInnerWidth();

        // Handle list indentation
        String prefix = null;
        PDFont prefixFont = FontResolver.resolve(EnumSet.noneOf(TextStyle.class));
        if (line.getListType() == ListType.BULLETED) {
            prefix = "• ";
            contentStartX += LIST_INDENT;
            contentWidth -= LIST_INDENT;
        } else if (line.getListType() == ListType.NUMBERED) {
            prefix = line.getListIndex() + ". ";
            contentStartX += LIST_INDENT;
            contentWidth -= LIST_INDENT;
        }

        // Word-wrap all elements (text segments + inline images), then render line by line
        List<List<LineElement>> wrappedVisualLines =
                wrapElements(line.getElements(), contentWidth);

        AlignmentStrategy strategy = AlignmentStrategyFactory.get(line.getAlignment());

        for (int i = 0; i < wrappedVisualLines.size(); i++) {
            if (!ctx.hasSpace(lineHeight)) {
                ctx.markOverflow();
                return;
            }
            ctx.advanceCursor(lineHeight);

            // Draw list prefix on the first visual line only
            if (i == 0 && prefix != null) {
                float prefixX = ctx.getLeft()
                        + (line.getListType() == ListType.BULLETED
                        ? BULLET_OFFSET : 0);
                ctx.getStream().setFont(prefixFont, fontSize);
                ctx.getStream().setNonStrokingColor(java.awt.Color.BLACK);
                ctx.getStream().newLineAt(prefixX, ctx.getCursorY());
                ctx.getStream().showText(prefix);
            }

            // Render the visual line elements using the chosen alignment strategy
            List<LineElement> visualElements = wrappedVisualLines.get(i);

            // For JUSTIFY: don't justify the last visual line
            if (line.getAlignment() == TextAlignment.JUSTIFY
                    && i == wrappedVisualLines.size() - 1) {
                AlignmentStrategyFactory.get(TextAlignment.LEFT)
                        .renderLine(ctx, visualElements, ctx.getCursorY(),
                                contentStartX, contentWidth);
            } else {
                strategy.renderLine(ctx, visualElements, ctx.getCursorY(),
                        contentStartX, contentWidth);
            }
        }
    }

    /**
     * Wraps a list of elements into visual lines that fit within {@code maxWidth}.
     * Each visual line is itself a list of {@link LineElement}s.
     * <p>
     * Text segments are split at word boundaries when they exceed the available width.
     * Inline images are treated as atomic, unsplittable units — if an image does not
     * fit on the current line it is pushed to the next line.
     * </p>
     */
    private List<List<LineElement>> wrapElements(List<LineElement> elements,
                                                  float maxWidth) throws IOException {
        List<List<LineElement>> visualLines = new ArrayList<>();
        List<LineElement> currentVisualLine = new ArrayList<>();
        float currentLineWidth = 0;

        for (LineElement element : elements) {
            if (element instanceof InlineImageSegment) {
                // Inline images are atomic — cannot be split
                float imgWidth = element.getWidth();
                if (imgWidth <= maxWidth - currentLineWidth) {
                    // Fits on current line
                    currentVisualLine.add(element);
                    currentLineWidth += imgWidth;
                } else if (currentLineWidth > 0) {
                    // Doesn't fit — flush current line and place on new line
                    visualLines.add(currentVisualLine);
                    currentVisualLine = new ArrayList<>();
                    currentVisualLine.add(element);
                    currentLineWidth = imgWidth;
                } else {
                    // Fresh line but image is wider than maxWidth — force-place it
                    currentVisualLine.add(element);
                    visualLines.add(currentVisualLine);
                    currentVisualLine = new ArrayList<>();
                    currentLineWidth = 0;
                }
            } else if (element instanceof RichTextSegment) {
                RichTextSegment segment = (RichTextSegment) element;
                PDFont font = segment.resolveFont();
                float fontSize = segment.getFontSize();
                String remaining = segment.getText();

                while (!remaining.isEmpty()) {
                    float availableWidth = maxWidth - currentLineWidth;
                    float textWidth = WordWrapUtil.textWidth(remaining, font, fontSize);

                    if (textWidth <= availableWidth) {
                        // Entire remaining text fits on the current line
                        currentVisualLine.add(new RichTextSegment(
                                remaining, segment.getStyles(), fontSize, segment.getColor()));
                        currentLineWidth += textWidth;
                        remaining = "";
                    } else {
                        // Text does not fit — attempt to split at a word boundary
                        String[] split = splitAtWidth(remaining, font, fontSize, availableWidth);

                        if (!split[0].isEmpty()) {
                            currentVisualLine.add(new RichTextSegment(
                                    split[0], segment.getStyles(), fontSize, segment.getColor()));
                            visualLines.add(currentVisualLine);
                            currentVisualLine = new ArrayList<>();
                            currentLineWidth = 0;
                            remaining = split[1];
                        } else if (currentLineWidth > 0) {
                            visualLines.add(currentVisualLine);
                            currentVisualLine = new ArrayList<>();
                            currentLineWidth = 0;
                        } else {
                            String[] forced = forceBreak(remaining, font, fontSize, maxWidth);
                            currentVisualLine.add(new RichTextSegment(
                                    forced[0], segment.getStyles(), fontSize, segment.getColor()));
                            visualLines.add(currentVisualLine);
                            currentVisualLine = new ArrayList<>();
                            currentLineWidth = 0;
                            remaining = forced[1];
                        }
                    }
                }
            }
        }

        if (!currentVisualLine.isEmpty()) {
            visualLines.add(currentVisualLine);
        }
        if (visualLines.isEmpty()) {
            visualLines.add(Collections.emptyList());
        }
        return visualLines;
    }

    /**
     * Splits text at a word boundary so that the first part fits within {@code maxWidth}.
     * Returns an empty first element if not even the first word fits.
     *
     * @return {@code [fittingPart, remainder]}
     */
    private String[] splitAtWidth(String text, PDFont font, float fontSize,
                                  float maxWidth) throws IOException {
        String[] words = text.split("(?<=\\s)");
        StringBuilder fitting = new StringBuilder();

        for (String word : words) {
            String candidate = fitting + word;
            float width = WordWrapUtil.textWidth(candidate.trim(), font, fontSize);
            if (width > maxWidth) {
                if (fitting.length() > 0) {
                    // We have content that fits — return it
                    String remainder = text.substring(fitting.length());
                    return new String[]{fitting.toString().trim(), remainder.trim()};
                } else {
                    // Not even the first word fits — signal with empty first part
                    return new String[]{"", text};
                }
            }
            fitting.append(word);
        }
        // Everything fits
        return new String[]{text, ""};
    }

    /**
     * Force-breaks text character-by-character when a single word is wider than
     * the available width.  Guarantees at least one character is placed to
     * prevent infinite loops.
     *
     * @return {@code [fittingPart, remainder]}
     */
    private String[] forceBreak(String text, PDFont font, float fontSize,
                                float maxWidth) throws IOException {
        for (int i = 1; i < text.length(); i++) {
            String candidate = text.substring(0, i);
            float width = WordWrapUtil.textWidth(candidate, font, fontSize);
            if (width > maxWidth && i > 1) {
                return new String[]{text.substring(0, i - 1), text.substring(i - 1).trim()};
            }
        }
        // The entire text fits or is a single char — place it all
        return new String[]{text, ""};
    }
}

