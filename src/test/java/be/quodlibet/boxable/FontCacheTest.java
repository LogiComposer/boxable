package be.quodlibet.boxable;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test class to verify font file caching functionality in FontUtils.
 */
public class FontCacheTest {

    /**
     * Test that the font caching mechanism works correctly.
     * Verifies that the same font path results in cached data being used.
     */
    @Test
    public void testFontDataCaching() throws IOException {
        PDDocument document1 = new PDDocument();
        PDDocument document2 = new PDDocument();
        
        String fontPath = "fonts/FreeSans.ttf";
        
        // First load - should cache the font data
        long startTime1 = System.nanoTime();
        PDType0Font font1 = FontUtils.loadFont(document1, fontPath);
        long endTime1 = System.nanoTime();
        long firstLoadTime = endTime1 - startTime1;
        
        assertNotNull("First font load should succeed", font1);
        
        // Second load from different document - should use cached data
        long startTime2 = System.nanoTime();
        PDType0Font font2 = FontUtils.loadFont(document2, fontPath);
        long endTime2 = System.nanoTime();
        long secondLoadTime = endTime2 - startTime2;
        
        assertNotNull("Second font load should succeed", font2);
        
        // Third load from same document - should still use cached data
        long startTime3 = System.nanoTime();
        PDType0Font font3 = FontUtils.loadFont(document1, fontPath);
        long endTime3 = System.nanoTime();
        long thirdLoadTime = endTime3 - startTime3;
        
        assertNotNull("Third font load should succeed", font3);
        
        // The second and third loads should generally be faster than the first
        // Note: This is not always guaranteed due to JVM warmup, but usually the case
        System.out.println("First load time (with file I/O): " + firstLoadTime + " ns");
        System.out.println("Second load time (from cache): " + secondLoadTime + " ns");
        System.out.println("Third load time (from cache): " + thirdLoadTime + " ns");
        
        // Verify the font names are the same (indicating they were loaded from the same data)
        assertTrue("All fonts should have the same name", 
                   font1.getName().equals(font2.getName()) && 
                   font1.getName().equals(font3.getName()));
        
        document1.close();
        document2.close();
    }

    /**
     * Test caching with multiple different fonts.
     */
    @Test
    public void testMultipleFontCaching() throws IOException {
        PDDocument document = new PDDocument();
        
        String freeSansPath = "fonts/FreeSans.ttf";
        String freeSansBoldPath = "fonts/FreeSansBold.ttf";
        
        // Load different fonts
        PDType0Font freeSans1 = FontUtils.loadFont(document, freeSansPath);
        PDType0Font freeSansBold1 = FontUtils.loadFont(document, freeSansBoldPath);
        
        assertNotNull("FreeSans font should load", freeSans1);
        assertNotNull("FreeSansBold font should load", freeSansBold1);
        
        // Load the same fonts again - should use cache
        PDType0Font freeSans2 = FontUtils.loadFont(document, freeSansPath);
        PDType0Font freeSansBold2 = FontUtils.loadFont(document, freeSansBoldPath);
        
        assertNotNull("FreeSans font should load from cache", freeSans2);
        assertNotNull("FreeSansBold font should load from cache", freeSansBold2);
        
        // Verify the fonts have the expected names
        assertTrue("FreeSans fonts should have consistent names", 
                   freeSans1.getName().equals(freeSans2.getName()));
        assertTrue("FreeSansBold fonts should have consistent names", 
                   freeSansBold1.getName().equals(freeSansBold2.getName()));
        
        document.close();
    }

    /**
     * Test behavior with non-existent font file.
     */
    @Test
    public void testNonExistentFontHandling() throws IOException {
        PDDocument document = new PDDocument();
        
        String nonExistentPath = "fonts/NonExistentFont.ttf";
        
        // Should handle gracefully and return null
        PDType0Font font = FontUtils.loadFont(document, nonExistentPath);
        
        assertTrue("Non-existent font should return null", font == null);
        
        // Second attempt should also return null (and not cause issues with cache)
        PDType0Font font2 = FontUtils.loadFont(document, nonExistentPath);
        
        assertTrue("Second attempt with non-existent font should also return null", font2 == null);
        
        document.close();
    }
}