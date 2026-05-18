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

    // -- ContentElement contract --------------------------------------------

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

        float contentWidth = availableWidth;
        if (line.getListType() == ListType.BULLETED || line.getListType() == ListType.NUMBERED) {
            contentWidth -= LIST_INDENT;
        }

        List<List<LineElement>> wrappedLines = wrapElements(line.getElements(), contentWidth);
        return lineHeight * wrappedLines.size();
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
                ctx.getStream().beginText();
                ctx.getStream().setFont(prefixFont, fontSize);
                ctx.getStream().setNonStrokingColor(java.awt.Color.BLACK);
                ctx.getStream().newLineAt(prefixX, ctx.getCursorY());
                ctx.getStream().showText(prefix);
                ctx.getStream().endText();
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
                    // floor/ceil: round available space down and text width up to avoid sub-pixel over-placement.
                    float availableWidth = (float) Math.floor(maxWidth - currentLineWidth);
                    float textWidth = (float) Math.ceil(WordWrapUtil.textWidth(remaining, font, fontSize));

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
        // floor: guard against sub-pixel float noise in font metrics.
        float effectiveMax = (float) Math.floor(maxWidth);

        // Tokenise into alternating word-runs and whitespace-runs so each token
        // is measured independently and spaces are treated as break points.
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int start = i;
            if (Character.isWhitespace(text.charAt(i))) {
                while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
            } else {
                while (i < text.length() && !Character.isWhitespace(text.charAt(i))) i++;
            }
            tokens.add(text.substring(start, i));
        }

        StringBuilder fitting = new StringBuilder();
        for (String token : tokens) {
            String candidate = fitting.toString() + token;
            float candidateWidth = (float) Math.ceil(WordWrapUtil.textWidth(candidate, font, fontSize));
            if (candidateWidth > effectiveMax) {
                if (fitting.length() > 0) {
                    // Strip trailing spaces from the fitting part only; remainder is taken
                    // verbatim so its leading space is preserved
                    String fittingText = stripTrailing(fitting.toString());
                    String remainder = text.substring(fittingText.length());
                    return new String[]{fittingText, remainder};
                } else {
                    // Not even the first token fits — signal with empty first part
                    return new String[]{"", text};
                }
            }
            fitting.append(token);
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
        // floor/ceil: same sub-pixel guard as in splitAtWidth.
        float effectiveMax = (float) Math.floor(maxWidth);
        // i <= text.length() (not <) so the last character is tested and overflow
        // at the exact boundary is caught rather than falling through to "place it all".
        for (int i = 1; i <= text.length(); i++) {
            String candidate = text.substring(0, i);
            float width = (float) Math.ceil(WordWrapUtil.textWidth(candidate, font, fontSize));
            if (width > effectiveMax && i > 1) {
                // remainder taken verbatim (no trim) to preserve boundary characters.
                return new String[]{text.substring(0, i - 1), text.substring(i - 1)};
            }
        }
        // The entire text fits or is a single char — place it all
        return new String[]{text, ""};
    }

    /** Java-8 equivalent of {@code String.stripTrailing()} (added in Java 11). */
    private static String stripTrailing(String s) {
        int right = s.length() - 1;
        while (right >= 0 && Character.isWhitespace(s.charAt(right))) {
            right--;
        }
        return right == s.length() - 1 ? s : s.substring(0, right + 1);
    }
}

