package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.LineElement;
import be.quodlibet.boxable.richtext.RenderContext;

import java.io.IOException;
import java.util.List;

/**
 * Right-aligned rendering — the element run is flush with the right edge
 * of the content area.
 */
public final class RightAlignmentStrategy implements AlignmentStrategy {

    static final RightAlignmentStrategy INSTANCE = new RightAlignmentStrategy();

    private RightAlignmentStrategy() {}

    @Override
    public void renderLine(RenderContext ctx, List<LineElement> elements,
                           float y, float contentStartX, float contentWidth) throws IOException {
        float totalWidth = SegmentRenderer.totalWidth(elements);
        float x = contentStartX + contentWidth - totalWidth;
        for (LineElement element : elements) {
            float w = SegmentRenderer.renderElement(ctx, element, x, y);
            x += w;
        }
    }
}
