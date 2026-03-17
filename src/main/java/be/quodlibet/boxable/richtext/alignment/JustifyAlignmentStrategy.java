package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.RenderContext;
import be.quodlibet.boxable.richtext.RichTextSegment;
import be.quodlibet.boxable.richtext.WordWrapUtil;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.List;

/**
 * Justified rendering — words are spread evenly across the full content width
 * by distributing extra space between words.
 * <p>
 * For single-word lines the behaviour falls back to left alignment.
 * </p>
 */
public final class JustifyAlignmentStrategy implements AlignmentStrategy {

    static final JustifyAlignmentStrategy INSTANCE = new JustifyAlignmentStrategy();

    private JustifyAlignmentStrategy() {}

    @Override
    public void renderLine(RenderContext ctx, List<RichTextSegment> segments,
                           float y, float contentStartX, float contentWidth) throws IOException {

        // Concatenate all segment text for word-level distribution
        StringBuilder fullText = new StringBuilder();
        for (RichTextSegment seg : segments) {
            fullText.append(seg.getText());
        }

        String[] words = fullText.toString().trim().split("\\s+");
        if (words.length <= 1) {
            // Single word — fall back to left alignment
            LeftAlignmentStrategy.INSTANCE.renderLine(ctx, segments, y, contentStartX, contentWidth);
            return;
        }

        // Walk segments and render word-by-word with computed gaps
        float totalWordWidth = computeTotalWordWidth(segments);
        float totalGap = contentWidth - totalWordWidth;
        float gapPerSpace = totalGap / (words.length - 1);

        float currentX = contentStartX;
        int wordIndex = 0;

        for (RichTextSegment segment : segments) {
            PDFont font = segment.resolveFont();
            float fontSize = segment.getFontSize();
            PageContentStreamOptimized stream = ctx.getStream();
            String segText = segment.getText();
            String[] segWords = segText.trim().split("\\s+");

            stream.setNonStrokingColor(segment.getColor());
            stream.setFont(font, fontSize);

            for (String word : segWords) {
                if (word.isEmpty()) continue;

                stream.newLineAt(currentX, y);
                stream.showText(word);

                float wordWidth = WordWrapUtil.textWidth(word, font, fontSize);

                if (segment.isUnderline()) {
                    SegmentRenderer.drawUnderline(ctx, currentX, y, wordWidth);
                }

                currentX += wordWidth;
                wordIndex++;

                // Add gap after word (unless it's the last word overall)
                if (wordIndex < words.length) {
                    currentX += gapPerSpace;
                }
            }
        }
    }

    private float computeTotalWordWidth(List<RichTextSegment> segments) throws IOException {
        float total = 0;
        for (RichTextSegment seg : segments) {
            PDFont font = seg.resolveFont();
            float fontSize = seg.getFontSize();
            String[] segWords = seg.getText().trim().split("\\s+");
            for (String word : segWords) {
                if (!word.isEmpty()) {
                    total += WordWrapUtil.textWidth(word, font, fontSize);
                }
            }
        }
        return total;
    }
}

