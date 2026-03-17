package be.quodlibet.boxable.richtext;

import java.util.Collections;
import java.util.List;

/**
 * One logical line of rich text — an ordered list of {@link RichTextSegment}s
 * combined with optional list decoration and an alignment directive.
 * <p>
 * Immutable value object.
 * </p>
 */
public final class RichTextLine {

    private final List<RichTextSegment> segments;
    private final ListType listType;
    private final int listIndex;
    private final TextAlignment alignment;

    public RichTextLine(List<RichTextSegment> segments, ListType listType,
                        int listIndex, TextAlignment alignment) {
        this.segments = segments != null
                ? Collections.unmodifiableList(segments)
                : Collections.emptyList();
        this.listType = listType != null ? listType : ListType.NONE;
        this.listIndex = listIndex;
        this.alignment = alignment != null ? alignment : TextAlignment.LEFT;
    }

    /** Convenience constructor defaulting to {@code LEFT} alignment. */
    public RichTextLine(List<RichTextSegment> segments, ListType listType, int listIndex) {
        this(segments, listType, listIndex, TextAlignment.LEFT);
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public List<RichTextSegment> getSegments()   { return segments; }
    public ListType getListType()                { return listType; }
    public int getListIndex()                    { return listIndex; }
    public TextAlignment getAlignment()          { return alignment; }

    /**
     * Returns the maximum font size across all segments (used for line-height calculation).
     */
    public float getMaxFontSize() {
        float max = 6f; // minimum floor
        for (RichTextSegment seg : segments) {
            if (seg.getFontSize() > max) {
                max = seg.getFontSize();
            }
        }
        return max;
    }
}

