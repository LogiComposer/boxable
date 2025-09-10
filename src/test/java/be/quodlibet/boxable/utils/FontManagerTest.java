package be.quodlibet.boxable.utils;

import be.quodlibet.boxable.utils.FontManager.FontStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Test class for FontManager functionality.
 * Verifies thread-safe font loading, caching, and management.
 */
public class FontManagerTest {

    private FontManager fontManager;

    @Before
    public void setUp() {
        fontManager = FontManager.getInstance();
        fontManager.clearAllCaches(); // Start with clean state
    }

    @After
    public void tearDown() {
        fontManager.clearAllCaches(); // Clean up after each test
    }

    /**
     * Test that FontManager follows singleton pattern.
     */
    @Test
    public void testSingletonPattern() {
        FontManager instance1 = FontManager.getInstance();
        FontManager instance2 = FontManager.getInstance();
        
        assertSame("FontManager should follow singleton pattern", instance1, instance2);
    }

    /**
     * Test loading individual font styles.
     */
    @Test
    public void testIndividualFontLoading() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Test each font style
            PDFont regularFont = fontManager.getFont(doc, FontStyle.REGULAR);
            PDFont boldFont = fontManager.getFont(doc, FontStyle.BOLD);
            PDFont italicFont = fontManager.getFont(doc, FontStyle.ITALIC);
            PDFont boldItalicFont = fontManager.getFont(doc, FontStyle.BOLD_ITALIC);

            // Verify fonts are loaded
            assertNotNull("Regular font should be loaded", regularFont);
            assertNotNull("Bold font should be loaded", boldFont);
            assertNotNull("Italic font should be loaded", italicFont);
            assertNotNull("Bold Italic font should be loaded", boldItalicFont);

            // Verify fonts are PDType0Font (embedded fonts)
            assertTrue("Regular font should be PDType0Font", regularFont instanceof PDType0Font);
            assertTrue("Bold font should be PDType0Font", boldFont instanceof PDType0Font);
            assertTrue("Italic font should be PDType0Font", italicFont instanceof PDType0Font);
            assertTrue("Bold Italic font should be PDType0Font", boldItalicFont instanceof PDType0Font);

