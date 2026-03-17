package be.quodlibet.boxable.richtext;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, per-{@link PDDocument} cache for {@link PDImageXObject} instances.
 * <p>
 * Creating a {@code PDImageXObject} (especially via {@code LosslessFactory} or
 * {@code JPEGFactory}) is expensive.  This cache ensures each unique image source
 * is encoded only once per document.
 * </p>
 * <p>
 * The outer map uses a {@link WeakHashMap} so that entries are automatically
 * garbage-collected when the {@code PDDocument} is closed/unreachable — the same
 * pattern used by {@link be.quodlibet.boxable.utils.FontCacheManager}.
 * </p>
 */
public final class ImageCache {

    private static final Logger logger = LoggerFactory.getLogger(ImageCache.class);

    /**
     * WeakHashMap: when a PDDocument is GC'd, its image cache is released.
     * Inner map is ConcurrentHashMap for thread safety within a single document.
     */
    private static final Map<PDDocument, Map<String, PDImageXObject>> DOCUMENT_CACHE =
            new WeakHashMap<>();

    private ImageCache() {
        // utility class
    }

    /**
     * Retrieves or creates a {@link PDImageXObject} for the given document and cache key.
     *
     * @param document the target PDF document
     * @param cacheKey a unique identifier for the image (e.g., file path, hash code)
     * @param image    the source {@link BufferedImage}
     * @param quality  JPEG quality (0 &lt; quality &le; 1).  Use {@code 1.0f} for lossless PNG encoding.
     * @return the cached or newly created {@link PDImageXObject}
     * @throws IOException if image encoding fails
     */
    public static PDImageXObject getOrCreate(PDDocument document, String cacheKey,
                                             BufferedImage image, float quality) throws IOException {
        Map<String, PDImageXObject> docCache = getDocumentCache(document);

        PDImageXObject cached = docCache.get(cacheKey);
        if (cached != null) {
            logger.debug("Image cache hit for key: {}", cacheKey);
            return cached;
        }

        // Encode image — this is the expensive operation
        PDImageXObject xObject;
        if (quality >= 1.0f) {
            xObject = LosslessFactory.createFromImage(document, image);
        } else {
            xObject = JPEGFactory.createFromImage(document, image, quality);
        }

        docCache.put(cacheKey, xObject);
        logger.debug("Image cached for key: {}", cacheKey);
        return xObject;
    }

    /**
     * Retrieves or creates a lossless {@link PDImageXObject} (quality = 1.0).
     *
     * @param document the target PDF document
     * @param cacheKey a unique identifier for the image
     * @param image    the source {@link BufferedImage}
     * @return the cached or newly created {@link PDImageXObject}
     * @throws IOException if image encoding fails
     */
    public static PDImageXObject getOrCreate(PDDocument document, String cacheKey,
                                             BufferedImage image) throws IOException {
        return getOrCreate(document, cacheKey, image, 1.0f);
    }

    /**
     * Stores an already-created {@link PDImageXObject} in the cache.
     *
     * @param document the target PDF document
     * @param cacheKey a unique identifier for the image
     * @param xObject  the image object to cache
     */
    public static void put(PDDocument document, String cacheKey, PDImageXObject xObject) {
        getDocumentCache(document).put(cacheKey, xObject);
    }

    /**
     * Clears all cached images for the given document.
     *
     * @param document the target PDF document
     */
    public static void clear(PDDocument document) {
        synchronized (DOCUMENT_CACHE) {
            DOCUMENT_CACHE.remove(document);
        }
    }

    private static Map<String, PDImageXObject> getDocumentCache(PDDocument document) {
        synchronized (DOCUMENT_CACHE) {
            return DOCUMENT_CACHE.computeIfAbsent(document, k -> new ConcurrentHashMap<>());
        }
    }
}

