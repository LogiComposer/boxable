/*
 * Example demonstrating custom font injection using FontSet
 * This example corresponds to the usage pattern described in the problem statement.
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

public class FontInjectionExampleTest {

    /**
     * Example: Injecting Custom Fonts - demonstrates the exact usage pattern from the problem statement
     */
    @Test
    public void exampleInjectingCustomFonts() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        // For this example, we'll use Standard14Fonts since we don't have actual .ttf files in the test environment
        // In real usage, you would use: FontUtils.loadFontSet(document, familyName, regularPath, boldPath, italicPath, boldItalicPath)
        FontSet customFontSet = new FontSet(
            "OpenSans",
            new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN),
            new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD),
            new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC),
            new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC)
        );

        // Create BaseTable with injected font set
        float yStart = page.getMediaBox().getHeight() - 50;
        float yStartNewPage = yStart;
        float bottomMargin = 50;
        float width = page.getMediaBox().getWidth() - 100;
        float margin = 50;
        
        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, width, margin, document, page, true, true);
        table.setFontSet(customFontSet);  // <— new setter as specified in the problem statement

        // Add rows, cells, paragraphs
        Row<PDPage> row = table.createRow(12);
        Cell<PDPage> cell = row.createCell(50, "Custom font text here");
        
        // Test that we can access different font styles through the FontSet
        Cell<PDPage> cell2 = row.createCell(50, "Bold italic text");
        cell2.getParagraph().setFontStyle(FontStyle.BOLD_ITALIC); // picks from fontSet as specified

        // Add another row to demonstrate various font styles
        Row<PDPage> row2 = table.createRow(12);
        Cell<PDPage> regularCell = row2.createCell(25, "Regular");
        Cell<PDPage> boldCell = row2.createCell(25, "Bold");
        Cell<PDPage> italicCell = row2.createCell(25, "Italic");
        Cell<PDPage> boldItalicCell = row2.createCell(25, "Bold Italic");
        
        // Apply different font styles
        regularCell.getParagraph().setFontStyle(FontStyle.REGULAR);
        boldCell.getParagraph().setFontStyle(FontStyle.BOLD);
        italicCell.getParagraph().setFontStyle(FontStyle.ITALIC);
        boldItalicCell.getParagraph().setFontStyle(FontStyle.BOLD_ITALIC);

        table.draw();

        // Save the document
        File file = new File("target/FontInjectionExample.pdf");
        System.out.println("Font injection example saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }

    /**
     * Example: Using FontUtils.loadFontSet (simulated since we don't have actual .ttf files)
     */
    @Test 
    public void exampleLoadFontSetFromPaths() throws IOException {
        PDDocument document = new PDDocument();
        
        // This demonstrates the API that would be used with actual .ttf files:
        // FontSet customFontSet = FontUtils.loadFontSet(
        //     document,
        //     "OpenSans",
        //     "fonts/OpenSans-Regular.ttf",
        //     "fonts/OpenSans-Bold.ttf", 
        //     "fonts/OpenSans-Italic.ttf",
        //     "fonts/OpenSans-BoldItalic.ttf"
        // );
        
        // For testing purposes, create a FontSet manually
        FontSet testFontSet = new FontSet(
            "TestFamily",
            new PDType1Font(Standard14Fonts.FontName.COURIER),
            new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD),
            new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE),
            new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE)
        );

        // Verify FontSet works correctly
        assert testFontSet.getFamilyName().equals("TestFamily");
        assert testFontSet.getFont(FontStyle.REGULAR) != null;
        assert testFontSet.getFont(FontStyle.BOLD) != null;
        assert testFontSet.getFont(FontStyle.ITALIC) != null;
        assert testFontSet.getFont(FontStyle.BOLD_ITALIC) != null;

        document.close();
    }

    /**
     * Example: Backward compatibility test - no font injection
     */
    @Test
    public void exampleBackwardCompatibility() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        // Create BaseTable without injecting fonts - should use defaults
        float yStart = page.getMediaBox().getHeight() - 50;
        float yStartNewPage = yStart;
        float bottomMargin = 50;
        float width = page.getMediaBox().getWidth() - 100;
        float margin = 50;
        
        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, width, margin, document, page, true, true);
        // No setFontSet() call - should use default fonts

        // Verify default FontSet is created
        assert table.getFontSet() != null;
        assert table.getFontSet().getFamilyName().equals("Helvetica");

        // Add content
        Row<PDPage> row = table.createRow(12);
        Cell<PDPage> cell = row.createCell(100, "Default font text - backward compatible");

        table.draw();

        // Save the document
        File file = new File("target/BackwardCompatibilityExample.pdf");
        System.out.println("Backward compatibility example saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }
}