package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.apache.pdfbox.pdmodel.PDDocument;


/**
 * Shared, mutable rendering state passed through the content element tree during
 * a single {@link RichTextBlock} render pass.
 * <p>
 * Encapsulates the current cursor position, available space, and references to
 * the PDF document and content stream so that individual {@link ContentElement}
 * implementations remain stateless.
 * </p>
 */
public final class RenderContext {

    private final PDDocument document;
    private final PageContentStreamOptimized stream;

    /** Left edge of the renderable area (block X + padding, in PDF coords). */
    private final float left;
    /** Right edge of the renderable area. */
    private final float right;
    /** Bottom boundary (block bottom + padding, in PDF coords). */
    private final float bottom;

    /** Inner width of the renderable area ({@code right - left}). */
    private final float innerWidth;

    /** Current Y cursor — starts at the top and decreases. */
    private float cursorY;

    /** Whether rendering has been truncated because content exceeded the block. */
    private boolean overflow;

    public RenderContext(PDDocument document, PageContentStreamOptimized stream,
                         float left, float right, float top, float bottom) {
        this.document = document;
        this.stream = stream;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
        this.innerWidth = right - left;
        this.cursorY = top;
        this.overflow = false;
    }

    // ── Queries ──────────────────────────────────────────────────────────

    /** Returns {@code true} if the required height still fits above the bottom boundary. */
    public boolean hasSpace(float requiredHeight) {
        return (cursorY - requiredHeight) >= bottom;
    }

    /** Returns the remaining vertical space in points. */
    public float remainingHeight() {
        return Math.max(0, cursorY - bottom);
    }

    public boolean isOverflow() {
        return overflow;
    }

    // ── Accessors ────────────────────────────────────────────────────────

    public PDDocument getDocument()                   { return document; }
    public PageContentStreamOptimized getStream()     { return stream; }
    public float getLeft()                            { return left; }
    public float getRight()                           { return right; }
    public float getBottom()                          { return bottom; }
    public float getInnerWidth()                      { return innerWidth; }
    public float getCursorY()                         { return cursorY; }

    // ── Mutators ─────────────────────────────────────────────────────────

    /** Advances the cursor downward by {@code amount} points. */
    public void advanceCursor(float amount) {
        this.cursorY -= amount;
    }

    /** Sets the cursor to an absolute Y position. */
    public void setCursorY(float y) {
        this.cursorY = y;
    }

    /** Marks that content has overflowed the block bounds. */
    public void markOverflow() {
        this.overflow = true;
    }
}

