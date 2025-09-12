package be.quodlibet.boxable.utils;

import org.junit.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import be.quodlibet.boxable.*;
import static org.junit.Assert.*;

/**
 * Test for document-level shared FontTextCache functionality
 */
public class SharedFontTextCacheTest {

    @Test
    public void testSharedCacheAcrossFonts() throws Exception {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        
        try {
            // Create BaseTable which has its own FontTextCache
            BaseTable table = new BaseTable(750, 750, 50, 500, 50, document, page, true, true);
            
            // Get the shared cache
            FontTextCache sharedCache = table.getFontTextCache();
            assertNotNull("Shared cache should not be null", sharedCache);
            
            // Create multiple PDFontTextAdapters using the shared cache
            PDFontTextAdapter adapter1 = new PDFontTextAdapter(new PDType1Font(Standard14Fonts.FontName.HELVETICA), sharedCache);
            PDFontTextAdapter adapter2 = new PDFontTextAdapter(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), sharedCache);
            
            // Test some text operations
            String testText = "Hello World";
            float fontSize = 12.0f;
            
            // Initial cache should be empty or small
            int initialStringCacheSize = sharedCache.getStringWidthCacheSize();
            
            // These should populate the shared cache
            float width1 = adapter1.getStringWidth(testText, fontSize);
            float width2 = adapter2.getStringWidth(testText, fontSize);
            
            assertTrue("Helvetica width should be positive", width1 > 0);
            assertTrue("Helvetica-Bold width should be positive", width2 > 0);
            
            // Cache should have grown
            int finalStringCacheSize = sharedCache.getStringWidthCacheSize();
            assertTrue("Cache should have entries after text processing", 
                      finalStringCacheSize > initialStringCacheSize);
            
            // Create a row and cell to test Cell's use of shared cache
            Row<PDPage> row = table.createRow(20);
            Cell<PDPage> cell = row.createCell(100, "Test cell text");
            
            // Get the cell's paragraph to trigger text processing, which will use the shared cache
            cell.getParagraph();
            
            // Cache should have grown further after paragraph processing
            int finalCacheSize = sharedCache.getStringWidthCacheSize();
            assertTrue("Cache should have more entries after cell text processing", 
                      finalCacheSize >= finalStringCacheSize);
            
            System.out.println("=== Shared Cache Test Results ===");
            System.out.println("Initial cache size: " + initialStringCacheSize);
            System.out.println("After font adapters: " + finalStringCacheSize);  
            System.out.println("After cell processing: " + finalCacheSize);
            System.out.println("Helvetica width: " + width1);
            System.out.println("Helvetica-Bold width: " + width2);
            
        } finally {
            document.close();
        }
    }
    
    @Test
    public void testCacheClearing() throws Exception {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        
        try {
            BaseTable table = new BaseTable(750, 750, 50, 500, 50, document, page, true, true);
            FontTextCache cache = table.getFontTextCache();
            
            // Populate cache
            PDFontTextAdapter adapter = new PDFontTextAdapter(new PDType1Font(Standard14Fonts.FontName.HELVETICA), cache);
            adapter.getStringWidth("Test text", 12.0f);
            
            assertTrue("Cache should have entries", cache.getStringWidthCacheSize() > 0);
            
            // Clear cache
            table.clearFontTextCache();
            
            assertEquals("Cache should be empty after clearing", 0, cache.getStringWidthCacheSize());
            assertEquals("Character display cache should be empty", 0, cache.getCharacterDisplayCacheSize());
            assertEquals("Character string cache should be empty", 0, cache.getCharacterStringCacheSize());
            
        } finally {
            document.close();
        }
    }
}