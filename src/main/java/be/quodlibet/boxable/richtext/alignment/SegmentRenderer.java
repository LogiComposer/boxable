package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.RenderContext;
import be.quodlibet.boxable.richtext.RichTextSegment;
import be.quodlibet.boxable.richtext.WordWrapUtil;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * Shared rendering helpers used by all alignment strategies.
 * Extracted to avoid code duplication (DRY).
 */
final class SegmentRenderer {

    static final float UNDERLINE_OFFSET = -2f;
    static final float UNDERLINE_THICKNESS = 0.5f;

    private SegmentRenderer() {}

    /**
     * Renders a single segment at the given position, including underline if styled.
     *
     * @return the rendered width in points
     */
    static float renderSegment(RenderContext ctx, RichTextSegment segment,
                               float x, float y) throws IOException {
        PageContentStreamOptimized stream = ctx.getStream();
        PDFont font = segment.resolveFont();
        float fontSize = segment.getFontSize();
        String text = segment.getText();

        stream.setNonStrokingColor(segment.getColor());
        stream.setFont(font, fontSize);
        stream.newLineAt(x, y);
        stream.showText(text);

        float width = WordWrapUtil.textWidth(text, font, fontSize);

        if (segment.isUnderline()) {
            drawUnderline(ctx, x, y, width);
        }

        return width;
    }

    /**
     * Computes the total rendered width of all segments.
     */
    static float totalWidth(List<RichTextSegment> segments) throws IOException {
        float total = 0;
        for (RichTextSegment seg : segments) {
            total += seg.getWidth();
        }
        return total;
    }

    static void drawUnderline(RenderContext ctx, float x, float y,
                              float width) throws IOException {
        PageContentStreamOptimized stream = ctx.getStream();
        stream.endText();
        stream.setStrokingColor(Color.BLACK);
        stream.setLineWidth(UNDERLINE_THICKNESS);
        stream.moveTo(x, y + UNDERLINE_OFFSET);
        stream.lineTo(x + width, y + UNDERLINE_OFFSET);
        stream.stroke();
    }
}

