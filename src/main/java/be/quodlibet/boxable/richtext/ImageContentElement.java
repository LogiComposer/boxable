package be.quodlibet.boxable.richtext;

import be.quodlibet.boxable.utils.ImageUtils;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
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
    private final boolean explicitSize;
    private final TextAlignment alignment;
    private final float spacingBefore;
    private final float spacingAfter;
    private final float quality;

    private ImageContentElement(Builder builder) {
        this.sourceImage = builder.sourceImage;
        this.cacheKey = builder.cacheKey;
        this.requestedWidth = builder.requestedWidth;
        this.requestedHeight = builder.requestedHeight;
        this.explicitSize = builder.explicitSize;
        this.alignment = builder.alignment;
        this.spacingBefore = builder.spacingBefore;
        this.spacingAfter = builder.spacingAfter;
        this.quality = builder.quality;
    }

    // ── ContentElement contract ──────────────────────────────────────────

    @Override
    public float estimateHeight(float availableWidth) throws IOException {
        float[] dim = resolveAndFit(availableWidth, Float.MAX_VALUE);
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

        float[] dim = resolveAndFit(availableWidth, availableHeight);
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
            dim = resolveAndFit(availableWidth, maxH);
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
     * Resolves the effective image dimensions.
     * <p>
     * When an explicit {@code .size(w, h)} was provided, those dimensions are
     * scaled to fit within {@code maxWidth x maxHeight} while maintaining the
     * aspect ratio.
     * </p>
     * <p>
     * When no explicit size was set, the image is scaled <strong>proportionally
     * to the block's available width</strong>: the width is set to
     * {@code maxWidth} and the height is derived from the source image's
     * aspect ratio, then clamped to {@code maxHeight}.
     * </p>
     */
    private float[] resolveAndFit(float maxWidth, float maxHeight) {
        float effectiveWidth;
        float effectiveHeight;

        if (explicitSize) {
            // Use the caller-specified dimensions, scale to fit
            effectiveWidth = requestedWidth;
            effectiveHeight = requestedHeight;
        } else {
            // Proportional: fill available width, derive height from aspect ratio
            float srcW = sourceImage.getWidth();
            float srcH = sourceImage.getHeight();
            effectiveWidth = maxWidth;
            effectiveHeight = (srcH / srcW) * maxWidth;
        }

        return ImageUtils.getScaledDimension(effectiveWidth, effectiveHeight,
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
        private boolean explicitSize = false;
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

        /**
         * Creates a Builder from an image file (supports any format that {@link ImageIO}
         * can read, including PNG and JPEG/JPG).
         *
         * @param imageFile the image file to load
         * @throws IOException if reading the file fails
         */
        public Builder(File imageFile) throws IOException {
            Objects.requireNonNull(imageFile, "imageFile");
            this.sourceImage = ImageIO.read(imageFile);
            if (this.sourceImage == null) {
                throw new IOException("Unsupported or unreadable image format: " + imageFile);
            }
            this.requestedWidth = sourceImage.getWidth();
            this.requestedHeight = sourceImage.getHeight();
            this.cacheKey = "file-" + imageFile.getAbsolutePath();
        }

        /**
        * Creates a Builder from an {@link InputStream} (supports any format that
        * {@link ImageIO} can read, including PNG and JPEG/JPG).
        *
        * @apiNote The caller is responsible for closing the stream after construction.
        *          This constructor fully reads the stream into a {@link BufferedImage}
        *          but does not close it, following the standard Java convention that
        *          the opener of a resource is responsible for its lifecycle.
        *
        * @param inputStream the input stream containing image data
        * @throws IOException if reading the stream fails
        */
        public Builder(InputStream inputStream) throws IOException {
            Objects.requireNonNull(inputStream, "inputStream");
            this.sourceImage = ImageIO.read(inputStream);
            if (this.sourceImage == null) {
                throw new IOException("Unsupported or unreadable image format from InputStream");
            }
            this.requestedWidth = sourceImage.getWidth();
            this.requestedHeight = sourceImage.getHeight();
            this.cacheKey = "img-" + System.identityHashCode(sourceImage);
        }

        /**
         * Creates a Builder from a Base64-encoded image string.
         * Supports both raw Base64 and data-URI format
         * (e.g., {@code "data:image/png;base64,iVBOR..."} or
         * {@code "data:image/jpeg;base64,/9j/4AAQ..."}).
         * <p>
         * Any image format supported by {@link ImageIO} (PNG, JPEG, etc.) can be used.
         * </p>
         *
         * @param base64 the Base64-encoded image string
         * @throws IOException if decoding or reading the image fails
         */
        public static Builder fromBase64(String base64) throws IOException {
            Objects.requireNonNull(base64, "base64");
            String rawBase64 = base64;
            // Strip optional data-URI prefix (e.g., "data:image/png;base64,")
            int commaIndex = base64.indexOf(',');
            if (commaIndex >= 0 && base64.substring(0, commaIndex).contains("base64")) {
                rawBase64 = base64.substring(commaIndex + 1);
            }
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(rawBase64);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid Base64 image data", e);
            }
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new IOException("Unsupported or unreadable image format from Base64 string");
            }
            Builder builder = new Builder(img);
            builder.cacheKey = "base64-" + base64.hashCode();
            return builder;
        }

        /** Sets a custom cache key (e.g., file path). */
        public Builder cacheKey(String key) {
            this.cacheKey = Objects.requireNonNull(key);
            return this;
        }

        /** Sets desired dimensions in points, overriding proportional auto-sizing. */
        public Builder size(float width, float height) {
            this.requestedWidth = width;
            this.requestedHeight = height;
            this.explicitSize = true;
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

