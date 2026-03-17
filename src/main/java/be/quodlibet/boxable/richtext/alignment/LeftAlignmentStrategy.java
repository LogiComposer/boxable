package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.RenderContext;
import be.quodlibet.boxable.richtext.RichTextSegment;

import java.io.IOException;
import java.util.List;

/**
 * Left-aligned rendering — segments are placed sequentially starting at
 * {@code contentStartX}.
 */
public final class LeftAlignmentStrategy implements AlignmentStrategy {

    static final LeftAlignmentStrategy INSTANCE = new LeftAlignmentStrategy();

    private LeftAlignmentStrategy() {}

    @Override
    public void renderLine(RenderContext ctx, List<RichTextSegment> segments,
                           float y, float contentStartX, float contentWidth) throws IOException {
        float x = contentStartX;
        for (RichTextSegment segment : segments) {
            float w = SegmentRenderer.renderSegment(ctx, segment, x, y);
            x += w;
        }
    }
}

