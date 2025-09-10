package be.quodlibet.boxable;

import be.quodlibet.boxable.utils.FontUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.After;
import org.junit.Test;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Test class for Google Source Sans 3 font integration.
 * Verifies that custom fonts are properly loaded, cached, and work with all Boxable components.
 */
public class SourceSans3FontTest {

    @After
    public void cleanup() {
        // Clear fonts after each test to ensure clean state
        FontUtils.getDefaultfonts().clear();
    }

    /**
     * Test that Source Sans 3 fonts are properly loaded and all variants are available.
     */
    @Test
    public void testSourceSans3FontsLoading() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Set Source Sans 3 as default fonts
            FontUtils.setSourceSans3FontsAsDefault(doc);
            
            // Verify all font variants are loaded
            Map<String, PDFont> fonts = FontUtils.getDefaultfonts();
            assertNotNull("Default fonts map should not be null", fonts);
            assertFalse("Default fonts map should not be empty", fonts.isEmpty());
            
            // Verify all four variants exist
            assertTrue("Regular font should be loaded", fonts.containsKey("font"));
            assertTrue("Bold font should be loaded", fonts.containsKey("fontBold"));
            assertTrue("Italic font should be loaded", fonts.containsKey("fontItalic"));
            assertTrue("Bold Italic font should be loaded", fonts.containsKey("fontBoldItalic"));
            
            // Verify fonts are PDType0Font (embedded fonts)
            assertTrue("Regular font should be PDType0Font", fonts.get("font") instanceof PDType0Font);
            assertTrue("Bold font should be PDType0Font", fonts.get("fontBold") instanceof PDType0Font);
            assertTrue("Italic font should be PDType0Font", fonts.get("fontItalic") instanceof PDType0Font);
            assertTrue("Bold Italic font should be PDType0Font", fonts.get("fontBoldItalic") instanceof PDType0Font);
            
