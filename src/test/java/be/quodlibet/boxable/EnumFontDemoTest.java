/*
 * Demonstration of the improved SupportedFont enum approach vs. the old hardcoded paths approach
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

public class EnumFontDemoTest {

    /**
     * Demonstrates the old approach vs. the new enum approach
     */
    @Test
    public void demonstrateImprovement() throws IOException {
        PDDocument document = new PDDocument();
        
        // OLD APPROACH - difficult to refer font paths (as mentioned in problem statement)
        FontSet freeSansSetOld = FontUtils.loadFontSet(
            document,
            "FreeSans",
            "fonts/FreeSans.ttf",              // Hard to remember and maintain
            "fonts/FreeSansBold.ttf",          // Path could be wrong
            "fonts/FreeSansOblique.ttf",       // Typos are easy
            "fonts/FreeSansBoldOblique.ttf"    // Long and error-prone
        );
        
        // NEW APPROACH - much cleaner using enum
        FontSet freeSansSetNew = FontUtils.loadFontSet(document, SupportedFont.FREE_SANS);
        FontSet freeSerifSet = FontUtils.loadFontSet(document, SupportedFont.FREE_SERIF);
        FontSet freeMonoSet = FontUtils.loadFontSet(document, SupportedFont.FREE_MONO);
        FontSet sourceSans3Set = FontUtils.loadFontSet(document, SupportedFont.SOURCE_SANS_3);
        
        // Create demonstration PDF
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        float yStart = page.getMediaBox().getHeight() - 50;
        float yStartNewPage = yStart;
        float bottomMargin = 50;
        float width = page.getMediaBox().getWidth() - 100;
        float margin = 50;
        
        BaseTable table = new BaseTable(yStart, yStartNewPage, bottomMargin, width, margin, document, page, true, true);
        
        // Header
        table.setFontSet(freeSansSetNew);
        Row<PDPage> headerRow = table.createRow(25f);
        Cell<PDPage> headerCell = headerRow.createCell(100, "Font Loading Improvement Demonstration");
        headerCell.setTextColor(Color.BLUE);
        headerCell.setHeaderCell(true);
        
        // Old approach example
        table.setFontSet(freeSansSetOld);
        Row<PDPage> oldRow = table.createRow(20f);
        Cell<PDPage> oldLabel = oldRow.createCell(30, "Old Approach:");
        Cell<PDPage> oldCode = oldRow.createCell(70, "FontUtils.loadFontSet(doc, \"FreeSans\", \"fonts/FreeSans.ttf\", ...)");
        oldLabel.setTextColor(Color.RED);
        oldCode.getParagraph().setFontStyle(FontStyle.ITALIC);
        
        // New approach examples
        table.setFontSet(freeSansSetNew);
        Row<PDPage> newRow1 = table.createRow(15f);
        Cell<PDPage> newLabel1 = newRow1.createCell(30, "New Approach:");
        Cell<PDPage> newCode1 = newRow1.createCell(70, "FontUtils.loadFontSet(doc, SupportedFont.FREE_SANS)");
        newLabel1.setTextColor(Color.GREEN);
        newCode1.getParagraph().setFontStyle(FontStyle.BOLD);
        
        table.setFontSet(freeSerifSet);
        Row<PDPage> newRow2 = table.createRow(15f);
        Cell<PDPage> newLabel2 = newRow2.createCell(30, "");
        Cell<PDPage> newCode2 = newRow2.createCell(70, "FontUtils.loadFontSet(doc, SupportedFont.FREE_SERIF)");
        newCode2.getParagraph().setFontStyle(FontStyle.BOLD);
        
        table.setFontSet(freeMonoSet);
        Row<PDPage> newRow3 = table.createRow(15f);
        Cell<PDPage> newLabel3 = newRow3.createCell(30, "");
        Cell<PDPage> newCode3 = newRow3.createCell(70, "FontUtils.loadFontSet(doc, SupportedFont.FREE_MONO)");
        newCode3.getParagraph().setFontStyle(FontStyle.BOLD);
        
        table.setFontSet(sourceSans3Set);
        Row<PDPage> newRow4 = table.createRow(15f);
        Cell<PDPage> newLabel4 = newRow4.createCell(30, "");
        Cell<PDPage> newCode4 = newRow4.createCell(70, "FontUtils.loadFontSet(doc, SupportedFont.SOURCE_SANS_3)");
        newCode4.getParagraph().setFontStyle(FontStyle.BOLD);
        
        // Benefits
        table.setFontSet(freeSansSetNew);
        Row<PDPage> benefitsRow = table.createRow(20f);
        Cell<PDPage> benefitsCell = benefitsRow.createCell(100, "Benefits: No hardcoded paths, type-safe, autocomplete support, less error-prone");
        benefitsCell.setTextColor(Color.DARK_GRAY);
        benefitsCell.getParagraph().setFontStyle(FontStyle.ITALIC);
        
        table.draw();
        
        // Save the document
        File file = new File("target/EnumFontDemonstration.pdf");
        System.out.println("Font enum demonstration saved at: " + file.getAbsolutePath());
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();
    }
}