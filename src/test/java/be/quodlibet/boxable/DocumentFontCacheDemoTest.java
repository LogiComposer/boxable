package be.quodlibet.boxable;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Manual demonstration test to show the document-level font caching in action.
 */
public class DocumentFontCacheDemoTest {

    @Test
    public void demonstrateDocumentLevelCaching() throws IOException {
        // Create a document
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        String fontPath = "fonts/FreeSans.ttf";
        
        System.out.println("=== Document-Level Font Caching Demonstration ===");
        System.out.println("Document cache size before loading any fonts: " + FontUtils.getDocumentCacheSize());
        System.out.println("Font cache size for this document: " + FontUtils.getFontCacheSize(document));
        
        // Load the same font multiple times - should get cached objects
        System.out.println("\nLoading FreeSans font for the first time...");
        long start1 = System.nanoTime();
        PDType0Font font1 = FontUtils.loadFont(document, fontPath);
        long end1 = System.nanoTime();
        
        assertNotNull("Font should load successfully", font1);
        System.out.println("Font loaded: " + font1.getName());
        System.out.println("Time taken: " + (end1 - start1) + " nanoseconds");
        System.out.println("Document cache size: " + FontUtils.getDocumentCacheSize());
        System.out.println("Font cache size for this document: " + FontUtils.getFontCacheSize(document));
        
        System.out.println("\nLoading FreeSans font for the second time (should be cached)...");
        long start2 = System.nanoTime();
        PDType0Font font2 = FontUtils.loadFont(document, fontPath);
        long end2 = System.nanoTime();
        
        assertNotNull("Font should load successfully", font2);
        assertSame("Second load should return the same cached object", font1, font2);
        System.out.println("Font loaded: " + font2.getName());
        System.out.println("Time taken: " + (end2 - start2) + " nanoseconds");
        System.out.println("Performance improvement: " + ((double)(end1 - start1) / (end2 - start2)) + "x faster");
        System.out.println("Font cache size for this document: " + FontUtils.getFontCacheSize(document));
        
        // Load a different font for the same document
        System.out.println("\nLoading FreeSansBold font...");
        PDType0Font boldFont = FontUtils.loadFont(document, "fonts/FreeSansBold.ttf");
        assertNotNull("Bold font should load successfully", boldFont);
        System.out.println("Bold font loaded: " + boldFont.getName());
        System.out.println("Font cache size for this document: " + FontUtils.getFontCacheSize(document));
        
        // Use the fonts in a PDF to demonstrate they work
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(font1, 12);
            contentStream.newLineAtOffset(50, 750);
            contentStream.showText("This text uses the cached FreeSans font (loaded twice)");
            
            contentStream.setFont(boldFont, 12);
            contentStream.newLineAtOffset(0, -20);
            contentStream.showText("This text uses the FreeSansBold font");
            
            contentStream.endText();
        }
        
        // Save the demonstration PDF
        File outputFile = new File("target/DocumentLevelCacheDemo.pdf");
        document.save(outputFile);
        System.out.println("\nDemo PDF saved to: " + outputFile.getAbsolutePath());
        
        // Test with a different document
        System.out.println("\n=== Testing with a different document ===");
        PDDocument document2 = new PDDocument();
        System.out.println("Document cache size with second document: " + FontUtils.getDocumentCacheSize());
        
        PDType0Font font3 = FontUtils.loadFont(document2, fontPath);
        assertNotNull("Font should load for second document", font3);
        System.out.println("Font loaded for second document: " + font3.getName());
        System.out.println("Same font path, different document - different object: " + (font1 != font3));
        System.out.println("Document cache size: " + FontUtils.getDocumentCacheSize());
        
        // Clean up
        document.close();
        document2.close();
        
        System.out.println("\n=== Demonstration Complete ===");
    }
}