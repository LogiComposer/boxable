package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.LineElement;
import be.quodlibet.boxable.richtext.RenderContext;

import java.io.IOException;
import java.util.List;

/**
 * Left-aligned rendering — elements are placed sequentially starting at
 * {@code contentStartX}.
 */
public final class LeftAlignmentStrategy implements AlignmentStrategy {

    static final LeftAlignmentStrategy INSTANCE = new LeftAlignmentStrategy();

    private LeftAlignmentStrategy() {}

    @Override
    public void renderLine(RenderContext ctx, List<LineElement> elements,
                           float y, float contentStartX, float contentWidth) throws IOException {
        float x = contentStartX;
        for (LineElement element : elements) {
            float w = SegmentRenderer.renderElement(ctx, element, x, y);
            x += w;
        }
    }
}
