package be.quodlibet.boxable;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

/**
 * Test class to verify Source Sans 3 fonts can be loaded properly
 */
public class SourceSans3FontTest {

    @Before
    public void beforeTest() {
        FontUtils.clearDefaultFonts();
    }

    @After
    public void afterTest() {
        FontUtils.clearDefaultFonts();
    }
    /**
     * Test that Source Sans 3 fonts can be loaded and used
     */
    @Test
    public void testSourceSans3FontLoading() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try {
            // Set Source Sans 3 fonts as default
            FontUtils.setSourceSans3FontsAsDefault(document);
            
            // Get the default fonts
            java.util.Map<String, PDFont> fonts = FontUtils.getDefaultfonts();
            
            // Verify all font variants are loaded
            Assert.assertNotNull("Regular Source Sans 3 font should be loaded", fonts.get("font"));
            Assert.assertNotNull("Bold Source Sans 3 font should be loaded", fonts.get("fontBold"));
            Assert.assertNotNull("Italic Source Sans 3 font should be loaded", fonts.get("fontItalic"));
            Assert.assertNotNull("Bold-Italic Source Sans 3 font should be loaded", fonts.get("fontBoldItalic"));
            
            // Verify font names contain Source Sans
            String regularFontName = fonts.get("font").getName();
            Assert.assertTrue("Regular font should be Source Sans 3", 
                regularFontName.contains("SourceSans3") || regularFontName.contains("Source Sans"));
            
            System.out.println("Source Sans 3 fonts loaded successfully:");
            System.out.println("  Regular: " + fonts.get("font").getName());
            System.out.println("  Bold: " + fonts.get("fontBold").getName());
            System.out.println("  Italic: " + fonts.get("fontItalic").getName());
            System.out.println("  Bold-Italic: " + fonts.get("fontBoldItalic").getName());
            
        } finally {
            document.close();
        }
    }

    /**
     * Test creating a simple document with Source Sans 3 fonts
     */
    @Test
    public void testSourceSans3DocumentCreation() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try {
            // Set Source Sans 3 fonts as default
            FontUtils.setSourceSans3FontsAsDefault(document);
            
            // Create a simple table to test the fonts work
            BaseTable table = new BaseTable(50, 750, 200, 500, 70, document, page, true, true);
            Row<PDPage> headerRow = table.createRow(15f);
            Cell<PDPage> cell = headerRow.createCell(100, "Source Sans 3 Test");
            cell.setFont(FontUtils.getDefaultfonts().get("font"));
            table.addHeaderRow(headerRow);
            table.draw();

            // Save test file
            File testFile = new File("target/SourceSans3Test.pdf");
            document.save(testFile);
            
            Assert.assertTrue("Test PDF should be created", testFile.exists());
            Assert.assertTrue("Test PDF should have content", testFile.length() > 0);
            
            System.out.println("Source Sans 3 test document saved at: " + testFile.getAbsolutePath());
            
        } finally {
            document.close();
        }
    }

    /**
     * Test backward compatibility - existing code should still work
     */
    @Test 
    public void testBackwardCompatibility() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try {
            // Use existing FreeSans fonts
            FontUtils.setSansFontsAsDefault(document);
            
            // Verify FreeSans fonts still work
            java.util.Map<String, PDFont> fonts = FontUtils.getDefaultfonts();
            Assert.assertNotNull("FreeSans fonts should still work", fonts.get("font"));
            
            System.out.println("Backward compatibility test passed - FreeSans fonts still work");
            
        } finally {
            document.close();
        }
    }
}