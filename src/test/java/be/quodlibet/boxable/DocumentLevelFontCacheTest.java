package be.quodlibet.boxable;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test class to verify document-level font caching functionality in FontUtils.
 * This tests the new caching mechanism that prevents unnecessary PDType0Font object creation
 * for the same document and font path combinations.
 */
public class DocumentLevelFontCacheTest {

    /**
     * Test that the same PDType0Font object is returned for the same document and font path.
     */
    @Test
    public void testDocumentLevelFontObjectCaching() throws IOException {
        PDDocument document = new PDDocument();
        String fontPath = "fonts/FreeSans.ttf";
        
        // Load the same font multiple times for the same document
        PDType0Font font1 = FontUtils.loadFont(document, fontPath);
        PDType0Font font2 = FontUtils.loadFont(document, fontPath);
        PDType0Font font3 = FontUtils.loadFont(document, fontPath);
        
        assertNotNull("First font load should succeed", font1);
        assertNotNull("Second font load should succeed", font2);
        assertNotNull("Third font load should succeed", font3);
        
        // Verify that the exact same object is returned (object identity, not just equality)
        assertSame("Font objects should be identical for same document and path", font1, font2);
        assertSame("Font objects should be identical for same document and path", font1, font3);
        assertSame("Font objects should be identical for same document and path", font2, font3);
        
        document.close();
    }

    /**
     * Test that different documents get different PDType0Font objects even for the same font path.
     */
    @Test
    public void testDifferentDocumentsGetDifferentFontObjects() throws IOException {
        PDDocument document1 = new PDDocument();
        PDDocument document2 = new PDDocument();
        String fontPath = "fonts/FreeSans.ttf";
        
        // Load the same font for different documents
        PDType0Font font1 = FontUtils.loadFont(document1, fontPath);
        PDType0Font font2 = FontUtils.loadFont(document2, fontPath);
        
        assertNotNull("Font for document1 should not be null", font1);
        assertNotNull("Font for document2 should not be null", font2);
        
        // Font objects should be different instances for different documents
        assertTrue("Different documents should get different font objects", font1 != font2);
        
        // But they should have the same font name (loaded from same font file)
        assertEquals("Font names should be the same for same font file", font1.getName(), font2.getName());
        
        document1.close();
        document2.close();
    }

    /**
     * Test that different font paths for the same document create different font objects.
     */
    @Test
    public void testDifferentFontPathsCreateDifferentObjects() throws IOException {
        PDDocument document = new PDDocument();
        String fontPath1 = "fonts/FreeSans.ttf";
        String fontPath2 = "fonts/FreeSansBold.ttf";
        
        // Load different fonts for the same document
        PDType0Font font1 = FontUtils.loadFont(document, fontPath1);
        PDType0Font font2 = FontUtils.loadFont(document, fontPath2);
        
        assertNotNull("FreeSans font should not be null", font1);
        assertNotNull("FreeSansBold font should not be null", font2);
        
        // Font objects should be different instances for different font paths
        assertTrue("Different font paths should create different font objects", font1 != font2);
        
        // Load the same fonts again - should get cached objects
        PDType0Font font1Again = FontUtils.loadFont(document, fontPath1);
        PDType0Font font2Again = FontUtils.loadFont(document, fontPath2);
        
        // Verify caching works for both fonts
        assertSame("First font should be cached", font1, font1Again);
        assertSame("Second font should be cached", font2, font2Again);
        
        document.close();
    }

