package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.RenderContext;
import be.quodlibet.boxable.richtext.RichTextSegment;

import java.io.IOException;
import java.util.List;

/**
 * Center-aligned rendering — the entire segment run is horizontally centered
 * within the available content width.
 */
public final class CenterAlignmentStrategy implements AlignmentStrategy {

    static final CenterAlignmentStrategy INSTANCE = new CenterAlignmentStrategy();

    private CenterAlignmentStrategy() {}

    @Override
    public void renderLine(RenderContext ctx, List<RichTextSegment> segments,
                           float y, float contentStartX, float contentWidth) throws IOException {
        float totalWidth = SegmentRenderer.totalWidth(segments);
        float x = contentStartX + (contentWidth - totalWidth) / 2f;
        for (RichTextSegment segment : segments) {
            float w = SegmentRenderer.renderSegment(ctx, segment, x, y);
            x += w;
        }
    }
}

