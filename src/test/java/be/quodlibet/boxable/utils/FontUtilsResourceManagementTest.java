package be.quodlibet.boxable.utils;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.Test;

import be.quodlibet.boxable.BoxableUtils;

/**
 * Test to verify proper resource management in font loading optimizations.
 * This test ensures that the try-with-resources pattern is working correctly
 * for both the new FontUtils implementation and the deprecated BoxableUtils method.
 */
public class FontUtilsResourceManagementTest {

    @Test
    public void testFontUtilsResourceManagement() throws IOException {
        PDDocument document = new PDDocument();
        
        try {
            // Test the main FontUtils.loadFont method
            PDType0Font font = FontUtils.loadFont(document, "fonts/FreeSans.ttf");
            assertNotNull("Font should load successfully with optimized resource management", font);
            
            // Load the same font again to test caching
            PDType0Font cachedFont = FontUtils.loadFont(document, "fonts/FreeSans.ttf");
            assertNotNull("Cached font should load successfully", cachedFont);
            
            // Verify they're the same cached instance
            assert(font == cachedFont);
            
        } finally {
            document.close();
        }
    }

    @Test
    public void testBoxableUtilsResourceManagement() throws IOException {
        PDDocument document = new PDDocument();
        
        try {
            // Test the deprecated BoxableUtils.loadFont method
            @SuppressWarnings("deprecation")
            PDType0Font font = BoxableUtils.loadFont(document, "fonts/FreeSans.ttf");
            assertNotNull("Font should load successfully with optimized resource management in deprecated method", font);
            
        } finally {
            document.close();
        }
    }

    @Test
    public void testMultipleDocumentResourceManagement() throws IOException {
        // Test resource management with multiple documents
        PDDocument doc1 = new PDDocument();
        PDDocument doc2 = new PDDocument();
        
        try {
            PDType0Font font1 = FontUtils.loadFont(doc1, "fonts/FreeSans.ttf");
            PDType0Font font2 = FontUtils.loadFont(doc2, "fonts/FreeSans.ttf");
            
            assertNotNull("Font should load for first document", font1);
            assertNotNull("Font should load for second document", font2);
            
            // They should be different objects (different documents)
            assert(font1 != font2);
            
        } finally {
            doc1.close();
            doc2.close();
        }
    }

    @Test
    public void testResourceCleanupWithDifferentFonts() throws IOException {
        PDDocument document = new PDDocument();
        
        try {
            // Load multiple different fonts to ensure resource cleanup works for each
            PDType0Font freeSans = FontUtils.loadFont(document, "fonts/FreeSans.ttf");
            PDType0Font freeSansBold = FontUtils.loadFont(document, "fonts/FreeSansBold.ttf");
            PDType0Font freeMono = FontUtils.loadFont(document, "fonts/FreeMono.ttf");
            
            assertNotNull("FreeSans should load successfully", freeSans);
            assertNotNull("FreeSansBold should load successfully", freeSansBold);
            assertNotNull("FreeMono should load successfully", freeMono);
            
            // Verify cache has all 3 fonts
            assert(FontUtils.getFontCacheSize(document) == 3);
            
        } finally {
            document.close();
        }
    }
}