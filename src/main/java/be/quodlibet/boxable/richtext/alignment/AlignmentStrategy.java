package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.LineElement;
import be.quodlibet.boxable.richtext.RenderContext;

import java.io.IOException;
import java.util.List;

/**
 * Strategy for rendering a list of {@link LineElement}s on a single
 * visual line with a specific horizontal alignment.
 * <p>
 * Implementations are <strong>stateless singletons</strong> obtained via
 * {@link AlignmentStrategyFactory}.
 * </p>
 * <p>
 * <strong>Open/Closed Principle:</strong> new alignment modes can be added
 * by implementing this interface and registering in the factory — no existing
 * code needs to change.
 * </p>
 */
public interface AlignmentStrategy {

    /**
     * Renders the elements at the given Y position within the content area
     * defined by the render context.
     *
     * @param ctx          the render context (provides stream, left/right bounds)
     * @param elements     the line elements to render (text segments and/or inline images)
     * @param y            the baseline Y coordinate
     * @param contentStartX the left X where content may start (accounts for list indent)
     * @param contentWidth  the available width for content
     * @throws IOException if writing to the stream fails
     */
    void renderLine(RenderContext ctx, List<LineElement> elements,
                    float y, float contentStartX, float contentWidth) throws IOException;
}
