package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.RenderContext;
import be.quodlibet.boxable.richtext.RichTextSegment;

import java.io.IOException;
import java.util.List;

/**
 * Right-aligned rendering — the segment run is flush with the right edge
 * of the content area.
 */
public final class RightAlignmentStrategy implements AlignmentStrategy {

    static final RightAlignmentStrategy INSTANCE = new RightAlignmentStrategy();

    private RightAlignmentStrategy() {}

    @Override
    public void renderLine(RenderContext ctx, List<RichTextSegment> segments,
                           float y, float contentStartX, float contentWidth) throws IOException {
        float totalWidth = SegmentRenderer.totalWidth(segments);
        float x = contentStartX + contentWidth - totalWidth;
        for (RichTextSegment segment : segments) {
            float w = SegmentRenderer.renderSegment(ctx, segment, x, y);
            x += w;
        }
    }
}