            // Verify fonts are not null
            assertNotNull("Regular font should not be null", fonts.get("font"));
            assertNotNull("Bold font should not be null", fonts.get("fontBold"));
            assertNotNull("Italic font should not be null", fonts.get("fontItalic"));
            assertNotNull("Bold Italic font should not be null", fonts.get("fontBoldItalic"));
            
        } finally {
            doc.close();
        }
    }

    /**
     * Test that fonts are cached and loaded only once.
     */
    @Test
    public void testFontCaching() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Clear any existing fonts first
            FontUtils.getDefaultfonts().clear();
            
            // Load fonts first time
            FontUtils.setSourceSans3FontsAsDefault(doc);
            Map<String, PDFont> fonts1 = FontUtils.getDefaultfonts();
            
            // Load fonts second time (should use cache)
            FontUtils.setSourceSans3FontsAsDefault(doc);
            Map<String, PDFont> fonts2 = FontUtils.getDefaultfonts();
            
            // Verify same instances are returned (cached)
            assertSame("Regular font should be cached", fonts1.get("font"), fonts2.get("font"));
            assertSame("Bold font should be cached", fonts1.get("fontBold"), fonts2.get("fontBold"));
            assertSame("Italic font should be cached", fonts1.get("fontItalic"), fonts2.get("fontItalic"));
            assertSame("Bold Italic font should be cached", fonts1.get("fontBoldItalic"), fonts2.get("fontBoldItalic"));
            
        } finally {
            doc.close();
        }
    }

    /**
     * Test concurrent font loading to ensure thread safety.
     */
    @Test
    public void testConcurrentFontLoading() throws InterruptedException, ExecutionException {
        final int NUM_THREADS = 10;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        
        try {
            // Submit multiple tasks that load fonts concurrently
            Future<Map<String, PDFont>>[] futures = new Future[NUM_THREADS];
            
            for (int i = 0; i < NUM_THREADS; i++) {
                futures[i] = executor.submit(() -> {
                    PDDocument doc = new PDDocument();
                    try {
                        FontUtils.setSourceSans3FontsAsDefault(doc);
                        return FontUtils.getDefaultfonts();
                    } finally {
                        try {
                            doc.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            
            // Wait for all tasks to complete and verify results
            Map<String, PDFont> firstResult = futures[0].get();
            for (int i = 1; i < NUM_THREADS; i++) {
                Map<String, PDFont> result = futures[i].get();
                assertNotNull("Font map should not be null", result);
                assertEquals("Font map should have 4 entries", 4, result.size());
                // Note: Due to separate documents, instances may differ, but fonts should be loaded successfully
                assertNotNull("Regular font should be loaded", result.get("font"));
                assertNotNull("Bold font should be loaded", result.get("fontBold"));
                assertNotNull("Italic font should be loaded", result.get("fontItalic"));
                assertNotNull("Bold Italic font should be loaded", result.get("fontBoldItalic"));
            }
            
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Test PDF generation with all four Source Sans 3 font variants.
     */
    @Test
    public void testPDFGenerationWithSourceSans3Fonts() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Set Source Sans 3 fonts as default
            FontUtils.setSourceSans3FontsAsDefault(doc);
            
            PDPage page = new PDPage();
            doc.addPage(page);
            
            // Create table with different font variants
            float margin = 10;
            float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
            float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
            float yStart = yStartNewPage;
            float bottomMargin = 70;
            
            BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, doc, page, true, true);
            
            // Header row with bold text
            Row<PDPage> headerRow = table.createRow(20f);
            Cell<PDPage> headerCell = headerRow.createCell(100, "Source Sans 3 Font Test - All Variants");
            headerCell.setHeaderCell(true); // This will use bold font
            headerCell.setFillColor(Color.LIGHT_GRAY);
            table.addHeaderRow(headerRow);
            
            // Row with all font variants
            Row<PDPage> row = table.createRow(30f);
            
            // Regular font
            Cell<PDPage> regularCell = row.createCell(25, "Regular Text | Google Source Sans 3 | Multiple Lines");
            regularCell.setFontSize(10);
            
            // Bold text using HTML
            Cell<PDPage> boldCell = row.createCell(25, "<b>Bold Text | Google Source Sans 3 | Multiple Lines</b>");
            boldCell.setFontSize(10);
            
            // Italic text using HTML  
            Cell<PDPage> italicCell = row.createCell(25, "<i>Italic Text | Google Source Sans 3 | Multiple Lines</i>");
            italicCell.setFontSize(10);
            
            // Bold Italic text using HTML
            Cell<PDPage> boldItalicCell = row.createCell(25, "<b><i>Bold Italic Text | Google Source Sans 3 | Multiple Lines</i></b>");
            boldItalicCell.setFontSize(10);
            
            // Test special characters that might fail with standard fonts
            Row<PDPage> specialCharsRow = table.createRow(20f);
            Cell<PDPage> specialCell = specialCharsRow.createCell(100, 
                "Special Characters Test: À Ñ Ü ß € £ ¥ © ® ™ • – — \" \" ' ' « » ¿ ¡");
            specialCell.setFontSize(8);
            
            table.draw();
            
            // Save the PDF
            File file = new File("target/SourceSans3FontTest.pdf");
            file.getParentFile().mkdirs();
            doc.save(file);
            
            // Verify file was created and has content
            assertTrue("PDF file should be created", file.exists());
            assertTrue("PDF file should have content", file.length() > 0);
            
        } finally {
            doc.close();
        }
    }

    /**
     * Test that existing PDF generation remains unaffected when not using custom fonts.
     */
    @Test
    public void testExistingFunctionalityUnaffected() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Clear any existing custom fonts to ensure clean test
            FontUtils.getDefaultfonts().clear();
            
            PDPage page = new PDPage();
            doc.addPage(page);
            
            // Create table WITHOUT setting custom fonts (should use Helvetica)
            float margin = 10;
            float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
            float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
            float yStart = yStartNewPage;
            float bottomMargin = 70;
            
            BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, doc, page, true, true);
            
            Row<PDPage> row = table.createRow(20f);
            Cell<PDPage> cell = row.createCell(100, "Default Font Test - Should use Helvetica");
            
            // Verify default font is used (Helvetica)
            PDFont cellFont = cell.getFont();
            assertTrue("Should use Helvetica when no custom fonts set", 
                      cellFont instanceof PDType1Font && 
                      cellFont.getName().contains("Helvetica"));
            
            table.draw();
            
            // Save the PDF
            File file = new File("target/DefaultFontTest.pdf");
            file.getParentFile().mkdirs();
            doc.save(file);
            
            assertTrue("PDF file should be created", file.exists());
            assertTrue("PDF file should have content", file.length() > 0);
            
        } finally {
            doc.close();
        }
    }

    /**
     * Test integration with Paragraph class.
     */
    @Test
    public void testParagraphIntegration() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            FontUtils.setSourceSans3FontsAsDefault(doc);
            
            // Create paragraph with HTML formatting
            String htmlText = "<b>Bold text</b> and <i>italic text</i> and <b><i>bold italic text</i></b> in Source Sans 3";
            Paragraph paragraph = new Paragraph(htmlText, FontUtils.getDefaultfonts().get("font"), 12, 400, 
                                              HorizontalAlignment.LEFT, null);
            
            // Verify paragraph uses custom fonts
            assertNotNull("Paragraph should be created", paragraph);
            assertEquals("Paragraph should use custom font", FontUtils.getDefaultfonts().get("font"), paragraph.getFont());
            
            // Test that paragraph can render lines
            assertNotNull("Paragraph should have lines", paragraph.getLines());
            assertTrue("Paragraph should have at least one line", paragraph.getLines().size() > 0);
            
        } finally {
            doc.close();
        }
    }

    /**
     * Test that custom fonts work correctly after clearing defaults.
     */
    @Test
    public void testFontReloading() throws IOException {
        PDDocument doc = new PDDocument();
        try {
            // Clear first to ensure clean state
            FontUtils.getDefaultfonts().clear();
            
            // Load Source Sans 3 fonts
            FontUtils.setSourceSans3FontsAsDefault(doc);
            Map<String, PDFont> sourceSansFonts = FontUtils.getDefaultfonts();
            assertFalse("Fonts should be loaded", sourceSansFonts.isEmpty());
            
            // Clear and load different fonts
            FontUtils.getDefaultfonts().clear();
            FontUtils.setSansFontsAsDefault(doc);
            Map<String, PDFont> freeSansFonts = FontUtils.getDefaultfonts();
            assertFalse("New fonts should be loaded", freeSansFonts.isEmpty());
            
            // Since fonts are loaded from different files, they should be different
            // But let's verify they are actually loaded
            assertNotNull("FreeSans font should be loaded", freeSansFonts.get("font"));
            assertNotNull("SourceSans font was loaded", sourceSansFonts.get("font"));
            
            // Load Source Sans 3 again
            FontUtils.getDefaultfonts().clear();
            FontUtils.setSourceSans3FontsAsDefault(doc);
            Map<String, PDFont> reloadedFonts = FontUtils.getDefaultfonts();
            assertNotNull("Fonts should be reloaded", reloadedFonts.get("font"));
            
        } finally {
            doc.close();
        }
    }
}