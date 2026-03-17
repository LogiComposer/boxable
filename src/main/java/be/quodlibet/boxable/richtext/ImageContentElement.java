package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.utils.ImageUtils;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * A {@link ContentElement} that renders an image within the block bounds.
 * <p>
 * The image is scaled to fit the available width while maintaining aspect ratio.
 * If the remaining vertical space is insufficient the image is further scaled
 * down, and if no space remains the element marks the context as overflowed.
 * </p>
 * <p>
 * <strong>Performance:</strong> The expensive {@link PDImageXObject} encoding
 * is performed lazily on first render and cached via {@link ImageCache} so
 * that the same source image is never encoded twice for the same document.
 * </p>
 */
public final class ImageContentElement implements ContentElement {

    private final BufferedImage sourceImage;
    private final String cacheKey;
    private final float requestedWidth;
    private final float requestedHeight;
    private final TextAlignment alignment;
    private final float spacingBefore;
    private final float spacingAfter;
    private final float quality;

    private ImageContentElement(Builder builder) {
        this.sourceImage = builder.sourceImage;
        this.cacheKey = builder.cacheKey;
        this.requestedWidth = builder.requestedWidth;
        this.requestedHeight = builder.requestedHeight;
        this.alignment = builder.alignment;
        this.spacingBefore = builder.spacingBefore;
        this.spacingAfter = builder.spacingAfter;
        this.quality = builder.quality;
    }

    // ── ContentElement contract ──────────────────────────────────────────

    @Override
    public float estimateHeight(float availableWidth) throws IOException {
        float[] dim = fitDimensions(availableWidth, Float.MAX_VALUE);
        return spacingBefore + dim[1] + spacingAfter;
    }

    @Override
    public void render(RenderContext ctx) throws IOException {
        float availableWidth = ctx.getInnerWidth();
        float availableHeight = ctx.remainingHeight() - spacingBefore - spacingAfter;

        if (availableHeight <= 0) {
            ctx.markOverflow();
            return;
        }

        float[] dim = fitDimensions(availableWidth, availableHeight);
        float imgWidth = dim[0];
        float imgHeight = dim[1];

        float totalRequired = spacingBefore + imgHeight + spacingAfter;
        if (!ctx.hasSpace(totalRequired)) {
            // Try to fit a smaller version
            float maxH = ctx.remainingHeight() - spacingBefore - spacingAfter;
            if (maxH <= 0) {
                ctx.markOverflow();
                return;
            }
            dim = fitDimensions(availableWidth, maxH);
            imgWidth = dim[0];
            imgHeight = dim[1];
        }

        ctx.advanceCursor(spacingBefore);

        // Resolve X using alignment
        float imgX = computeAlignedX(imgWidth, ctx.getLeft(), availableWidth, alignment);

        // Lazy-load the PDImageXObject via cache
        PDImageXObject xObject = ImageCache.getOrCreate(
                ctx.getDocument(), cacheKey, sourceImage, quality);

        // PDF draws images from bottom-left corner
        float imgY = ctx.getCursorY() - imgHeight;
        ctx.getStream().drawImage(xObject, imgX, imgY, imgWidth, imgHeight);

        ctx.setCursorY(imgY);
        ctx.advanceCursor(spacingAfter);
    }

    // ── Internals ────────────────────────────────────────────────────────

    /**
     * Computes the dimensions that fit within {@code maxWidth × maxHeight}
     * while maintaining the original aspect ratio.
     */
    private float[] fitDimensions(float maxWidth, float maxHeight) {
        return ImageUtils.getScaledDimension(requestedWidth, requestedHeight,
                maxWidth, maxHeight);
    }

    private float computeAlignedX(float contentWidth, float regionStart,
                                  float regionWidth, TextAlignment align) {
        switch (align != null ? align : TextAlignment.LEFT) {
            case CENTER:
                return regionStart + (regionWidth - contentWidth) / 2f;
            case RIGHT:
                return regionStart + regionWidth - contentWidth;
            default:
                return regionStart;
        }
    }

    // ── Builder ──────────────────────────────────────────────────────────

    /**
     * Fluent builder for {@link ImageContentElement}.
     * <p>
     * Only the source image is required; all other properties have sensible defaults.
     * </p>
     */
    public static final class Builder {
        private final BufferedImage sourceImage;
        private String cacheKey;
        private float requestedWidth;
        private float requestedHeight;
        private TextAlignment alignment = TextAlignment.LEFT;
        private float spacingBefore = 5f;
        private float spacingAfter = 5f;
        private float quality = 1.0f;

        /**
         * @param sourceImage the source image (required)
         */
        public Builder(BufferedImage sourceImage) {
            this.sourceImage = Objects.requireNonNull(sourceImage, "sourceImage");
            this.requestedWidth = sourceImage.getWidth();
            this.requestedHeight = sourceImage.getHeight();
            this.cacheKey = "img-" + System.identityHashCode(sourceImage);
        }

        /** Sets a custom cache key (e.g., file path). */
        public Builder cacheKey(String key) {
            this.cacheKey = Objects.requireNonNull(key);
            return this;
        }

        /** Sets desired dimensions in points. */
        public Builder size(float width, float height) {
            this.requestedWidth = width;
            this.requestedHeight = height;
            return this;
        }

        /** Sets horizontal alignment within the block. */
        public Builder alignment(TextAlignment alignment) {
            this.alignment = alignment;
            return this;
        }

        /** Sets vertical spacing before the image (in points). */
        public Builder spacingBefore(float pts) {
            this.spacingBefore = pts;
            return this;
        }

        /** Sets vertical spacing after the image (in points). */
        public Builder spacingAfter(float pts) {
            this.spacingAfter = pts;
            return this;
        }

        /**
         * Sets JPEG encoding quality (0 &lt; q &le; 1).  Use 1.0 for lossless PNG.
         */
        public Builder quality(float q) {
            if (q <= 0 || q > 1) {
                throw new IllegalArgumentException("quality must be in (0, 1]");
            }
            this.quality = q;
            return this;
        }

        public ImageContentElement build() {
            return new ImageContentElement(this);
        }
    }
}