            // Verify fonts are different instances for different styles
            assertNotSame("Different font styles should have different instances", regularFont, boldFont);
            assertNotSame("Different font styles should have different instances", regularFont, italicFont);
            assertNotSame("Different font styles should have different instances", boldFont, italicFont);

        } finally {
            doc.close();
        }
    }

    /**
     * Test font caching within the same document.
     */
    @Test
    public void testFontCachingInSameDocument() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Load same font multiple times
            PDFont font1 = fontManager.getFont(doc, FontStyle.REGULAR);
            PDFont font2 = fontManager.getFont(doc, FontStyle.REGULAR);
            PDFont font3 = fontManager.getFont(doc, FontStyle.BOLD);
            PDFont font4 = fontManager.getFont(doc, FontStyle.BOLD);

            // Verify same instances are returned for same style in same document
            assertSame("Same font style in same document should return cached instance", font1, font2);
            assertSame("Same font style in same document should return cached instance", font3, font4);

            // Verify different styles return different instances
            assertNotSame("Different font styles should return different instances", font1, font3);

        } finally {
            doc.close();
        }
    }

    /**
     * Test font loading across different documents.
     */
    @Test
    public void testFontLoadingAcrossDocuments() throws IOException {
        PDDocument doc1 = new PDDocument();
        PDDocument doc2 = new PDDocument();
        try {
            // Load same font style in different documents
            PDFont font1Doc1 = fontManager.getFont(doc1, FontStyle.REGULAR);
            PDFont font1Doc2 = fontManager.getFont(doc2, FontStyle.REGULAR);

            // Fonts should be different instances (document-specific) but both valid
            assertNotSame("Font instances should be different for different documents", font1Doc1, font1Doc2);
            assertNotNull("Font for doc1 should not be null", font1Doc1);
            assertNotNull("Font for doc2 should not be null", font1Doc2);

            // Both should be PDType0Font
            assertTrue("Font for doc1 should be PDType0Font", font1Doc1 instanceof PDType0Font);
            assertTrue("Font for doc2 should be PDType0Font", font1Doc2 instanceof PDType0Font);

        } finally {
            doc1.close();
            doc2.close();
        }
    }

    /**
     * Test loading all Source Sans 3 fonts.
     */
    @Test
    public void testLoadSourceSans3Fonts() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            Map<String, PDFont> fonts = fontManager.loadSourceSans3Fonts(doc);

            // Verify all fonts are loaded
            assertNotNull("Fonts map should not be null", fonts);
            assertEquals("Should have 4 font variants", 4, fonts.size());
            
            assertTrue("Should contain regular font", fonts.containsKey("font"));
            assertTrue("Should contain bold font", fonts.containsKey("fontBold"));
            assertTrue("Should contain italic font", fonts.containsKey("fontItalic"));
            assertTrue("Should contain bold italic font", fonts.containsKey("fontBoldItalic"));

            // Verify all fonts are not null and are PDType0Font
            for (Map.Entry<String, PDFont> entry : fonts.entrySet()) {
                assertNotNull(entry.getKey() + " should not be null", entry.getValue());
                assertTrue(entry.getKey() + " should be PDType0Font", entry.getValue() instanceof PDType0Font);
            }

        } finally {
            doc.close();
        }
    }

    /**
     * Test concurrent font loading to ensure thread safety.
     */
    @Test
    public void testConcurrentFontLoading() throws InterruptedException, ExecutionException {
        final int NUM_THREADS = 10; // Reduced for faster test execution
        final int NUM_DOCS_PER_THREAD = 3;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        try {
            // Submit multiple tasks that load fonts concurrently
            Future<Boolean>[] futures = new Future[NUM_THREADS];

            for (int i = 0; i < NUM_THREADS; i++) {
                futures[i] = executor.submit(() -> {
                    try {
                        for (int j = 0; j < NUM_DOCS_PER_THREAD; j++) {
                            PDDocument doc = new PDDocument();
                            try {
                                // Load all font styles
                                for (FontStyle style : FontStyle.values()) {
                                    PDFont font = fontManager.getFont(doc, style);
                                    assertNotNull("Font should not be null", font);
                                    assertTrue("Font should be PDType0Font", font instanceof PDType0Font);
                                }
                                
                                // Load all fonts via loadSourceSans3Fonts method
                                Map<String, PDFont> fonts = fontManager.loadSourceSans3Fonts(doc);
                                assertEquals("Should have 4 fonts", 4, fonts.size());
                                
                            } finally {
                                doc.close();
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                });
            }

            // Wait for all tasks to complete and verify results
            for (int i = 0; i < NUM_THREADS; i++) {
                Boolean result = futures[i].get();
                assertTrue("Thread " + i + " should complete successfully", result);
            }

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Test cache size monitoring.
     */
    @Test
    public void testCacheSizeMonitoring() throws IOException {
        // Start with empty caches
        assertEquals("Font data cache should be empty initially", 0, fontManager.getFontDataCacheSize());
        assertEquals("Font instance cache should be empty initially", 0, fontManager.getFontInstanceCacheSize());

        PDDocument doc1 = new PDDocument();
        PDDocument doc2 = new PDDocument();
        try {
            // Load fonts - should populate caches
            fontManager.getFont(doc1, FontStyle.REGULAR);
            fontManager.getFont(doc1, FontStyle.BOLD);
            
            // Should have 2 font data entries and 2 font instances
            assertTrue("Font data cache should have entries", fontManager.getFontDataCacheSize() >= 2);
            assertTrue("Font instance cache should have entries", fontManager.getFontInstanceCacheSize() >= 2);

            // Load same fonts for different document
            fontManager.getFont(doc2, FontStyle.REGULAR);
            fontManager.getFont(doc2, FontStyle.BOLD);
            
            // Font data cache should not grow (same fonts), but instance cache should
            assertTrue("Font instance cache should grow for new document", fontManager.getFontInstanceCacheSize() >= 4);

        } finally {
            doc1.close();
            doc2.close();
        }
    }

    /**
     * Test clearing document fonts.
     */
    @Test
    public void testClearDocumentFonts() throws IOException {
        PDDocument doc1 = new PDDocument();
        PDDocument doc2 = new PDDocument();
        try {
            // Load fonts for both documents
            fontManager.getFont(doc1, FontStyle.REGULAR);
            fontManager.getFont(doc2, FontStyle.REGULAR);
            
            int initialInstanceCacheSize = fontManager.getFontInstanceCacheSize();
            assertTrue("Should have font instances cached", initialInstanceCacheSize >= 2);

            // Clear fonts for doc1
            fontManager.clearDocumentFonts(doc1);
            
            // Instance cache should be smaller, but font data cache should remain
            int afterClearInstanceCacheSize = fontManager.getFontInstanceCacheSize();
            assertTrue("Instance cache should be smaller after clearing doc1", 
                      afterClearInstanceCacheSize < initialInstanceCacheSize);
            
            // Font data cache should remain (for performance)
            assertTrue("Font data cache should remain", fontManager.getFontDataCacheSize() > 0);

        } finally {
            doc1.close();
            doc2.close();
        }
    }

    /**
     * Test parameter validation.
     */
    @Test
    public void testParameterValidation() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Test null document
            try {
                fontManager.getFont(null, FontStyle.REGULAR);
                fail("Should throw IllegalArgumentException for null document");
            } catch (IllegalArgumentException e) {
                assertEquals("PDDocument cannot be null", e.getMessage());
            }

            // Test null font style
            try {
                fontManager.getFont(doc, null);
                fail("Should throw IllegalArgumentException for null font style");
            } catch (IllegalArgumentException e) {
                assertEquals("FontStyle cannot be null", e.getMessage());
            }

            // Test null document for loadSourceSans3Fonts
            try {
                fontManager.loadSourceSans3Fonts(null);
                fail("Should throw IllegalArgumentException for null document");
            } catch (IllegalArgumentException e) {
                assertEquals("PDDocument cannot be null", e.getMessage());
            }

        } finally {
            doc.close();
        }
    }

    /**
     * Test that font data is cached and reused.
     */
    @Test
    public void testFontDataCaching() throws IOException {
        PDDocument doc1 = new PDDocument();
        PDDocument doc2 = new PDDocument();
        try {
            // Clear caches to start fresh
            fontManager.clearAllCaches();
            
            // Load font for first document
            fontManager.getFont(doc1, FontStyle.REGULAR);
            int fontDataCacheSizeAfterFirst = fontManager.getFontDataCacheSize();
            
            // Load same font for second document
            fontManager.getFont(doc2, FontStyle.REGULAR);
            int fontDataCacheSizeAfterSecond = fontManager.getFontDataCacheSize();
            
            // Font data cache size should not increase (same font file)
            assertEquals("Font data cache should not grow for same font", 
                        fontDataCacheSizeAfterFirst, fontDataCacheSizeAfterSecond);
            
            // But instance cache should have grown
            assertTrue("Instance cache should have entries for both documents", 
                      fontManager.getFontInstanceCacheSize() >= 2);

        } finally {
            doc1.close();
            doc2.close();
        }
    }
}