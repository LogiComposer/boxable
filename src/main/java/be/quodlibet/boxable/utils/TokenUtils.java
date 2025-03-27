package be.quodlibet.boxable.utils;

import be.quodlibet.boxable.text.FontWidthCacheKey;
import be.quodlibet.boxable.text.Token;
import be.quodlibet.boxable.text.TokenWidthCacheKey;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class TokenUtils {
    private static final ConcurrentHashMap<TokenWidthCacheKey, Float> tokenWidthCache = new ConcurrentHashMap<>();

    public static float getOptimizedStringWidth(PDFont font, Token token) throws IOException {
        // Create a cache key to store unique width calculations
        TokenWidthCacheKey cacheKey = new TokenWidthCacheKey(font, token);

        // Check cache first to avoid redundant calculations
        return tokenWidthCache.computeIfAbsent(cacheKey, key -> {
            try {
                // Calculate width only if not in cache
                return key.getFont().getStringWidth(key.getToken().getData());
            } catch (IOException e) {
                // Handle potential exceptions
                throw new RuntimeException("Failed to calculate string width", e);
            }
        });
    }
}
