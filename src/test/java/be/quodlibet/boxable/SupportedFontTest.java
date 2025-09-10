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
import org.junit.Test;

import be.quodlibet.boxable.utils.FontUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SupportedFontTest {

    /**
     * Test loading fonts using the SupportedFont enum - FreeSans
     */
    @Test
    public void testLoadFontSetWithSupportedFontEnum_FreeSans() throws IOException {
        PDDocument document = new PDDocument();
        
        // Load FreeSans font family using the new enum approach
        FontSet freeSansSet = FontUtils.loadFontSet(document, SupportedFont.FREE_SANS);
        
        // Verify the FontSet was loaded successfully
        assertNotNull("FontSet should not be null", freeSansSet);
        assertEquals("FontSet family name should match", "FreeSans", freeSansSet.getFamilyName());
        assertNotNull("Regular font should not be null", freeSansSet.getRegular());
        assertNotNull("Bold font should not be null", freeSansSet.getBold());
        assertNotNull("Italic font should not be null", freeSansSet.getItalic());
        assertNotNull("Bold-Italic font should not be null", freeSansSet.getBoldItalic());
        
        // Create a simple table with this font
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        float yStart = page.getMediaBox().getHeight() - 50;
        float yStartNewPage = yStart;
        float bottomMargin = 50;
        float width = page.getMediaBox().getWidth() - 100;
        float margin = 50;
        
        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, width, margin, document, page, true, true);
        table.setFontSet(freeSansSet);

        // Create test content
        Row<PDPage> headerRow = table.createRow(20f);
        Cell<PDPage> headerCell = headerRow.createCell(100, "FreeSans Font Test (Using SupportedFont Enum)");
        headerCell.setTextColor(Color.BLUE);
        headerCell.setHeaderCell(true);

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

        table.draw();

        // Save the document
        File file = new File("target/SupportedFontTest_FreeSans.pdf");
        System.out.println("SupportedFont FreeSans test saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }

    /**
     * Test loading fonts using the SupportedFont enum - FreeSerif
     */
    @Test
    public void testLoadFontSetWithSupportedFontEnum_FreeSerif() throws IOException {
        PDDocument document = new PDDocument();
        
        // Load FreeSerif font family using the new enum approach
        FontSet freeSerifSet = FontUtils.loadFontSet(document, SupportedFont.FREE_SERIF);
        
        assertNotNull("FreeSerif FontSet should not be null", freeSerifSet);
        assertEquals("FontSet family name should match", "FreeSerif", freeSerifSet.getFamilyName());
        
        // Create a simple table
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
        Cell<PDPage> cell = row.createCell(100, "FreeSerif Font Test (Using SupportedFont Enum)");
        cell.setTextColor(Color.RED);

        table.draw();

        File file = new File("target/SupportedFontTest_FreeSerif.pdf");
        System.out.println("SupportedFont FreeSerif test saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }

    /**
     * Test loading fonts using the SupportedFont enum - FreeMono
     */
    @Test
    public void testLoadFontSetWithSupportedFontEnum_FreeMono() throws IOException {
        PDDocument document = new PDDocument();
        
        // Load FreeMono font family using the new enum approach
        FontSet freeMonoSet = FontUtils.loadFontSet(document, SupportedFont.FREE_MONO);
        
        assertNotNull("FreeMono FontSet should not be null", freeMonoSet);
        assertEquals("FontSet family name should match", "FreeMono", freeMonoSet.getFamilyName());
        
        // Create a simple table
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
        table.setFontSet(freeMonoSet);

        Row<PDPage> row = table.createRow(20f);
        Cell<PDPage> cell = row.createCell(100, "FreeMono Font Test (Using SupportedFont Enum)");
        cell.setTextColor(Color.GREEN);

        table.draw();

        File file = new File("target/SupportedFontTest_FreeMono.pdf");
        System.out.println("SupportedFont FreeMono test saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }

    /**
     * Test loading fonts using the SupportedFont enum - SourceSans3
     */
    @Test
    public void testLoadFontSetWithSupportedFontEnum_SourceSans3() throws IOException {
        PDDocument document = new PDDocument();
        
        // Load SourceSans3 font family using the new enum approach
        FontSet sourceSans3Set = FontUtils.loadFontSet(document, SupportedFont.SOURCE_SANS_3);
        
        assertNotNull("SourceSans3 FontSet should not be null", sourceSans3Set);
        assertEquals("FontSet family name should match", "SourceSans3", sourceSans3Set.getFamilyName());
        
        // Create a simple table
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
        table.setFontSet(sourceSans3Set);

        Row<PDPage> row = table.createRow(20f);
        Cell<PDPage> cell = row.createCell(100, "SourceSans3 Font Test (Using SupportedFont Enum)");
        cell.setTextColor(Color.MAGENTA);

        table.draw();

        File file = new File("target/SupportedFontTest_SourceSans3.pdf");
        System.out.println("SupportedFont SourceSans3 test saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }

    /**
     * Test that all enum values have correct paths
     */
    @Test
    public void testSupportedFontEnumValues() {
        // Test FREE_SANS
        assertEquals("FreeSans", SupportedFont.FREE_SANS.getFamilyName());
        assertEquals("fonts/FreeSans.ttf", SupportedFont.FREE_SANS.getRegularPath());
        assertEquals("fonts/FreeSansBold.ttf", SupportedFont.FREE_SANS.getBoldPath());
        assertEquals("fonts/FreeSansOblique.ttf", SupportedFont.FREE_SANS.getItalicPath());
        assertEquals("fonts/FreeSansBoldOblique.ttf", SupportedFont.FREE_SANS.getBoldItalicPath());
        
        // Test FREE_SERIF
        assertEquals("FreeSerif", SupportedFont.FREE_SERIF.getFamilyName());
        assertEquals("fonts/FreeSerif.ttf", SupportedFont.FREE_SERIF.getRegularPath());
        assertEquals("fonts/FreeSerifBold.ttf", SupportedFont.FREE_SERIF.getBoldPath());
        assertEquals("fonts/FreeSerifItalic.ttf", SupportedFont.FREE_SERIF.getItalicPath());
        assertEquals("fonts/FreeSerifBoldItalic.ttf", SupportedFont.FREE_SERIF.getBoldItalicPath());
        
        // Test FREE_MONO
        assertEquals("FreeMono", SupportedFont.FREE_MONO.getFamilyName());
        assertEquals("fonts/FreeMono.ttf", SupportedFont.FREE_MONO.getRegularPath());
        assertEquals("fonts/FreeMonoBold.ttf", SupportedFont.FREE_MONO.getBoldPath());
        assertEquals("fonts/FreeMonoOblique.ttf", SupportedFont.FREE_MONO.getItalicPath());
        assertEquals("fonts/FreeMonoBoldOblique.ttf", SupportedFont.FREE_MONO.getBoldItalicPath());
        
        // Test SOURCE_SANS_3
        assertEquals("SourceSans3", SupportedFont.SOURCE_SANS_3.getFamilyName());
        assertEquals("fonts/SourceSans3-Regular.ttf", SupportedFont.SOURCE_SANS_3.getRegularPath());
        assertEquals("fonts/SourceSans3-Bold.ttf", SupportedFont.SOURCE_SANS_3.getBoldPath());
        assertEquals("fonts/SourceSans3-It.ttf", SupportedFont.SOURCE_SANS_3.getItalicPath());
        assertEquals("fonts/SourceSans3-BoldIt.ttf", SupportedFont.SOURCE_SANS_3.getBoldItalicPath());
    }

    /**
     * Test backward compatibility - the old loadFontSet method should still work
     */
    @Test
    public void testBackwardCompatibilityWithOldLoadFontSet() throws IOException {
        PDDocument document = new PDDocument();
        
        // Use the old method with explicit paths (should still work)
        FontSet freeSansSetOld = FontUtils.loadFontSet(
            document,
            "FreeSans",
            "fonts/FreeSans.ttf",
            "fonts/FreeSansBold.ttf",
            "fonts/FreeSansOblique.ttf",
            "fonts/FreeSansBoldOblique.ttf"
        );
        
        // Use the new method with enum
        FontSet freeSansSetNew = FontUtils.loadFontSet(document, SupportedFont.FREE_SANS);
        
        // Both should work and produce equivalent results
        assertNotNull("Old method should still work", freeSansSetOld);
        assertNotNull("New method should work", freeSansSetNew);
        assertEquals("Both methods should produce same family name", 
                     freeSansSetOld.getFamilyName(), freeSansSetNew.getFamilyName());
        
        document.close();
    }
}