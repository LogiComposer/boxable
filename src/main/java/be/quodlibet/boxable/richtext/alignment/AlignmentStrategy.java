package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.RenderContext;
import be.quodlibet.boxable.richtext.RichTextSegment;

import java.io.IOException;
import java.util.List;

/**
 * Strategy for rendering a list of {@link RichTextSegment}s on a single
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
     * Renders the segments at the given Y position within the content area
     * defined by the render context.
     *
     * @param ctx          the render context (provides stream, left/right bounds)
     * @param segments     the text segments to render
     * @param y            the baseline Y coordinate
     * @param contentStartX the left X where content may start (accounts for list indent)
     * @param contentWidth  the available width for content
     * @throws IOException if writing to the stream fails
     */
    void renderLine(RenderContext ctx, List<RichTextSegment> segments,
                    float y, float contentStartX, float contentWidth) throws IOException;
}

