package be.quodlibet.boxable.richtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One logical line of rich text — an ordered list of {@link LineElement}s
 * (text segments and/or inline images) combined with optional list decoration
 * and an alignment directive.
 * <p>
 * Immutable value object.
 * </p>
 */
public final class RichTextLine {

    private final List<LineElement> elements;
    private final ListType listType;
    private final int listIndex;
    private final TextAlignment alignment;

    /**
     * Primary constructor accepting mixed {@link LineElement}s (text and inline images).
     */
    public RichTextLine(List<? extends LineElement> elements, ListType listType,
                        int listIndex, TextAlignment alignment) {
        this.elements = elements != null
                ? Collections.unmodifiableList(new ArrayList<>(elements))
                : Collections.emptyList();
        this.listType = listType != null ? listType : ListType.NONE;
        this.listIndex = listIndex;
        this.alignment = alignment != null ? alignment : TextAlignment.LEFT;
    }

    /** Convenience constructor defaulting to {@code LEFT} alignment. */
    public RichTextLine(List<? extends LineElement> elements, ListType listType, int listIndex) {
        this(elements, listType, listIndex, TextAlignment.LEFT);
    }

    // ── Getters ──────────────────────────────────────────────────────────

    /**
     * Returns all elements (text segments and inline images) in order.
     */
    public List<LineElement> getElements() { return elements; }

    /**
     * Returns only the {@link RichTextSegment} elements, for backward
     * compatibility with code that expects text-only lines.
     *
     * @deprecated use {@link #getElements()} instead
     */
    @Deprecated
    public List<RichTextSegment> getSegments() {
        List<RichTextSegment> result = new ArrayList<>();
        for (LineElement el : elements) {
            if (el instanceof RichTextSegment) {
                result.add((RichTextSegment) el);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public ListType getListType()                { return listType; }
    public int getListIndex()                    { return listIndex; }
    public TextAlignment getAlignment()          { return alignment; }

    /**
     * Returns the maximum "font size" across all elements.
     * For text segments this is the font size; for inline images it is
     * the display height.  Used for line-height calculation.
     */
    public float getMaxFontSize() {
        float max = 6f; // minimum floor
        for (LineElement el : elements) {
            float h = el.getHeight();
            if (h > max) {
                max = h;
            }
        }
        return max;
    }
}
