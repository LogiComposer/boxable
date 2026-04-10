package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.InlineImageSegment;
import be.quodlibet.boxable.richtext.LineElement;
import be.quodlibet.boxable.richtext.RenderContext;
import be.quodlibet.boxable.richtext.RichTextSegment;
import be.quodlibet.boxable.richtext.WordWrapUtil;
import be.quodlibet.boxable.richtext.ImageCache;
import be.quodlibet.boxable.utils.PageContentStreamOptimized;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * Shared rendering helpers used by all alignment strategies.
 * Extracted to avoid code duplication (DRY).
 */
final class SegmentRenderer {

    static final float UNDERLINE_OFFSET = -2f;
    static final float UNDERLINE_THICKNESS = 0.5f;

    private SegmentRenderer() {}

    /**
     * Renders any {@link LineElement} (text or inline image) and returns its width.
     */
    static float renderElement(RenderContext ctx, LineElement element,
                               float x, float y) throws IOException {
        if (element instanceof RichTextSegment) {
            return renderSegment(ctx, (RichTextSegment) element, x, y);
        } else if (element instanceof InlineImageSegment) {
            return renderInlineImage(ctx, (InlineImageSegment) element, x, y);
        }
        return 0;
    }

    /**
     * Renders a single text segment at the given position, including underline if styled.
     *
     * @return the rendered width in points
     */
    static float renderSegment(RenderContext ctx, RichTextSegment segment,
                               float x, float y) throws IOException {
        PageContentStreamOptimized stream = ctx.getStream();
        PDFont font = segment.resolveFont();
        float fontSize = segment.getFontSize();
        String text = segment.getText();

        stream.beginText();
        stream.setNonStrokingColor(segment.getColor());
        stream.setFont(font, fontSize);
        stream.newLineAt(x, y);
        stream.showText(text);
        stream.endText();

        float width = WordWrapUtil.textWidth(text, font, fontSize);

        if (segment.isUnderline()) {
            drawUnderline(ctx, x, y, width);
        }

        return width;
    }

    /**
     * Renders an inline image at the given position.
     * The image bottom is aligned with the text baseline.
     *
     * @return the rendered width in points
     */
    static float renderInlineImage(RenderContext ctx, InlineImageSegment seg,
                                   float x, float y) throws IOException {
        PageContentStreamOptimized stream = ctx.getStream();

        PDImageXObject xObject = ImageCache.getOrCreate(
                ctx.getDocument(), seg.getCacheKey(), seg.getImage(), seg.getQuality());

        // End any open text mode before drawing the image
        stream.endText();

        // PDF draws images from bottom-left; y is the text baseline,
        // so draw the image starting at the baseline (bottom-aligned with text)
        stream.drawImage(xObject, x, y, seg.getWidth(), seg.getHeight());

        return seg.getWidth();
    }

    /**
     * Computes the total rendered width of all elements.
     */
    static float totalWidth(List<? extends LineElement> elements) throws IOException {
        float total = 0;
        for (LineElement el : elements) {
            total += el.getWidth();
        }
        return total;
    }

    static void drawUnderline(RenderContext ctx, float x, float y,
                              float width) throws IOException {
        PageContentStreamOptimized stream = ctx.getStream();
        stream.endText();
        stream.setStrokingColor(Color.BLACK);
        stream.setLineWidth(UNDERLINE_THICKNESS);
        stream.moveTo(x, y + UNDERLINE_OFFSET);
        stream.lineTo(x + width, y + UNDERLINE_OFFSET);
        stream.stroke();
    }
}
