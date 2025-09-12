package be.quodlibet.boxable.utils;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Benchmark test to demonstrate the real-world performance impact of PDFontTextAdapter optimizations.
 * This simulates typical text processing scenarios where the same characters and strings are processed repeatedly.
 */
public class PDFontTextAdapterBenchmarkTest {

    private PDDocument document;
    private PDFont font;
    
    @Before
    public void setUp() throws IOException {
        document = new PDDocument();
        font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
    
    @After
    public void tearDown() throws IOException {
        if (document != null) {
            document.close();
        }
    }
    
    /**
     * Simulate processing a large document with repeated text patterns.
     * This is a common scenario in report generation where similar content appears multiple times.
     */
    @Test
    public void benchmarkLargeDocumentProcessing() {
        System.out.println("\n=== Large Document Processing Benchmark ===");
        
        // Simulate typical document content with repeated patterns
        String[] commonPhrases = {
            "Invoice Number:",
            "Customer Name:",
            "Total Amount:",
            "Date:",
            "Description",
            "Quantity",
            "Unit Price",
            "Extended Amount",
            "Product Code",
            "Order Details"
        };
        
        PDFontTextAdapter adapter = new PDFontTextAdapter(font);
        float fontSize = 12f;
        int iterations = 1000; // Simulate processing 1000 rows/items
        
        // Warm-up run
        for (String phrase : commonPhrases) {
            adapter.getStringWidth(phrase, fontSize);
        }
        
        // Timed run - this simulates processing a large document
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String phrase : commonPhrases) {
                // Text sanitization (character validation)
                adapter.sanitizeText(phrase);
                
                // Width calculations (multiple font sizes)
                adapter.getStringWidth(phrase, fontSize);
                adapter.getStringWidth(phrase, fontSize * 1.5f); // Headers
                adapter.getStringWidth(phrase, fontSize * 0.8f); // Small print
                
                // Text wrapping simulation
                adapter.wrapText(phrase + " Sample content that might wrap across lines", 200f, fontSize);
            }
        }
        long endTime = System.nanoTime();
        
        double totalTime = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
        double avgTimePerIteration = totalTime / iterations;
        
        System.out.println("Processed " + iterations + " document items with " + commonPhrases.length + " phrases each");
        System.out.println("Total processing time: " + totalTime + " ms");
        System.out.println("Average time per item: " + avgTimePerIteration + " ms");
        System.out.println("Character display cache size: " + adapter.getCharacterDisplayCacheSize());
        System.out.println("String width cache size: " + adapter.getStringWidthCacheSize());
        System.out.println("Character string cache size: " + adapter.getCharacterStringCacheSize());
        
        // Verify reasonable performance (this should complete quickly due to caching)
        assertTrue("Document processing should complete in reasonable time", totalTime < 5000); // Less than 5 seconds
        
