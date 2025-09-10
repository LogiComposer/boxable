/*
 * Test using actual TTF font files to validate FontUtils.loadFontSet method
 */
package be.quodlibet.boxable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class RealFontLoadingTest {

    /**
     * Test loading real TTF fonts using FontUtils.loadFontSet method
     */
    @Test
    public void testLoadFontSetWithRealFonts() throws IOException {
        PDDocument document = new PDDocument();
        
        // Load FreeSans font family using FontUtils.loadFontSet
        FontSet freeSansSet = FontUtils.loadFontSet(
            document,
            "FreeSans",
            "fonts/FreeSans.ttf",
            "fonts/FreeSansBold.ttf",
            "fonts/FreeSansOblique.ttf",
            "fonts/FreeSansBoldOblique.ttf"
        );

        // Verify the FontSet was loaded successfully
        assertNotNull("FontSet should not be null", freeSansSet);
        assertEquals("FontSet family name should match", "FreeSans", freeSansSet.getFamilyName());
        assertNotNull("Regular font should not be null", freeSansSet.getRegular());
        assertNotNull("Bold font should not be null", freeSansSet.getBold());
        assertNotNull("Italic font should not be null", freeSansSet.getItalic());
        assertNotNull("Bold-Italic font should not be null", freeSansSet.getBoldItalic());

        // Create a table and use the loaded FontSet
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        float yStart = page.getMediaBox().getHeight() - 50;
        float yStartNewPage = yStart;
        float bottomMargin = 50;
        float width = page.getMediaBox().getWidth() - 100;
        float margin = 50;
        
        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, width, margin, document, page, true, true);
        table.setFontSet(freeSansSet);

        // Create rows with different font styles
        Row<PDPage> headerRow = table.createRow(20f);
        Cell<PDPage> headerCell = headerRow.createCell(100, "FreeSans Font Test Header");
        headerCell.setTextColor(Color.BLUE);
        headerCell.setHeaderCell(true); // Should use bold font

        Row<PDPage> row1 = table.createRow(15f);
        Cell<PDPage> regularCell = row1.createCell(25, "Regular");
        Cell<PDPage> boldCell = row1.createCell(25, "Bold");
        Cell<PDPage> italicCell = row1.createCell(25, "Italic");  
        Cell<PDPage> boldItalicCell = row1.createCell(25, "Bold Italic");

        // Apply different font styles
        regularCell.getParagraph().setFontStyle(FontStyle.REGULAR);
        boldCell.getParagraph().setFontStyle(FontStyle.BOLD);
        italicCell.getParagraph().setFontStyle(FontStyle.ITALIC);
        boldItalicCell.getParagraph().setFontStyle(FontStyle.BOLD_ITALIC);

        Row<PDPage> row2 = table.createRow(15f);
        Cell<PDPage> testCell = row2.createCell(100, "This text uses the FreeSans font family loaded from TTF files");

        table.draw();

        // Save the document
        File file = new File("target/RealFontLoadingTest.pdf");
        System.out.println("Real font loading test saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }

    /**
     * Test loading FreeSerif font family
     */
    @Test
    public void testLoadFreeSerifFontSet() throws IOException {
        PDDocument document = new PDDocument();
        
        // Load FreeSerif font family
        FontSet freeSerifSet = FontUtils.loadFontSet(
            document,
            "FreeSerif",
            "fonts/FreeSerif.ttf",
            "fonts/FreeSerifBold.ttf",
            "fonts/FreeSerifItalic.ttf",
            "fonts/FreeSerifBoldItalic.ttf"
        );

        assertNotNull("FreeSerif FontSet should not be null", freeSerifSet);
        assertEquals("FontSet family name should match", "FreeSerif", freeSerifSet.getFamilyName());

        // Create a simple table with this font
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        BaseTable table = new BaseTable(
            page.getMediaBox().getHeight() - 50, 
            page.getMediaBox().getHeight() - 50, 
            50, 
            page.getMediaBox().getWidth() - 100, 
            50, 
            document, 
            page, 
            true, 
            true
        );
        table.setFontSet(freeSerifSet);

        Row<PDPage> row = table.createRow(20f);
        Cell<PDPage> cell = row.createCell(100, "FreeSerif Font Test - This is a serif font");
        cell.setTextColor(Color.RED);

        table.draw();

        File file = new File("target/FreeSerifTest.pdf");
        System.out.println("FreeSerif test saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }

    /**
     * Test that the setSansFontsAsDefault method works with our FontSet implementation
     */
    @Test
    public void testSetSansFontsAsDefaultWithFontSet() throws IOException {
        PDDocument document = new PDDocument();
        
        // Set FreeSans as default fonts using the existing method
        FontUtils.setSansFontsAsDefault(document);
        
        // Now get the default FontSet - should use the FreeSans fonts
        FontSet defaultFontSet = FontUtils.getDefaultFontSet();
        
        assertNotNull("Default FontSet should not be null", defaultFontSet);
        assertEquals("Default FontSet should be named 'Default'", "Default", defaultFontSet.getFamilyName());
        
        // Create a table without explicitly setting FontSet - should use FreeSans defaults
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        BaseTable table = new BaseTable(
            page.getMediaBox().getHeight() - 50, 
            page.getMediaBox().getHeight() - 50, 
            50, 
            page.getMediaBox().getWidth() - 100, 
            50, 
            document, 
            page, 
            true, 
            true
        );
        
        // Should automatically use FreeSans fonts as default
        assertNotNull("Table should have a FontSet", table.getFontSet());
        
        Row<PDPage> row = table.createRow(20f);
        Cell<PDPage> cell = row.createCell(100, "Default FreeSans Font Test");

        table.draw();

        File file = new File("target/DefaultFreeSansTest.pdf");
        System.out.println("Default FreeSans test saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }
}