    /**
     * Test the cache size monitoring utilities.
     */
    @Test
    public void testCacheSizeMonitoring() throws IOException {
        // Start with a known state
        int initialDocumentCacheSize = FontUtils.getDocumentCacheSize();
        
        PDDocument document = new PDDocument();
        
        // Initially no fonts cached for this document
        assertEquals("No fonts should be cached initially", 0, FontUtils.getFontCacheSize(document));
        
        // Load first font
        PDType0Font font1 = FontUtils.loadFont(document, "fonts/FreeSans.ttf");
        assertNotNull("Font should load successfully", font1);
        
        // Check cache sizes
        assertEquals("Document cache should contain one more document", 
                     initialDocumentCacheSize + 1, FontUtils.getDocumentCacheSize());
        assertEquals("One font should be cached for this document", 1, FontUtils.getFontCacheSize(document));
        
        // Load second font
        PDType0Font font2 = FontUtils.loadFont(document, "fonts/FreeSansBold.ttf");
        assertNotNull("Second font should load successfully", font2);
        
        // Check cache sizes again
        assertEquals("Document cache size should remain the same", 
                     initialDocumentCacheSize + 1, FontUtils.getDocumentCacheSize());
        assertEquals("Two fonts should be cached for this document", 2, FontUtils.getFontCacheSize(document));
        
        // Load first font again - should be cached
        PDType0Font font1Again = FontUtils.loadFont(document, "fonts/FreeSans.ttf");
        assertSame("Font should come from cache", font1, font1Again);
        
        // Cache sizes should remain the same
        assertEquals("Document cache size should remain the same", 
                     initialDocumentCacheSize + 1, FontUtils.getDocumentCacheSize());
        assertEquals("Font cache size should remain the same", 2, FontUtils.getFontCacheSize(document));
        
        document.close();
    }

    /**
     * Test explicit cache clearing functionality.
     */
    @Test
    public void testExplicitCacheClearing() throws IOException {
        PDDocument document = new PDDocument();
        String fontPath = "fonts/FreeSans.ttf";
        
        // Load a font to populate the cache
        PDType0Font font = FontUtils.loadFont(document, fontPath);
        assertNotNull("Font should load successfully", font);
        
        // Verify it's cached
        assertEquals("One font should be cached", 1, FontUtils.getFontCacheSize(document));
        
        // Clear the cache for this document
        FontUtils.clearDocumentFontCache(document);
        
        // Cache should be empty now
        assertEquals("Font cache should be empty after clearing", 0, FontUtils.getFontCacheSize(document));
        
        // Loading the same font again should work and create a new cache entry
        PDType0Font font2 = FontUtils.loadFont(document, fontPath);
        assertNotNull("Font should load successfully after cache clear", font2);
        assertEquals("One font should be cached again", 1, FontUtils.getFontCacheSize(document));
        
        document.close();
    }

    /**
     * Test performance improvement with caching.
     */
    @Test
    public void testPerformanceImprovement() throws IOException {
        PDDocument document = new PDDocument();
        String fontPath = "fonts/FreeSans.ttf";
        
        // First load - creates the font object and caches it
        long startTime1 = System.nanoTime();
        PDType0Font font1 = FontUtils.loadFont(document, fontPath);
        long endTime1 = System.nanoTime();
        long firstLoadTime = endTime1 - startTime1;
        
        assertNotNull("First font load should succeed", font1);
        
        // Second load - should use cached object
        long startTime2 = System.nanoTime();
        PDType0Font font2 = FontUtils.loadFont(document, fontPath);
        long endTime2 = System.nanoTime();
        long secondLoadTime = endTime2 - startTime2;
        
        assertNotNull("Second font load should succeed", font2);
        assertSame("Second load should return cached object", font1, font2);
        
        // Print timing for analysis
        System.out.println("First load time (creates PDType0Font): " + firstLoadTime + " ns");
        System.out.println("Second load time (from cache): " + secondLoadTime + " ns");
        
        // Generally, the second load should be faster, though exact timing can vary
        // This is more of an informational test than a strict assertion
        
        document.close();
    }

    /**
     * Test null parameter handling with document-level caching.
     */
    @Test
    public void testNullParameterHandling() throws IOException {
        PDDocument document = new PDDocument();
        
        // Test null document
        PDType0Font fontWithNullDoc = FontUtils.loadFont(null, "fonts/FreeSans.ttf");
        assertEquals("Font with null document should be null", null, fontWithNullDoc);
        
        // Test null font path
        PDType0Font fontWithNullPath = FontUtils.loadFont(document, null);
        assertEquals("Font with null path should be null", null, fontWithNullPath);
        
        // Test both null
        PDType0Font fontWithBothNull = FontUtils.loadFont(null, null);
        assertEquals("Font with both null should be null", null, fontWithBothNull);
        
        // Cache operations with null should not crash
        FontUtils.clearDocumentFontCache(null);
        assertEquals("Cache size with null document should be 0", 0, FontUtils.getFontCacheSize(null));
        
        document.close();
    }
}