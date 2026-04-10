package be.quodlibet.boxable.richtext;

import java.io.IOException;

/**
 * A renderable unit that can appear on a visual line within a {@link RichTextLine}.
 * <p>
 * Implementations include {@link RichTextSegment} (styled text) and
 * {@link InlineImageSegment} (an image that flows inline with text).
 * </p>
 * <p>
 * The two operations required by the layout engine are width (for word-wrapping
 * and alignment) and height (for line-height calculation).
 * </p>
 */
public interface LineElement {

    /**
     * Returns the rendered width of this element in points.
     *
     * @return width in points
     * @throws IOException if font metrics cannot be read (text segments)
     */
    float getWidth() throws IOException;

    /**
     * Returns the rendered height of this element in points.
     * For text segments this is typically the font size; for images it is
     * the display height.
     *
     * @return height in points
     */
    float getHeight();

    /**
     * Returns {@code true} if this element is a text segment.
     */
    default boolean isText() {
        return this instanceof RichTextSegment;
    }

    /**
     * Returns {@code true} if this element is an inline image.
     */
    default boolean isImage() {
        return this instanceof InlineImageSegment;
    }
}