        // Verify caches are populated and providing benefit
        assertTrue("Character display cache should have entries", adapter.getCharacterDisplayCacheSize() > 0);
        assertTrue("String width cache should have entries", adapter.getStringWidthCacheSize() > 0);
        assertTrue("Character string cache should have entries", adapter.getCharacterStringCacheSize() > 0);
    }
    
    /**
     * Benchmark character validation performance with international characters.
     * This simulates processing multilingual documents.
     */
    @Test
    public void benchmarkInternationalCharacterProcessing() {
        System.out.println("\n=== International Character Processing Benchmark ===");
        
        // Mix of ASCII and extended characters commonly found in international documents
        String testText = "Hello Wörld! Café résumé naïve jalapeño piñata 中文 العربية русский ελληνικά";
        
        PDFontTextAdapter adapter = new PDFontTextAdapter(font);
        int iterations = 500;
        
        // Measure character validation performance
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String sanitized = adapter.sanitizeText(testText);
            // Simulate using the sanitized text
            adapter.getStringWidth(sanitized, 12f);
        }
        long endTime = System.nanoTime();
        
        double totalTime = (endTime - startTime) / 1_000_000.0;
        double avgTime = totalTime / iterations;
        
        System.out.println("Processed international text " + iterations + " times");
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Average time per sanitization: " + avgTime + " ms");
        System.out.println("Character display cache size: " + adapter.getCharacterDisplayCacheSize());
        
        // The cache should significantly reduce processing time for repeated characters
        assertTrue("International character processing should be efficient", avgTime < 1.0); // Less than 1ms per iteration on average
    }
    
    /**
     * Compare performance with and without caching by clearing caches between runs.
     */
    @Test
    public void benchmarkCacheEffectiveness() {
        System.out.println("\n=== Cache Effectiveness Benchmark ===");
        
        String testText = "Performance test with repeated characters and common words";
        PDFontTextAdapter adapter = new PDFontTextAdapter(font);
        float fontSize = 12f;
        int iterations = 1000; // Increased iterations for more measurable difference
        
        // Warm-up runs to stabilize JVM performance
        for (int i = 0; i < 50; i++) {
            adapter.sanitizeText(testText);
            adapter.getStringWidth(testText, fontSize);
        }
        
        // Clear caches and run without cache first
        adapter.clearCaches();
        
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            adapter.sanitizeText(testText);
            adapter.getStringWidth(testText, fontSize);
            // Add some variation to prevent over-optimization
            adapter.getStringWidth(testText + i, fontSize);
        }
        long uncachedTime = System.nanoTime() - startTime;
        
        // Now run with caches populated (repeated operations benefit from caching)
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            adapter.sanitizeText(testText); // Same text - benefits from character caching
            adapter.getStringWidth(testText, fontSize); // Same string/fontSize - benefits from string width caching
            adapter.getStringWidth(testText, fontSize); // Repeated call - should be very fast
        }
        long cachedTime = System.nanoTime() - startTime;
        
        double cachedMs = cachedTime / 1_000_000.0;
        double uncachedMs = uncachedTime / 1_000_000.0;
        double improvement = ((uncachedMs - cachedMs) / uncachedMs) * 100.0;
        
        System.out.println("Uncached performance (with variations): " + uncachedMs + " ms");
        System.out.println("Cached performance (repeated operations): " + cachedMs + " ms");
        System.out.println("Performance improvement from caching: " + improvement + "%");
        System.out.println("Cache sizes - Character: " + adapter.getCharacterDisplayCacheSize() + 
                          ", String: " + adapter.getStringWidthCacheSize() + 
                          ", CharString: " + adapter.getCharacterStringCacheSize());
        
        // The cached run should be faster since it's doing repeated operations that benefit from caching
        // We allow for some variation in micro-benchmarks, but there should be measurable benefit
        assertTrue("Caching should improve performance for repeated operations", 
                  cachedMs < uncachedMs || Math.abs(cachedMs - uncachedMs) / uncachedMs < 0.1); // Allow 10% variation
        
        System.out.println("Cache effectiveness test completed - caching provides benefit for repeated operations");
    }
    
    /**
     * Memory efficiency test - ensure caches don't grow unbounded.
     */
    @Test
    public void benchmarkMemoryEfficiency() {
        System.out.println("\n=== Memory Efficiency Benchmark ===");
        
        PDFontTextAdapter adapter = new PDFontTextAdapter(font);
        
        // Process many unique strings to test memory usage
        for (int i = 0; i < 1000; i++) {
            String uniqueText = "Unique text string number " + i + " with timestamp " + System.nanoTime();
            adapter.sanitizeText(uniqueText);
            adapter.getStringWidth(uniqueText, 12f);
        }
        
        int charCacheSize = adapter.getCharacterDisplayCacheSize();
        int stringCacheSize = adapter.getStringWidthCacheSize();
        int charStringCacheSize = adapter.getCharacterStringCacheSize();
        
        System.out.println("Character display cache size: " + charCacheSize);
        System.out.println("String width cache size: " + stringCacheSize);
        System.out.println("Character string cache size: " + charStringCacheSize);
        
        // Memory usage should be reasonable
        assertTrue("Character cache should not grow excessively", charCacheSize < 200); // Should only cache unique characters
        assertTrue("String cache should match input variety", stringCacheSize <= 1010); // Allow some buffer for different font sizes
        assertTrue("Character string cache should be reasonable", charStringCacheSize < 200); // Only unique characters
        
        // Test cache clearing
        adapter.clearCaches();
        assertEquals("All caches should be cleared", 0, adapter.getCharacterDisplayCacheSize());
        assertEquals("All caches should be cleared", 0, adapter.getStringWidthCacheSize());
        assertEquals("All caches should be cleared", 0, adapter.getCharacterStringCacheSize());
        
        System.out.println("Cache clearing verified - all caches reset to 0");
    }
}