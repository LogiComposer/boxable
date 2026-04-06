package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.InlineImageSegment;
import be.quodlibet.boxable.richtext.LineElement;
import be.quodlibet.boxable.richtext.RenderContext;
import be.quodlibet.boxable.richtext.RichTextSegment;
import be.quodlibet.boxable.richtext.WordWrapUtil;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Justified rendering — words and inline images are spread evenly across the
 * full content width by distributing extra space between units.
 * <p>
 * Inline images are treated as atomic, unsplittable "word" units.
 * For single-unit lines the behaviour falls back to left alignment.
 * </p>
 */
public final class JustifyAlignmentStrategy implements AlignmentStrategy {

    static final JustifyAlignmentStrategy INSTANCE = new JustifyAlignmentStrategy();

    private JustifyAlignmentStrategy() {}

    /**
     * A renderable unit for justify layout — either a word (text) or an inline image.
     */
    private static final class JustifyUnit {
        final RichTextSegment textSegment; // non-null for text words
        final InlineImageSegment imageSegment; // non-null for images
        final String word; // the word text (null for images)
        final float width;

        JustifyUnit(RichTextSegment seg, String word, float width) {
            this.textSegment = seg;
            this.imageSegment = null;
            this.word = word;
            this.width = width;
        }

        JustifyUnit(InlineImageSegment img) {
            this.textSegment = null;
            this.imageSegment = img;
            this.word = null;
            this.width = img.getWidth();
        }
    }

    @Override
    public void renderLine(RenderContext ctx, List<LineElement> elements,
                           float y, float contentStartX, float contentWidth) throws IOException {

        // Build a flat list of justify units (words + images)
        List<JustifyUnit> units = buildUnits(elements);

        if (units.size() <= 1) {
            LeftAlignmentStrategy.INSTANCE.renderLine(ctx, elements, y, contentStartX, contentWidth);
            return;
        }

        // Compute total content width and gaps
        float totalUnitWidth = 0;
        for (JustifyUnit u : units) {
            totalUnitWidth += u.width;
        }
        float totalGap = contentWidth - totalUnitWidth;
        // If there is no positive gap to distribute, fall back to left alignment
        if (totalGap <= 0) {
            LeftAlignmentStrategy.INSTANCE.renderLine(ctx, elements, y, contentStartX, contentWidth);
            return;
        }
        float gapPerSpace = totalGap / (units.size() - 1);

        // Render each unit with computed gaps
        float currentX = contentStartX;
        PageContentStreamOptimized stream = ctx.getStream();

        for (int i = 0; i < units.size(); i++) {
            JustifyUnit unit = units.get(i);

            if (unit.imageSegment != null) {
                SegmentRenderer.renderInlineImage(ctx, unit.imageSegment, currentX, y);
            } else {
                RichTextSegment seg = unit.textSegment;
                PDFont font = seg.resolveFont();
                float fontSize = seg.getFontSize();

                stream.setNonStrokingColor(seg.getColor());
                stream.setFont(font, fontSize);
                stream.newLineAt(currentX, y);
                stream.showText(unit.word);

                if (seg.isUnderline()) {
                    SegmentRenderer.drawUnderline(ctx, currentX, y, unit.width);
                }
            }

            currentX += unit.width;

            // Add gap after unit (unless it's the last one)
            if (i < units.size() - 1) {
                currentX += gapPerSpace;
            }
        }
    }

    /**
     * Breaks all line elements into atomic justify units.
     * Text segments are split by whitespace into individual words;
     * inline images become single units.
     */
    private List<JustifyUnit> buildUnits(List<LineElement> elements) throws IOException {
        List<JustifyUnit> units = new ArrayList<>();
        for (LineElement el : elements) {
            if (el instanceof InlineImageSegment) {
                units.add(new JustifyUnit((InlineImageSegment) el));
            } else if (el instanceof RichTextSegment) {
                RichTextSegment seg = (RichTextSegment) el;
                PDFont font = seg.resolveFont();
                float fontSize = seg.getFontSize();
                String[] words = seg.getText().trim().split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        float w = WordWrapUtil.textWidth(word, font, fontSize);
                        units.add(new JustifyUnit(seg, word, w));
                    }
                }
            }
        }
        return units;
    }
}
