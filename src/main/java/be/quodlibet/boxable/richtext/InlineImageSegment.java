package be.quodlibet.boxable.richtext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Objects;

/**
 * An inline image that flows on the same line as text within a {@link RichTextLine}.
 * <p>
 * Unlike the block-level {@link ImageContentElement}, an {@code InlineImageSegment}
 * is treated as an atomic, unsplittable unit during word-wrapping — similar to a
 * single word that cannot be hyphenated.
 * </p>
 * <p>
 * The expensive {@link org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject}
 * encoding is deferred to render time and cached via {@link ImageCache}.
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Inline image between two text segments
 * RichTextLine line = new RichTextLine(Arrays.asList(
 *         new RichTextSegment("Revenue grew by ", ...),
 *         InlineImageSegment.fromFile(chartFile, 40, 12),
 *         new RichTextSegment(" compared to last quarter.", ...)
 * ), ListType.NONE, 0, TextAlignment.LEFT);
 * }</pre>
 */
public final class InlineImageSegment implements LineElement {

    private final BufferedImage image;
    private final String cacheKey;
    private final float widthPt;
    private final float heightPt;
    private final float quality;

    private InlineImageSegment(BufferedImage image, String cacheKey,
                               float widthPt, float heightPt, float quality) {
        this.image = Objects.requireNonNull(image, "image");
        this.cacheKey = Objects.requireNonNull(cacheKey, "cacheKey");
        this.widthPt = widthPt;
        this.heightPt = heightPt;
        this.quality = quality;
    }

    // Factory methods -----------------------------------------------------

    /**
     * Creates an inline image from a {@link BufferedImage}.
     *
     * @param image   the source image
     * @param widthPt display width in points
     * @param heightPt display height in points
     */
    public static InlineImageSegment of(BufferedImage image, float widthPt, float heightPt) {
        return new InlineImageSegment(image,
                "inline-" + System.identityHashCode(image),
                widthPt, heightPt, 1.0f);
    }

    /**
     * Creates an inline image from a file (PNG, JPEG, etc.).
     *
     * @param imageFile the image file
     * @param widthPt   display width in points
     * @param heightPt  display height in points
     * @throws IOException if reading the file fails
     */
    public static InlineImageSegment fromFile(File imageFile, float widthPt,
                                              float heightPt) throws IOException {
        Objects.requireNonNull(imageFile, "imageFile");
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) {
            throw new IOException("Unsupported or unreadable image: " + imageFile);
        }
        return new InlineImageSegment(img,
                "inline-file-" + imageFile.getAbsolutePath(),
                widthPt, heightPt, 1.0f);
    }

    /**
     * Creates an inline image from an {@link InputStream} (PNG, JPEG, etc.).
     *
     * @param inputStream the image data stream
     * @param widthPt     display width in points
     * @param heightPt    display height in points
     * @throws IOException if reading the stream fails
     */
    public static InlineImageSegment fromStream(InputStream inputStream, float widthPt,
                                                float heightPt) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        BufferedImage img = ImageIO.read(inputStream);
        if (img == null) {
            throw new IOException("Unsupported or unreadable image from InputStream");
        }
        return new InlineImageSegment(img,
                "inline-stream-" + System.identityHashCode(img),
                widthPt, heightPt, 1.0f);
    }

    /**
     * Creates an inline image from a Base64-encoded string.
     * Supports both raw Base64 and data-URI format
     * (e.g., {@code "data:image/png;base64,iVBOR..."}).
     *
     * @param base64   the Base64 string
     * @param widthPt  display width in points
     * @param heightPt display height in points
     * @throws IOException if decoding or reading fails
     */
    public static InlineImageSegment fromBase64(String base64, float widthPt,
                                                float heightPt) throws IOException {
        Objects.requireNonNull(base64, "base64");
        String raw = base64;
        int commaIdx = base64.indexOf(',');
        if (commaIdx >= 0 && base64.substring(0, commaIdx).contains("base64")) {
            raw = base64.substring(commaIdx + 1);
        }
        byte[] bytes = Base64.getDecoder().decode(raw);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        if (img == null) {
            throw new IOException("Unsupported or unreadable image from Base64 string");
        }
        return new InlineImageSegment(img,
                "inline-b64-" + base64.hashCode(),
                widthPt, heightPt, 1.0f);
    }

    // ── LineElement contract ─────────────────────────────────────────────

    @Override
    public float getWidth() {
        return widthPt;
    }

    @Override
    public float getHeight() {
        return heightPt;
    }

    // ── Accessors ────────────────────────────────────────────────────────

    public BufferedImage getImage()  { return image; }
    public String getCacheKey()     { return cacheKey; }
    public float getQuality()       { return quality; }

    /**
     * Returns a copy with a custom cache key.
     */
    public InlineImageSegment withCacheKey(String key) {
        return new InlineImageSegment(image, key, widthPt, heightPt, quality);
    }

    /**
     * Returns a copy with a custom JPEG quality (0 &lt; q &le; 1).
     * Use 1.0 for lossless PNG encoding.
     */
    public InlineImageSegment withQuality(float q) {
        if (q <= 0 || q > 1) {
            throw new IllegalArgumentException("quality must be in (0, 1]");
        }
        return new InlineImageSegment(image, cacheKey, widthPt, heightPt, q);
    }
}

