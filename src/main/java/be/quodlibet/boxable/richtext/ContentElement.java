package be.quodlibet.boxable.richtext;

import java.io.IOException;

/**
 * A renderable element that can compute its required height and draw itself
 * within the bounds tracked by a {@link RenderContext}.
 * <p>
 * Follows the <strong>Composite</strong> pattern — implementations include
 * atomic elements ({@link TextContentElement}, {@link ImageContentElement})
 * and composite ones ({@link ListContentElement}).
 * </p>
 * <p>
 * <strong>Interface Segregation:</strong> Only two operations are required
 * of every content element: height estimation and rendering.
 * </p>
 */
public interface ContentElement {

    /**
     * Estimates the height this element will consume when rendered within
     * the given available width.  Used for overflow checks.
     *
     * @param availableWidth the maximum width in points
     * @return estimated height in points
     * @throws IOException if font metrics cannot be read
     */
    float estimateHeight(float availableWidth) throws IOException;

    /**
     * Renders this element into the PDF stream tracked by {@code ctx},
     * advancing the cursor accordingly.
     *
     * @param ctx the current render context
     * @throws IOException if writing to the stream fails
     */
    void render(RenderContext ctx) throws IOException;
}

