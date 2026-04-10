package be.quodlibet.boxable.richtext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A <strong>Composite</strong> {@link ContentElement} that renders a list
 * (bulleted or numbered) of {@link RichTextLine}s.
 * <p>
 * Each item is rendered with a prefix (bullet or number) and an indented body.
 * Nested lists can be achieved by nesting {@code ListContentElement}s inside
 * a parent block.
 * </p>
 */
public final class ListContentElement implements ContentElement {


    private final ListType listType;
    private final List<RichTextLine> items;

    /**
     * @param listType the kind of list (BULLETED or NUMBERED)
     * @param items    the lines to display as list items
     */
    public ListContentElement(ListType listType, List<RichTextLine> items) {
        this.listType = listType != null ? listType : ListType.BULLETED;
        this.items = items != null
                ? Collections.unmodifiableList(new ArrayList<>(items))
                : Collections.emptyList();
    }

    // ── ContentElement contract ──────────────────────────────────────────

    @Override
    public float estimateHeight(float availableWidth) throws IOException {
        float total = 0;
        for (int i = 0; i < items.size(); i++) {
            RichTextLine item = items.get(i);
            // Each item is rendered as a TextContentElement (one line)
            RichTextLine decorated = decorateLine(item, i + 1);
            TextContentElement textEl = new TextContentElement(decorated);
            total += textEl.estimateHeight(availableWidth);
        }
        return total;
    }

    @Override
    public void render(RenderContext ctx) throws IOException {
        for (int i = 0; i < items.size(); i++) {
            if (ctx.isOverflow()) return;
            RichTextLine decorated = decorateLine(items.get(i), i + 1);
            TextContentElement textEl = new TextContentElement(decorated);
            textEl.render(ctx);
        }
    }

    // ── Internals ────────────────────────────────────────────────────────

    /**
     * Re-wraps the line with the correct {@link ListType} and index so that
     * {@link TextContentElement} knows to render the prefix and indent.
     */
    private RichTextLine decorateLine(RichTextLine original, int index) {
        return new RichTextLine(
                original.getSegments(),
                listType,
                index,
                original.getAlignment()
        );
    }
}

