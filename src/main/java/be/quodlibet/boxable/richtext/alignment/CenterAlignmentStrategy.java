package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.LineElement;
import be.quodlibet.boxable.richtext.RenderContext;

import java.io.IOException;
import java.util.List;

/**
 * Center-aligned rendering — the entire element run is horizontally centered
 * within the available content width.
 */
public final class CenterAlignmentStrategy implements AlignmentStrategy {

    static final CenterAlignmentStrategy INSTANCE = new CenterAlignmentStrategy();

    private CenterAlignmentStrategy() {}

    @Override
    public void renderLine(RenderContext ctx, List<LineElement> elements,
                           float y, float contentStartX, float contentWidth) throws IOException {
        float totalWidth = SegmentRenderer.totalWidth(elements);
        float x = contentStartX + (contentWidth - totalWidth) / 2f;
        for (LineElement element : elements) {
            float w = SegmentRenderer.renderElement(ctx, element, x, y);
            x += w;
        }
    }
}
