/*
 Quodlibet.be
 */
package be.quodlibet.boxable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class FontSetTest {

    /**
     * Test for custom font injection using FontSet
     */
    @Test
    public void testCustomFontSetInjection() throws IOException {
        // Set margins
        float margin = 10;

        // Initialize Document
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);

        // Create a custom FontSet with different fonts (using Standard14Fonts for testing)
        FontSet customFontSet = new FontSet(
            "TestFamily",
            new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN),          // Regular
            new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD),           // Bold
            new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC),         // Italic
            new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC)     // Bold-Italic
        );

        // Initialize table
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        boolean drawContent = true;
        float yStart = yStartNewPage;
        float bottomMargin = 70;
        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, doc, page, true,
                drawContent);

        // Inject custom font set
        table.setFontSet(customFontSet);

        // Verify that the FontSet was set correctly
        assertNotNull("FontSet should not be null", table.getFontSet());
        assertEquals("FontSet family name should match", "TestFamily", table.getFontSet().getFamilyName());
        assertSame("FontSet should be the same instance", customFontSet, table.getFontSet());

        // Create Header row
        Row<PDPage> headerRow = table.createRow(20f);
        Cell<PDPage> headerCell = headerRow.createCell(100, "Custom Font Test Header");
        headerCell.setTextColor(Color.BLUE);
        headerCell.setHeaderCell(true);
        
        // Create regular row
        Row<PDPage> row = table.createRow(15f);
        Cell<PDPage> cell1 = row.createCell(50, "Regular text with custom font");
        Cell<PDPage> cell2 = row.createCell(50, "Another cell with custom font");

        // Verify that cells are using the custom font
        assertNotNull("Header cell paragraph should not be null", headerCell.getParagraph());
        assertNotNull("Regular cell paragraph should not be null", cell1.getParagraph());
        
        // Test FontStyle on paragraph
        cell1.getParagraph().setFontStyle(FontStyle.ITALIC);
        cell2.getParagraph().setFontStyle(FontStyle.BOLD_ITALIC);

        table.draw();

        // Save the document
        File file = new File("target/FontSetTest.pdf");
        System.out.println("Custom font test file saved at : " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        doc.save(file);
        doc.close();
    }

    /**
     * Test for backward compatibility without custom fonts
     */
    @Test
    public void testBackwardCompatibility() throws IOException {
        // Set margins
        float margin = 10;

        // Initialize Document
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);

        // Initialize table without setting custom font
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        boolean drawContent = true;
        float yStart = yStartNewPage;
        float bottomMargin = 70;
        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, tableWidth, margin, doc, page, true,
                drawContent);

        // Verify that default FontSet is created
        assertNotNull("Default FontSet should not be null", table.getFontSet());
        assertEquals("Default FontSet should use Helvetica", "Helvetica", table.getFontSet().getFamilyName());

        // Create Header row
        Row<PDPage> headerRow = table.createRow(20f);
        Cell<PDPage> headerCell = headerRow.createCell(100, "Backward Compatibility Test Header");
        headerCell.setTextColor(Color.RED);
        headerCell.setHeaderCell(true);
        
        // Create regular row
        Row<PDPage> row = table.createRow(15f);
        Cell<PDPage> cell1 = row.createCell(50, "Regular text with default font");
        Cell<PDPage> cell2 = row.createCell(50, "Another cell with default font");

        // Verify that cells work with default fonts
        assertNotNull("Header cell paragraph should not be null", headerCell.getParagraph());
        assertNotNull("Regular cell paragraph should not be null", cell1.getParagraph());

        table.draw();

        // Save the document
        File file = new File("target/BackwardCompatibilityTest.pdf");
        System.out.println("Backward compatibility test file saved at : " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        doc.save(file);
        doc.close();
    }

    /**
     * Test FontUtils.getDefaultFontSet method
     */
    @Test
    public void testGetDefaultFontSet() {
        FontSet defaultFontSet = FontUtils.getDefaultFontSet();
        
        assertNotNull("Default FontSet should not be null", defaultFontSet);
        assertNotNull("Regular font should not be null", defaultFontSet.getRegular());
        assertNotNull("Bold font should not be null", defaultFontSet.getBold());
        assertNotNull("Italic font should not be null", defaultFontSet.getItalic());
        assertNotNull("Bold-Italic font should not be null", defaultFontSet.getBoldItalic());
        
        // Test FontStyle access
        assertNotNull("FontStyle.REGULAR should return a font", defaultFontSet.getFont(FontStyle.REGULAR));
        assertNotNull("FontStyle.BOLD should return a font", defaultFontSet.getFont(FontStyle.BOLD));
        assertNotNull("FontStyle.ITALIC should return a font", defaultFontSet.getFont(FontStyle.ITALIC));
        assertNotNull("FontStyle.BOLD_ITALIC should return a font", defaultFontSet.getFont(FontStyle.BOLD_ITALIC));
        
        assertSame("FontStyle.REGULAR should return regular font", 
                   defaultFontSet.getRegular(), defaultFontSet.getFont(FontStyle.REGULAR));
        assertSame("FontStyle.BOLD should return bold font", 
                   defaultFontSet.getBold(), defaultFontSet.getFont(FontStyle.BOLD));
    }
}