package be.quodlibet.boxable.richtext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
     * @apiNote The caller is responsible for closing the stream after construction.
     *          This method fully reads the stream into a {@link BufferedImage}
     *          but does not close it, following the standard Java convention that
     *          the opener of a resource is responsible for its lifecycle.
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
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid Base64 image data", e);
        }
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        if (img == null) {
            throw new IOException("Unsupported or unreadable image from Base64 string");
        }
        return new InlineImageSegment(img,
                "inline-b64-" + sha256Hex(raw.getBytes(StandardCharsets.UTF_8)),
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

    /**
     * Returns the lowercase hex-encoded SHA-256 digest of the given bytes.
     */
    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the Java specification; this should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

